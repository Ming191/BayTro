"""Dashboard and analytics endpoints."""

import logging
from datetime import datetime, timedelta, timezone
from firebase_functions import https_fn as https

from config import db
from models import Status, DashboardMetrics, TenantDashboard

logger = logging.getLogger(__name__)


def _filter_active_entities(entities: list, id_field: str = None) -> tuple[list, list]:
    filtered = [e for e in entities if e.to_dict().get("status") not in ["ARCHIVED", Status.DELETED]]
    ids = [e.id for e in filtered] if id_field else []
    return filtered, ids


@https.on_call()
def getLandlordDashboard(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    landlord_id = req.auth.uid

    try:
        logger.info(f"Fetching landlord dashboard: user={landlord_id}")
        now = datetime.now(timezone.utc)
        current_month, current_year = now.month, now.year

        buildings_query = db.collection("buildings").where("userId", "==", landlord_id).stream()
        buildings, building_ids = _filter_active_entities(buildings_query, "id")

        user_doc = db.collection("users").document(landlord_id).get()
        pending_readings = list(db.collection("meter_readings").where("landlordId", "==", landlord_id).where("status", "==", Status.METER_PENDING).stream())
        active_contracts = list(db.collection("contracts").where("landlordId", "==", landlord_id).where("status", "==", Status.CONTRACT_ACTIVE).stream())
        join_requests = list(db.collection("qr_sessions").where("inviterId", "==", landlord_id).where("status", "==", Status.SCANNED).stream())
        
        all_rooms = []
        if building_ids:
            all_rooms, _ = _filter_active_entities(db.collection("rooms").where("buildingId", "in", building_ids).stream())
        
        bills_this_month = [b for b in db.collection("bills").where("landlordId", "==", landlord_id).where("month", "==", current_month).where("year", "==", current_year).stream() if b.to_dict().get("status") != Status.DELETED]

        pending_readings_count = len(pending_readings)
        new_join_requests_count = len(join_requests)
        overdue_bills_count = sum(1 for b in bills_this_month if b.to_dict().get("status") == "OVERDUE")
        total_pending_actions = pending_readings_count + new_join_requests_count + overdue_bills_count

        total_revenue_this_month = sum(b.to_dict().get("paidAmount", 0) for b in bills_this_month if b.to_dict().get("status") == "PAID")
        total_unpaid_amount = sum(b.to_dict().get("totalAmount", 0) for b in bills_this_month if b.to_dict().get("status") in ["UNPAID", "OVERDUE"])

        total_room_count = len(all_rooms)
        occupied_room_count = len(active_contracts)
        occupancy_rate = DashboardMetrics.calculate_occupancy_rate(occupied_room_count, total_room_count)

        revenue_history = DashboardMetrics.calculate_revenue_history(db, landlord_id, now)
        pending_requests_count = db.collection("requests").where("landlordId", "==", landlord_id).where("status", "==", "PENDING").count().get()[0][0].value
        total_tenants_count = len(DashboardMetrics.extract_unique_tenant_ids(active_contracts))
        upcoming_deadlines_count = DashboardMetrics.count_upcoming_deadlines(db, building_ids, now.day)
        
        seven_days_ago = now - timedelta(days=7)
        recent_payments_count = db.collection("bills").where("landlordId", "==", landlord_id).where("status", "==", "PAID").where("paymentDate", ">=", seven_days_ago).count().get()[0][0].value

        dashboard_data = {
            "totalPendingActions": total_pending_actions,
            "pendingReadingsCount": pending_readings_count,
            "newJoinRequestsCount": new_join_requests_count,
            "overdueBillsCount": overdue_bills_count,
            "pendingRequestsCount": pending_requests_count,
            "totalRevenueThisMonth": total_revenue_this_month,
            "totalUnpaidAmount": total_unpaid_amount,
            "totalOccupancyRate": occupancy_rate,
            "occupiedRoomCount": occupied_room_count,
            "totalRoomCount": total_room_count,
            "totalTenantsCount": total_tenants_count,
            "totalBuildingsCount": len(building_ids),
            "activeContractsCount": len(active_contracts),
            "upcomingBillingDeadlinesCount": upcoming_deadlines_count,
            "recentPaymentsCount": recent_payments_count,
            "monthlyRevenueHistory": revenue_history,
            "username": user_doc.to_dict().get("fullName", "") if user_doc.exists else ""
        }

        logger.info(f"Landlord dashboard fetched: user={landlord_id}, buildings={len(building_ids)}, rooms={total_room_count}")
        return {"status": "success", "data": dashboard_data}

    except Exception as e:
        logger.error(f"getLandlordDashboard failed: user={landlord_id}, error={e}", exc_info=True)
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An error occurred while fetching dashboard data.")


@https.on_call()
def getTenantDashboardData(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    tenant_id = req.auth.uid

    try:
        logger.info(f"Fetching tenant dashboard: user={tenant_id}")
        user_doc = db.collection("users").document(tenant_id).get()
        if not user_doc.exists:
            raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="User not found.")

        contracts = list(db.collection("contracts").where("tenantIds", "array_contains", tenant_id).where("status", "==", Status.CONTRACT_ACTIVE).limit(1).stream())

        if not contracts:
            logger.info(f"No active contract: user={tenant_id}")
            return {"status": "success", "data": {"user": user_doc.to_dict()}}

        contract_doc = contracts[0]
        contract_data = {**contract_doc.to_dict(), 'id': contract_doc.id}
        contract_id = contract_doc.id
        room_id = contract_data.get("roomId")
        building_id = contract_data.get("buildingId")

        room_doc = db.collection("rooms").document(room_id).get() if room_id else None
        building_doc = db.collection("buildings").document(building_id).get() if building_id else None

        services, extra_services = TenantDashboard.fetch_services(db, building_id, room_id)

        last_reading_doc = TenantDashboard.get_latest_document(db, "meter_readings", [("contractId", "==", contract_id), ("status", "==", Status.METER_APPROVED)], "createdAt")
        current_bill_doc = TenantDashboard.get_latest_document(db, "bills", [("contractId", "==", contract_id), ("status", "in", ["UNPAID", "OVERDUE"])], "issuedDate")

        building_data = building_doc.to_dict() if building_doc and building_doc.exists else None
        room_data = room_doc.to_dict() if room_doc and room_doc.exists else None

        if building_data:
            building_data['services'] = services
        if room_data:
            room_data['extraService'] = extra_services

        dashboard_data = {
            "user": user_doc.to_dict(),
            "contract": contract_data,
            "room": room_data,
            "building": building_data,
            "lastApprovedReading": {**last_reading_doc.to_dict(), 'id': last_reading_doc.id} if last_reading_doc else None,
            "currentBill": {**current_bill_doc.to_dict(), 'id': current_bill_doc.id} if current_bill_doc else None,
        }

        logger.info(f"Tenant dashboard fetched: user={tenant_id}, contract={contract_id}")
        return {"status": "success", "data": dashboard_data}

    except Exception as e:
        logger.error(f"getTenantDashboardData failed: user={tenant_id}, error={e}", exc_info=True)
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An error occurred while fetching dashboard data.")
