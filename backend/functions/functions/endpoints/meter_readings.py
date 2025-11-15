"""Meter reading endpoints."""

import logging
from datetime import datetime, timezone
from firebase_functions import https_fn as https
from firebase_admin import firestore

from config import db
from models import Status, MeterReadingCalculator, MeterReadingValidator
from utils import send_fcm_notification, get_user_name, get_room_number

logger = logging.getLogger(__name__)


@https.on_call()
def notifyNewMeterReading(req: https.CallableRequest) -> dict:
    if not req.auth:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    reading_id = req.data.get("readingId")
    if not isinstance(reading_id, str) or not reading_id:
        raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="Invalid 'readingId'.")
    
    try:
        logger.info(f"Notifying new meter reading: reading={reading_id}")
        
        reading_doc = db.collection("meter_readings").document(reading_id).get()
        if not reading_doc.exists:
            raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="Meter reading not found.")
        
        landlord_id, tenant_id, room_id = MeterReadingValidator.validate_reading_data(reading_doc.to_dict())
        tenant_name = get_user_name(tenant_id)
        room_number = get_room_number(room_id)
        
        send_fcm_notification(
            user_id=landlord_id,
            title="New Meter Reading",
            body=f"{tenant_name} submitted a reading for room {room_number}.",
            data={"screen": "PendingRequests"}
        )
        
        logger.info(f"Notification sent: reading={reading_id}, landlord={landlord_id}")
        return {"status": "success", "message": "Notification sent."}
    
    except https.HttpsError:
        raise
    except Exception as e:
        logger.error(f"notifyNewMeterReading failed: reading={reading_id}, error={e}", exc_info=True)
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="Failed to send notification.")


@https.on_call()
def approveMeterReading(req: https.CallableRequest) -> dict:
    if not req.auth:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    landlord_uid = req.auth.uid
    reading_id = req.data.get("readingId")
    if not isinstance(reading_id, str) or not reading_id:
        raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="Invalid 'readingId'.")

    try:
        logger.info(f"Approving meter reading: user={landlord_uid}, reading={reading_id}")
        
        @firestore.transactional
        def _approve_transaction(transaction):
            reading_ref = db.collection("meter_readings").document(reading_id)
            reading_doc = reading_ref.get(transaction=transaction)
            if not reading_doc.exists:
                raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="Meter reading not found.")

            reading_data = reading_doc.to_dict()
            if reading_data.get("status") != Status.METER_PENDING:
                raise https.HttpsError(code=https.FunctionsErrorCode.FAILED_PRECONDITION, message="Reading is not pending.")

            contract_id = reading_data.get("contractId")
            contract_ref = db.collection("contracts").document(contract_id)
            contract_doc = contract_ref.get(transaction=transaction)
            if not contract_doc.exists:
                raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="Contract not found.")
            
            contract_data = contract_doc.to_dict()
            if contract_data.get("landlordId") != landlord_uid:
                raise https.HttpsError(code=https.FunctionsErrorCode.PERMISSION_DENIED, message="No permission to approve this reading.")

            building_id = contract_data.get("buildingId")
            if not building_id:
                raise https.HttpsError(code=https.FunctionsErrorCode.FAILED_PRECONDITION, message="Contract missing buildingId.")

            building_doc = db.collection("buildings").document(building_id).get(transaction=transaction)
            if not building_doc.exists:
                raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="Building not found.")

            billing_date = building_doc.to_dict().get("billingDate", 25)
            bill_month, bill_year = MeterReadingCalculator.calculate_bill_month(datetime.now(timezone.utc), billing_date)
            bill_month_str = f"{bill_year:04d}-{bill_month:02d}"
            bill_id = f"{bill_month_str}-{contract_id}"

            bill_ref = db.collection("bills").document(bill_id)
            MeterReadingValidator.validate_bill_status(bill_ref.get(transaction=transaction), bill_month_str)

            old_elec_value, old_water_value = MeterReadingCalculator.get_previous_readings(db, contract_id, contract_data, transaction)

            room_id = contract_data.get("roomId")
            building_services = [s.to_dict() for s in db.collection("buildings").document(building_id).collection("services").stream(transaction=transaction)]
            room_services = [s.to_dict() for s in db.collection("rooms").document(room_id).collection("extraServices").stream(transaction=transaction)]
            
            price_elec, price_water = MeterReadingCalculator.get_utility_prices(building_services + room_services)

            new_elec_value = reading_data.get("electricityValue", 0)
            new_water_value = reading_data.get("waterValue", 0)
            elec_consumption, water_consumption = MeterReadingCalculator.calculate_consumption(
                new_elec_value, new_water_value, old_elec_value, old_water_value
            )

            bill_item_elec, bill_item_water, total_cost = MeterReadingCalculator.create_bill_items(
                reading_id, old_elec_value, new_elec_value, old_water_value, new_water_value,
                elec_consumption, water_consumption, price_elec, price_water
            )

            transaction.set(bill_ref, {
                "month": bill_month,
                "year": bill_year,
                "status": "NOT_ISSUED_YET",
                "contractId": contract_id,
                "landlordId": contract_data.get("landlordId"),
                "buildingId": building_id,
                "roomId": room_id,
                "roomName": reading_data.get("roomName"),
                "buildingName": reading_data.get("buildingName"),
                "lineItems": firestore.ArrayUnion([bill_item_elec, bill_item_water]),
                "totalAmount": firestore.Increment(total_cost)
            }, merge=True)

            transaction.update(reading_ref, {
                "status": Status.METER_APPROVED,
                "approvedAt": firestore.SERVER_TIMESTAMP,
                "electricityConsumption": elec_consumption,
                "waterConsumption": water_consumption,
                "electricityCost": bill_item_elec["totalCost"],
                "waterCost": bill_item_water["totalCost"],
                "totalCost": total_cost
            })

            return contract_data.get("tenantIds", []), contract_id

        tenant_ids, contract_id = _approve_transaction(db.transaction())

        for tenant_id in tenant_ids:
            send_fcm_notification(
                user_id=tenant_id,
                title="Meter Reading Approved",
                body="Your meter reading has been approved by the landlord.",
                data={"screen": "MeterReadingHistory", "contractId": contract_id}
            )

        logger.info(f"Meter reading approved: reading={reading_id}, contract={contract_id}, tenants={len(tenant_ids)}")
        return {"status": "success", "message": "Reading approved and bill updated."}

    except https.HttpsError:
        raise
    except Exception as e:
        logger.error(f"approveMeterReading failed: user={landlord_uid}, reading={reading_id}, error={e}", exc_info=True)
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An internal server error occurred.")


@https.on_call()
def declineMeterReading(req: https.CallableRequest) -> dict:
    if not req.auth:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    landlord_uid = req.auth.uid
    reading_id = req.data.get("readingId")
    reason = req.data.get("reason")

    if not isinstance(reading_id, str) or not reading_id:
        raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="Invalid 'readingId'.")
    if not isinstance(reason, str) or not reason:
        raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="A reason must be provided.")
    
    try:
        logger.info(f"Declining meter reading: user={landlord_uid}, reading={reading_id}")
        
        reading_ref = db.collection("meter_readings").document(reading_id)
        reading_doc = reading_ref.get()
        if not reading_doc.exists:
            raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="Meter reading not found.")
        
        reading_data = reading_doc.to_dict()
        if reading_data.get("status") != Status.METER_PENDING:
            raise https.HttpsError(code=https.FunctionsErrorCode.FAILED_PRECONDITION, message="Reading is not pending.")
        
        contract_doc = db.collection("contracts").document(reading_data.get("contractId")).get()
        if not contract_doc.exists or contract_doc.to_dict().get("landlordId") != landlord_uid:
            raise https.HttpsError(code=https.FunctionsErrorCode.PERMISSION_DENIED, message="No permission for this contract.")
        
        reading_ref.update({
            "status": Status.METER_DECLINED,
            "declinedAt": firestore.SERVER_TIMESTAMP,
            "declineReason": reason
        })
        
        tenant_id = reading_data.get("tenantId")
        send_fcm_notification(
            user_id=tenant_id,
            title="Meter Reading Declined",
            body=f"Your reading was declined. Reason: {reason}",
            data={"screen": "MeterReadingHistory", "contractId": reading_data.get("contractId")}
        )
        
        logger.info(f"Meter reading declined: reading={reading_id}, tenant={tenant_id}")
        return {"status": "success", "message": "The reading has been declined."}
    
    except https.HttpsError:
        raise
    except Exception as e:
        logger.error(f"declineMeterReading failed: user={landlord_uid}, reading={reading_id}, error={e}", exc_info=True)
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An internal error occurred.")
