"""Contract management endpoints."""

import logging
from datetime import datetime
from firebase_functions import https_fn as https
from firebase_admin import firestore

from config import db
from models import Status, ContractFilter, ContractCleaner
from utils import send_fcm_notification

logger = logging.getLogger(__name__)


@https.on_call()
def getContractList(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    landlord_id = req.auth.uid
    status_filter = req.data.get("statusFilter")
    building_id_filter = req.data.get("buildingIdFilter")
    search_query = req.data.get("searchQuery", "")

    try:
        logger.info(f"Fetching contracts: user={landlord_id}, status={status_filter}, building={building_id_filter}, search='{search_query}'")

        buildings_filtered = [b for b in db.collection("buildings").where("userId", "==", landlord_id).stream() if b.to_dict().get("status") not in ["ARCHIVED", Status.DELETED]]
        non_archived_building_ids = [b.id for b in buildings_filtered]

        if not non_archived_building_ids:
            logger.info(f"No buildings found: user={landlord_id}")
            return {"contracts": []}

        status_list = ContractFilter.apply_status_filter(status_filter)
        contracts_query = db.collection("contracts").where("landlordId", "==", landlord_id).where("status", "in", status_list)

        if building_id_filter:
            if building_id_filter not in non_archived_building_ids:
                logger.info(f"Building archived: building={building_id_filter}")
                return {"contracts": []}
            contracts_query = contracts_query.where("buildingId", "==", building_id_filter)

        all_contracts = [c for c in contracts_query.stream() if c.to_dict().get("buildingId") in non_archived_building_ids]

        if not all_contracts:
            return {"contracts": []}

        room_ids = list(set(c.to_dict().get("roomId") for c in all_contracts if c.to_dict().get("roomId")))
        rooms_map = {}
        if room_ids:
            for i in range(0, len(room_ids), 30):
                for room in db.collection("rooms").where("__name__", "in", room_ids[i:i + 30]).stream():
                    rooms_map[room.id] = room.to_dict().get("roomNumber", "N/A")

        filtered_contracts = ContractFilter.apply_search_filter(all_contracts, search_query, rooms_map)

        contracts_with_rooms = []
        for contract_doc in filtered_contracts:
            contract_data = {**contract_doc.to_dict(), 'id': contract_doc.id}
            contracts_with_rooms.append({
                "contract": contract_data,
                "roomNumber": rooms_map.get(contract_data.get("roomId"), "N/A")
            })

        contracts_with_rooms = ContractFilter.sort_by_start_date(contracts_with_rooms)

        logger.info(f"Contracts fetched: user={landlord_id}, total={len(contracts_with_rooms)}")
        return {"contracts": contracts_with_rooms}

    except Exception as e:
        logger.error(f"getContractList failed: user={landlord_id}, error={e}", exc_info=True)
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An error occurred while fetching contracts.")


@https.on_call()
def endContract(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    landlord_uid = req.auth.uid
    contract_id = req.data.get("contractId")
    force_end = req.data.get("forceEnd", False)

    if not isinstance(contract_id, str) or not contract_id:
        raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="A valid 'contractId' must be provided.")

    try:
        logger.info(f"Ending contract: user={landlord_uid}, contract={contract_id}, force={force_end}")
        contract_ref = db.collection("contracts").document(contract_id)
        contract_doc = contract_ref.get()

        if not contract_doc.exists:
            raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="Contract not found.")

        contract_data = contract_doc.to_dict()

        if contract_data.get("landlordId") != landlord_uid:
            raise https.HttpsError(code=https.FunctionsErrorCode.PERMISSION_DENIED, message="You do not have permission to end this contract.")

        if contract_data.get("status") == "ENDED":
            logger.info(f"Contract already ended: contract={contract_id}")
            return {"status": "success", "message": "Contract was already ended."}

        if contract_data.get("status") != Status.CONTRACT_ACTIVE:
            raise https.HttpsError(code=https.FunctionsErrorCode.FAILED_PRECONDITION, message=f"Cannot end contract with status '{contract_data.get('status')}'. Only ACTIVE contracts can be ended.")

        unpaid_bills = list(db.collection("bills").where("contractId", "==", contract_id).where("status", "in", ["UNPAID", "OVERDUE"]).stream())

        if unpaid_bills and not force_end:
            total_unpaid = sum(bill.to_dict().get("totalAmount", 0) for bill in unpaid_bills)
            logger.warning(f"Unpaid bills detected: contract={contract_id}, bills={len(unpaid_bills)}, total={total_unpaid}")
            return {
                "status": "warning",
                "message": f"This contract has {len(unpaid_bills)} unpaid bill(s) totaling {total_unpaid:,.0f} ₫. To proceed, please confirm that you want to end the contract anyway.",
                "unpaidBillsCount": len(unpaid_bills),
                "totalUnpaidAmount": total_unpaid
            }

        room_id = contract_data.get("roomId")
        cleanup_data = ContractCleaner.fetch_cleanup_data(db, contract_id, room_id)

        logger.info(f"Cleanup data fetched: contract={contract_id}, readings={len(cleanup_data['pending_readings'])}, sessions={len(cleanup_data['pending_sessions'])}, requests={len(cleanup_data['pending_requests'])}")

        batch = db.batch()
        ContractCleaner.apply_cleanup_batch(batch, contract_ref, room_id, cleanup_data, landlord_uid)
        batch.commit()

        tenant_ids = contract_data.get("tenantIds", [])
        room_name = contract_data.get("roomName", contract_data.get("name", "your room"))

        for tenant_id in tenant_ids:
            send_fcm_notification(
                user_id=tenant_id,
                title="Contract Ended",
                body=f"Your contract for {room_name} has been ended by the landlord."
            )

        warnings = []
        if unpaid_bills and force_end:
            warnings.append(f"{len(unpaid_bills)} unpaid bill(s) remain associated with this ended contract.")
        if cleanup_data['pending_requests']:
            warnings.append(f"{len(cleanup_data['pending_requests'])} pending request(s) were cancelled.")

        logger.info(f"Contract ended: contract={contract_id}, tenants_notified={len(tenant_ids)}")
        return {
            "status": "success",
            "message": "Contract has been successfully ended.",
            "warnings": warnings if warnings else None
        }

    except https.HttpsError:
        raise
    except Exception as e:
        logger.error(f"endContract failed: user={landlord_uid}, contract={contract_id}, error={e}", exc_info=True)
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An internal server error occurred while ending the contract.")
