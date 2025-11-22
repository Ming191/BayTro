"""Dashboard data models and utilities."""

from datetime import datetime, timedelta


class DashboardMetrics:
    """Dashboard metrics calculator for landlords."""
    
    @staticmethod
    def calculate_revenue_history(db, landlord_id: str, now: datetime, months: int = 6) -> list[dict]:
        """Calculate monthly revenue history for the last N months."""
        history = []
        for i in range(months):
            month = now.month - i
            year = now.year
            if month <= 0:
                month += 12
                year -= 1
            
            bills = db.collection("bills").where("landlordId", "==", landlord_id).where("month", "==", month).where("year", "==", year).where("status", "==", "PAID").stream()
            revenue = sum(b.to_dict().get("paidAmount", 0) for b in bills)
            history.append({"month": datetime(year, month, 1).strftime("%b"), "revenue": revenue})
        
        return list(reversed(history))
    
    @staticmethod
    def count_upcoming_deadlines(db, building_ids: list[str], current_day: int) -> int:
        """Count buildings with billing deadlines in the next 3 days."""
        count = 0
        for building_id in building_ids:
            doc = db.collection("buildings").document(building_id).get()
            if doc.exists:
                billing_date = doc.to_dict().get("billingDate")
                if billing_date and 0 <= (billing_date - current_day) <= 3:
                    count += 1
        return count
    
    @staticmethod
    def extract_unique_tenant_ids(contracts: list) -> set:
        """Extract unique tenant IDs from a list of contracts."""
        tenant_ids = set()
        for contract in contracts:
            tenant_ids.update(contract.to_dict().get("tenantIds", []))
        return tenant_ids
    
    @staticmethod
    def calculate_occupancy_rate(occupied: int, total: int) -> float:
        """Calculate occupancy rate as a percentage."""
        return (occupied / total * 100) if total > 0 else 0


class TenantDashboard:
    """Tenant dashboard data fetching utilities."""
    
    @staticmethod
    def fetch_services(db, building_id: str, room_id: str) -> tuple[list[dict], list[dict]]:
        """Fetch building services and room extra services."""
        services = []
        extra_services = []
        
        if building_id:
            services = [doc.to_dict() for doc in db.collection("buildings").document(building_id).collection("services").where("status", "==", "ACTIVE").stream()]
        
        if room_id:
            extra_services = [doc.to_dict() for doc in db.collection("rooms").document(room_id).collection("extraServices").where("status", "==", "ACTIVE").stream()]
        
        return services, extra_services
    
    @staticmethod
    def get_latest_document(db, collection: str, filters: list[tuple], order_field: str):
        """Get the latest document from a collection with filters."""
        from firebase_admin import firestore
        
        query = db.collection(collection)
        for field, op, value in filters:
            query = query.where(field, op, value)
        query = query.order_by(order_field, direction=firestore.Query.DESCENDING).limit(1)
        return next(query.stream(), None)
