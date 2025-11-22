"""Request management endpoints."""

import logging
from firebase_functions import https_fn as https
from firebase_admin import firestore

from config import db
from models import Status, RequestFilter, RequestEnricher

logger = logging.getLogger(__name__)


@https.on_call()
def getRequestList(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    user_id = req.auth.uid
    building_id_filter = req.data.get("buildingIdFilter")
    limit = int(req.data.get("limit", 10))
    start_after = req.data.get("startAfter")

    logger.info(f"Fetching request list: user={user_id}, building_filter={building_id_filter}, limit={limit}")

    try:
        user_doc = db.collection("users").document(user_id).get()
        if not user_doc.exists:
            raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="User not found.")

        user_role = user_doc.to_dict().get("role", {}).get("type")
        requests_query = None
        base_query = db.collection("requests").order_by("createdAt", direction=firestore.Query.DESCENDING).limit(limit)
        requests_query = RequestFilter.apply_role_filter(base_query, user_id, user_role)
        
        if not requests_query:
            logger.warning(f"Unknown user role: user={user_id}, role={user_role}")
            return {"requests": [], "nextCursor": None}
        
        if user_role == "Landlord" and building_id_filter:
            room_ids_in_building = RequestFilter.apply_building_filter(db, building_id_filter)
            if not room_ids_in_building:
                logger.info(f"No rooms in building: building={building_id_filter}")
                return {"requests": [], "nextCursor": None}
            requests_query = requests_query.where("roomId", "in", room_ids_in_building)

        if start_after:
            last_doc = db.collection("requests").document(start_after).get()
            requests_query = RequestFilter.apply_pagination(requests_query, last_doc)

        all_requests = list(requests_query.stream())
        logger.info(f"Fetched requests: count={len(all_requests)}")

        if not all_requests:
            return {"requests": [], "nextCursor": None}

        tenant_ids, landlord_ids, room_ids = RequestEnricher.extract_unique_ids(all_requests)
        users_map = RequestEnricher.fetch_users_map(db, tenant_ids + landlord_ids)
        rooms_map = RequestEnricher.fetch_rooms_map(db, room_ids)
        
        building_ids = list(set(
            room.get("buildingId") for room in rooms_map.values() if room.get("buildingId")
        ))
        buildings_map = RequestEnricher.fetch_buildings_map(db, building_ids)

        full_requests_info = []
        for request_doc in all_requests:
            enriched = RequestEnricher.enrich_request(request_doc, users_map, rooms_map, buildings_map)
            if enriched:
                full_requests_info.append(enriched)
            else:
                logger.warning(f"Skipping request with missing data: request={request_doc.id}")

        full_requests_info = RequestEnricher.sort_by_created_at(full_requests_info)
        next_cursor = full_requests_info[-1]["request"]["id"] if full_requests_info else None

        logger.info(f"Returning request list: count={len(full_requests_info)}, next_cursor={next_cursor}")
        return {"requests": full_requests_info, "nextCursor": next_cursor}

    except https.HttpsError:
        raise
    except Exception as e:
        logger.error(f"getRequestList failed: user={user_id}, error={e}", exc_info=True)
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An error occurred while fetching requests.")
