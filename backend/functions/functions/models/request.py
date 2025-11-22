"""Request management models."""

from typing import Dict, List, Optional, Tuple
from firebase_admin import firestore

from models.status import Status


class RequestFilter:
    """Handles filtering logic for request queries."""
    
    @staticmethod
    def apply_role_filter(query, user_id: str, role: str):
        """Apply user role-based filtering to request query."""
        if role == "Landlord":
            return query.where("landlordId", "==", user_id)
        elif role == "Tenant":
            return query.where("tenantId", "==", user_id)
        return None
    
    @staticmethod
    def apply_building_filter(db, building_id: str) -> List[str]:
        """Get room IDs for a specific building, excluding archived/deleted."""
        room_docs = db.collection("rooms").where("buildingId", "==", building_id).stream()
        return [
            doc.id for doc in room_docs
            if doc.to_dict().get("status") not in ["ARCHIVED", Status.DELETED]
        ]
    
    @staticmethod
    def apply_pagination(query, start_after_doc):
        """Apply pagination to query using start_after cursor."""
        if start_after_doc and start_after_doc.exists:
            return query.start_after(start_after_doc)
        return query


class RequestEnricher:
    """Enriches request data with related entity information."""
    
    @staticmethod
    def extract_unique_ids(requests: List) -> Tuple[List[str], List[str], List[str]]:
        """Extract unique tenant, landlord, and room IDs from requests."""
        tenant_ids = list(set(req.to_dict().get("tenantId") for req in requests if req.to_dict().get("tenantId")))
        landlord_ids = list(set(req.to_dict().get("landlordId") for req in requests if req.to_dict().get("landlordId")))
        room_ids = list(set(req.to_dict().get("roomId") for req in requests if req.to_dict().get("roomId")))
        return tenant_ids, landlord_ids, room_ids
    
    @staticmethod
    def fetch_users_map(db, user_ids: List[str]) -> Dict:
        """Fetch user documents and return as map."""
        if not user_ids:
            return {}
        return {doc.id: doc.to_dict() for doc in db.collection("users").where("__name__", "in", user_ids).stream()}
    
    @staticmethod
    def fetch_rooms_map(db, room_ids: List[str]) -> Dict:
        """Fetch room documents and return as map, excluding archived/deleted."""
        if not room_ids:
            return {}
        return {
            doc.id: doc.to_dict()
            for doc in db.collection("rooms").where("__name__", "in", room_ids).stream()
            if doc.to_dict().get("status") not in ["ARCHIVED", Status.DELETED]
        }
    
    @staticmethod
    def fetch_buildings_map(db, building_ids: List[str]) -> Dict:
        """Fetch building documents and return as map."""
        if not building_ids:
            return {}
        return {doc.id: doc.to_dict() for doc in db.collection("buildings").where("__name__", "in", building_ids).stream()}
    
    @staticmethod
    def enrich_request(request_doc, users_map: Dict, rooms_map: Dict, buildings_map: Dict) -> Optional[Dict]:
        """Enrich request with tenant, landlord, room, and building info."""
        request_data = request_doc.to_dict()
        request_data["id"] = request_doc.id
        
        tenant = users_map.get(request_data.get("tenantId"))
        landlord = users_map.get(request_data.get("landlordId"))
        room = rooms_map.get(request_data.get("roomId"))
        building = buildings_map.get(room.get("buildingId")) if room else None
        
        if not all([tenant, landlord, room, building]):
            return None
        
        return {
            "request": request_data,
            "tenantName": tenant.get("fullName", "N/A"),
            "tenantPhoneNumber": tenant.get("phoneNumber", ""),
            "roomName": room.get("roomNumber", "N/A"),
            "buildingName": building.get("name", "N/A"),
            "landlordName": landlord.get("fullName", "N/A"),
            "landlordPhoneNumber": landlord.get("phoneNumber", "")
        }
    
    @staticmethod
    def sort_by_created_at(requests: List[Dict], descending: bool = True) -> List[Dict]:
        """Sort requests by createdAt timestamp."""
        return sorted(
            requests,
            key=lambda x: x["request"].get("createdAt").timestamp() if x["request"].get("createdAt") else 0,
            reverse=descending
        )
