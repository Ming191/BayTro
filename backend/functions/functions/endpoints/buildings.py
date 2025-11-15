"""Building and room management endpoints."""

import logging
from datetime import datetime, timezone
from firebase_functions import https_fn as https
from firebase_admin import firestore

from config import db
from models import Status, BuildingStats, BuildingFilter

logger = logging.getLogger(__name__)


def _validate_building_ownership(building_doc, user_id: str):
    if not building_doc.exists:
        raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="Building not found.")
    if building_doc.to_dict().get("userId") != user_id:
        raise https.HttpsError(code=https.FunctionsErrorCode.PERMISSION_DENIED, message="You do not have permission to modify this building.")


def _validate_room_ownership(room_doc, user_id: str):
    if not room_doc.exists:
        raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="Room not found.")
    room_data = room_doc.to_dict()
    building_id = room_data.get("buildingId")
    if not building_id:
        raise https.HttpsError(code=https.FunctionsErrorCode.FAILED_PRECONDITION, message="Room has no associated building.")
    building_doc = db.collection("buildings").document(building_id).get()
    if not building_doc.exists or building_doc.to_dict().get("userId") != user_id:
        raise https.HttpsError(code=https.FunctionsErrorCode.PERMISSION_DENIED, message="You do not have permission to modify this room.")
    return room_data


def _check_active_contracts(collection_field: str, entity_id: str, entity_type: str):
    active_contracts = list(db.collection("contracts").where(collection_field, "==", entity_id).where("status", "==", Status.CONTRACT_ACTIVE).limit(1).stream())
    if active_contracts:
        raise https.HttpsError(code=https.FunctionsErrorCode.FAILED_PRECONDITION, message=f"Cannot archive {entity_type} with active contracts. Please end all contracts first.")


@https.on_call()
def archiveBuilding(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    landlord_uid = req.auth.uid
    building_id = req.data.get("buildingId")

    if not isinstance(building_id, str) or not building_id:
        raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="A valid 'buildingId' must be provided.")

    try:
        logger.info(f"Archiving building: user={landlord_uid}, building={building_id}")
        building_ref = db.collection("buildings").document(building_id)
        building_doc = building_ref.get()

        _validate_building_ownership(building_doc, landlord_uid)

        if building_doc.to_dict().get("status") == "ARCHIVED":
            logger.info(f"Building already archived: building={building_id}")
            return {"status": "success", "message": "Building was already archived."}

        _check_active_contracts("buildingId", building_id, "building")

        batch = db.batch()
        batch.update(building_ref, {"status": "ARCHIVED"})

        rooms_to_archive = list(db.collection("rooms").where("buildingId", "==", building_id).stream())
        for room_doc in rooms_to_archive:
            batch.update(room_doc.reference, {"status": "ARCHIVED"})

        batch.commit()
        logger.info(f"Building archived: building={building_id}, rooms={len(rooms_to_archive)}")
        return {"status": "success", "message": "Building and all its rooms have been successfully archived."}

    except https.HttpsError:
        raise
    except Exception as e:
        logger.error(f"archiveBuilding failed: user={landlord_uid}, building={building_id}, error={e}")
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An internal server error occurred while archiving the building.")


@https.on_call()
def archiveRoom(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    landlord_uid = req.auth.uid
    room_id = req.data.get("roomId")

    if not isinstance(room_id, str) or not room_id:
        raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="A valid 'roomId' must be provided.")

    try:
        logger.info(f"Archiving room: user={landlord_uid}, room={room_id}")
        room_ref = db.collection("rooms").document(room_id)
        room_doc = room_ref.get()

        room_data = _validate_room_ownership(room_doc, landlord_uid)

        if room_data.get("status") == "ARCHIVED":
            logger.info(f"Room already archived: room={room_id}")
            return {"status": "success", "message": "Room was already archived."}

        _check_active_contracts("roomId", room_id, "room")

        room_ref.update({"status": "ARCHIVED"})
        logger.info(f"Room archived: room={room_id}")
        return {"status": "success", "message": "Room has been successfully archived."}

    except https.HttpsError:
        raise
    except Exception as e:
        logger.error(f"archiveRoom failed: user={landlord_uid}, room={room_id}, error={e}")
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An internal server error occurred while archiving the room.")


@https.on_call()
def getBuildingListWithStats(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    landlord_id = req.auth.uid
    search_query = req.data.get("searchQuery", "")
    status_filter = req.data.get("statusFilter")

    try:
        logger.info(f"Fetching building list: user={landlord_id}, search='{search_query}', status={status_filter}")
        now = datetime.now(timezone.utc)

        all_buildings = [doc for doc in db.collection("buildings").where("userId", "==", landlord_id).stream() if doc.to_dict().get("status") not in ["ARCHIVED", Status.DELETED]]
        building_ids = [doc.id for doc in all_buildings]

        if not building_ids:
            logger.info(f"No buildings found: user={landlord_id}")
            return {"buildings": []}

        all_rooms = []
        all_contracts = []
        paid_bills_this_month = []

        for i in range(0, len(building_ids), 30):
            chunk_ids = building_ids[i:i + 30]
            rooms_filtered = [r for r in db.collection("rooms").where("buildingId", "in", chunk_ids).stream() if r.to_dict().get("status") not in ["ARCHIVED", Status.DELETED]]
            contracts = list(db.collection("contracts").where("buildingId", "in", chunk_ids).where("status", "==", Status.CONTRACT_ACTIVE).stream())
            bills = list(db.collection("bills").where("landlordId", "==", landlord_id).where("buildingId", "in", chunk_ids).where("month", "==", now.month).where("year", "==", now.year).where("status", "==", "PAID").stream())

            all_rooms.extend(rooms_filtered)
            all_contracts.extend(contracts)
            paid_bills_this_month.extend(bills)

        buildings_with_stats = []
        for building_doc in all_buildings:
            stats = BuildingStats.calculate(building_doc.id, all_rooms, all_contracts, paid_bills_this_month)
            buildings_with_stats.append({"building": building_doc.to_dict(), **stats})

        filtered = BuildingFilter.apply_status_filter(buildings_with_stats, status_filter)
        filtered = BuildingFilter.apply_search_filter(filtered, search_query)
        filtered = BuildingFilter.sort_by_name(filtered)

        logger.info(f"Building list fetched: user={landlord_id}, total={len(all_buildings)}, filtered={len(filtered)}")
        return {"buildings": filtered}

    except Exception as e:
        logger.error(f"getBuildingListWithStats failed: user={landlord_id}, error={e}", exc_info=True)
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An error occurred while fetching the building list.")
