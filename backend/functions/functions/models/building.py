"""Building and room data models."""

from dataclasses import dataclass
from typing import Optional


@dataclass
class BuildingStatus:
    """Building status constants."""
    ACTIVE = "ACTIVE"
    ARCHIVED = "ARCHIVED"
    DELETED = "DELETED"


class BuildingStats:
    """Building statistics calculator."""
    
    @staticmethod
    def calculate(building_id: str, rooms: list, contracts: list, bills: list) -> dict:
        """Calculate statistics for a building.
        
        Args:
            building_id: Building document ID
            rooms: List of room documents for the building
            contracts: List of active contract documents for the building
            bills: List of paid bill documents for the building (current month)
            
        Returns:
            Dictionary with totalRooms, occupiedRooms, and revenue
        """
        rooms_in_building = [r for r in rooms if r.to_dict().get("buildingId") == building_id]
        contracts_in_building = [c for c in contracts if c.to_dict().get("buildingId") == building_id]
        
        total_rooms = len(rooms_in_building)
        occupied_rooms = len(set(c.to_dict().get("roomId") for c in contracts_in_building))
        revenue = sum(b.to_dict().get("paidAmount", 0) for b in bills if b.to_dict().get("buildingId") == building_id)
        
        return {
            "totalRooms": total_rooms,
            "occupiedRooms": occupied_rooms,
            "revenue": revenue
        }


class BuildingFilter:
    """Building list filtering logic."""
    
    @staticmethod
    def apply_status_filter(buildings: list, status: Optional[str]) -> list:
        """Filter buildings by status."""
        if not status or status == "ALL":
            return buildings
        return [b for b in buildings if b["building"].get("status") == status]
    
    @staticmethod
    def apply_search_filter(buildings: list, query: str) -> list:
        """Filter buildings by search query (name or address)."""
        if not query:
            return buildings
        query_lower = query.strip().lower()
        return [
            b for b in buildings
            if query_lower in b["building"].get("name", "").lower()
            or query_lower in b["building"].get("address", "").lower()
        ]
    
    @staticmethod
    def sort_by_name(buildings: list) -> list:
        """Sort buildings by name alphabetically."""
        return sorted(buildings, key=lambda x: x["building"].get("name", ""))
