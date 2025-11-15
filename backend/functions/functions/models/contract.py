"""Contract data models and utilities."""

from datetime import datetime
from typing import Optional


class ContractStatus:
    """Contract status constants."""
    ACTIVE = "ACTIVE"
    PENDING = "PENDING"
    ENDED = "ENDED"
    OVERDUE = "OVERDUE"


class ContractFilter:
    """Contract list filtering and sorting logic."""
    
    @staticmethod
    def apply_status_filter(status_filter: Optional[str]) -> list[str]:
        """Get status list for query based on filter.
        
        Returns:
            List of statuses to query
        """
        if status_filter == "ACTIVE":
            return ["ACTIVE", "OVERDUE"]
        elif status_filter in ["PENDING", "ENDED"]:
            return [status_filter]
        else:
            return ["ACTIVE", "OVERDUE"]
    
    @staticmethod
    def apply_search_filter(contracts: list, search_query: str, rooms_map: dict) -> list:
        """Filter contracts by search query (contract number or room number)."""
        if not search_query:
            return contracts
        
        query_lower = search_query.strip().lower()
        filtered = []
        
        for contract_doc in contracts:
            contract_data = contract_doc.to_dict()
            contract_num = contract_data.get("contractNumber", "").lower()
            room_number = rooms_map.get(contract_data.get("roomId"), "N/A").lower()
            
            if query_lower in contract_num or query_lower in room_number:
                filtered.append(contract_doc)
        
        return filtered
    
    @staticmethod
    def sort_by_start_date(contracts_with_rooms: list) -> list:
        """Sort contracts by start date (newest first)."""
        def parse_date(date_str):
            if not date_str:
                return datetime.min
            try:
                return datetime.strptime(date_str, "%d/%m/%Y")
            except:
                return datetime.min
        
        return sorted(
            contracts_with_rooms,
            key=lambda x: parse_date(x["contract"].get("startDate")),
            reverse=True
        )


class ContractCleaner:
    """Contract cleanup operations for ending contracts."""
    
    @staticmethod
    def fetch_cleanup_data(db, contract_id: str, room_id: Optional[str]) -> dict:
        """Fetch all data that needs to be cleaned up when ending contract.
        
        Returns:
            Dictionary with pending_readings, pending_sessions, pending_requests
        """
        pending_readings = list(db.collection("meter_readings").where("contractId", "==", contract_id).where("status", "==", "PENDING").stream())
        pending_sessions = list(db.collection("qr_sessions").where("contractId", "==", contract_id).where("status", "in", ["PENDING", "SCANNED"]).stream())
        
        pending_requests = []
        if room_id:
            pending_requests = list(db.collection("requests").where("roomId", "==", room_id).where("status", "in", ["PENDING", "IN_PROGRESS"]).stream())
        
        return {
            "pending_readings": pending_readings,
            "pending_sessions": pending_sessions,
            "pending_requests": pending_requests
        }
    
    @staticmethod
    def apply_cleanup_batch(batch, contract_ref, room_id: Optional[str], cleanup_data: dict, landlord_uid: str):
        """Apply all cleanup operations to a batch.
        
        Args:
            batch: Firestore batch
            contract_ref: Contract document reference
            room_id: Room ID to mark as available
            cleanup_data: Data from fetch_cleanup_data()
            landlord_uid: Landlord user ID
        """
        from firebase_admin import firestore
        
        batch.update(contract_ref, {
            "status": "ENDED",
            "endedAt": firestore.SERVER_TIMESTAMP,
            "endedBy": landlord_uid
        })
        
        if room_id:
            from config import db
            batch.update(db.collection("rooms").document(room_id), {"status": "AVAILABLE"})
        
        for reading_doc in cleanup_data["pending_readings"]:
            batch.update(reading_doc.reference, {"status": "CANCELLED", "cancelledReason": "Contract ended"})
        
        for session_doc in cleanup_data["pending_sessions"]:
            batch.update(session_doc.reference, {"status": "EXPIRED"})
        
        for request_doc in cleanup_data["pending_requests"]:
            batch.update(request_doc.reference, {
                "status": "CANCELLED",
                "cancelledReason": "Contract ended",
                "cancelledAt": firestore.SERVER_TIMESTAMP
            })
