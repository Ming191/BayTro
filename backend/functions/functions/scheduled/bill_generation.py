"""Scheduled job for generating monthly bills."""

import logging
from datetime import datetime, timezone
from firebase_functions import scheduler_fn

from config import db
from models import Status, BillData, PaymentCodeConfig
from utils import get_user_name, calculate_payment_dates, process_service_charges

logger = logging.getLogger(__name__)


def _get_tenant_names(tenant_ids: list) -> str:
    tenant_names = [name for t_id in tenant_ids if (name := get_user_name(t_id, default=None))]
    return ", ".join(tenant_names) if tenant_names else "N/A"


def _calculate_line_items(contract_data: dict, building_doc, room_doc, num_tenants: int) -> tuple[list, int]:
    line_items = []
    total_cost = 0
    
    rental_fee = contract_data.get("rentalFee", 0)
    if rental_fee > 0:
        line_items.append({"description": "Monthly Rent", "totalCost": rental_fee})
        total_cost += rental_fee
    
    building_services = [s.to_dict() for s in building_doc.reference.collection("services").stream()]
    building_items, building_cost = process_service_charges(building_services, num_tenants)
    line_items.extend(building_items)
    total_cost += building_cost
    
    room_services = [s.to_dict() for s in room_doc.reference.collection("extraServices").stream()]
    room_items, room_cost = process_service_charges(room_services, num_tenants)
    line_items.extend(room_items)
    total_cost += room_cost
    
    return line_items, total_cost


def _process_contract(contract_doc, building_data: dict, building_doc, now) -> None:
    contract_data = contract_doc.to_dict()
    contract_id = contract_doc.id
    
    landlord_id = contract_data.get("landlordId")
    room_id = contract_data.get("roomId")
    
    if not all([room_id, landlord_id]):
        logger.warning(f"Skipping contract={contract_id}: missing landlord_id={landlord_id} or room_id={room_id}")
        return
    
    room_doc = db.collection("rooms").document(room_id).get()
    if not room_doc.exists:
        logger.warning(f"Skipping contract={contract_id}: room={room_id} not found")
        return
    
    tenant_ids = contract_data.get("tenantIds", [])
    if not tenant_ids:
        logger.warning(f"Skipping contract={contract_id}: no tenants")
        return
    
    landlord_user_doc = db.collection("users").document(landlord_id).get()
    landlord_data = landlord_user_doc.to_dict() if landlord_user_doc.exists else {}
    
    if not landlord_data:
        logger.warning(f"Landlord={landlord_id} not found, using default payment config")
    
    tenant_names = _get_tenant_names(tenant_ids)
    line_items, total_cost = _calculate_line_items(contract_data, building_doc, room_doc, len(tenant_ids))
    
    if total_cost <= 0:
        logger.info(f"Skipping contract={contract_id}: no charges to bill")
        return
    
    payment_code = BillData.generate_payment_code(landlord_data, contract_id) if landlord_data else f"{PaymentCodeConfig.DEFAULT_PREFIX}{contract_id[-PaymentCodeConfig.DEFAULT_SUFFIX_LENGTH:].upper()}"
    bank_info = BillData.extract_bank_info(landlord_data) if landlord_data else {}
    payment_start_date, payment_due_date = calculate_payment_dates(now, building_data)
    
    bill_id = f"{now.strftime('%Y-%m')}-{contract_id}"
    bill_data = BillData.create(
        now, contract_id, contract_data, building_data, room_doc.to_dict(),
        tenant_names, payment_code, bank_info, line_items, total_cost,
        payment_start_date, payment_due_date
    )
    
    db.collection("bills").document(bill_id).set(bill_data, merge=True)
    logger.info(f"Bill created: bill_id={bill_id}, contract={contract_id}, total={total_cost}, payment_code={payment_code}")


def _process_building(building_doc, now) -> int:
    building_data = building_doc.to_dict()
    building_id = building_doc.id
    building_name = building_data.get('name', 'N/A')
    
    active_contracts = list(db.collection("contracts").where("buildingId", "==", building_id).where("status", "==", Status.CONTRACT_ACTIVE).stream())
    
    logger.info(f"Processing building: building_id={building_id}, name={building_name}, contracts={len(active_contracts)}")
    
    bills_created = 0
    for contract_doc in active_contracts:
        try:
            _process_contract(contract_doc, building_data, building_doc, now)
            bills_created += 1
        except Exception as e:
            logger.error(f"Contract processing failed: contract_id={contract_doc.id}, building_id={building_id}, error={e}")
    
    return bills_created


@scheduler_fn.on_schedule(schedule="every day 01:00", timezone="Asia/Ho_Chi_Minh")
def generateMonthlyBills(event: scheduler_fn.ScheduledEvent) -> None:
    logger.info("generateMonthlyBills job started")
    
    try:
        now = datetime.now(timezone.utc)
        billing_day = now.day
        
        due_buildings = list(db.collection("buildings").where("billingDate", "==", billing_day).stream())
        
        if not due_buildings:
            logger.info(f"No buildings due for billing: date={now.strftime('%Y-%m-%d')}, day={billing_day}")
            return
        
        logger.info(f"Processing bill generation: date={now.strftime('%Y-%m-%d')}, buildings={len(due_buildings)}")
        
        total_bills = 0
        for building_doc in due_buildings:
            total_bills += _process_building(building_doc, now)
        
        logger.info(f"generateMonthlyBills completed: buildings={len(due_buildings)}, bills_created={total_bills}")
        
    except Exception as e:
        logger.error(f"generateMonthlyBills failed: {e}", exc_info=True)
