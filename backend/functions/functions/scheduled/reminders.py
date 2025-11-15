"""Scheduled job for handling meter reading deadline reminders."""

from datetime import datetime, timezone
from firebase_functions import scheduler_fn

from config import db
from models import Status
from utils import send_fcm_notification, get_room_number


@scheduler_fn.on_schedule(schedule="every day 00:30", timezone="Asia/Ho_Chi_Minh")
def handleMeterReadingDeadlines(event: scheduler_fn.ScheduledEvent) -> None:
    print("Running scheduled job: handleMeterReadingDeadlines")
    try:
        now = datetime.now(timezone.utc)
        today = now.day

        for building_doc in db.collection("buildings").stream():
            building_data = building_doc.to_dict()
            building_id = building_doc.id
            billing_date = building_data.get("billingDate")

            if not billing_date:
                continue

            reminder_start_day = billing_date - 3
            if reminder_start_day <= today < billing_date:
                contracts_query = db.collection("contracts").where("buildingId", "==", building_id).where("status", "==", Status.CONTRACT_ACTIVE)
                for contract_doc in contracts_query.stream():
                    reading_query = db.collection("meter_readings") \
                        .where("contractId", "==", contract_doc.id) \
                        .where("createdAt", ">=", datetime(now.year, now.month, 1, tzinfo=timezone.utc))

                    if not list(reading_query.stream()):
                        tenant_ids = contract_doc.to_dict().get("tenantIds", [])
                        days_left = billing_date - today
                        for tenant_id in tenant_ids:
                            send_fcm_notification(
                                user_id=tenant_id,
                                title="Reminder: Submit Meter Readings",
                                body=f"You have {days_left} day(s) left to submit this month's readings. The deadline is day {billing_date}."
                            )
                        print(f"Sent reminder to tenants in contract {contract_doc.id}")

            if today == billing_date:
                contracts_query = db.collection("contracts").where("buildingId", "==", building_id).where("status", "==", Status.CONTRACT_ACTIVE)
                for contract_doc in contracts_query.stream():
                    reading_query = db.collection("meter_readings") \
                        .where("contractId", "==", contract_doc.id) \
                        .where("createdAt", ">=", datetime(now.year, now.month, 1, tzinfo=timezone.utc))

                    if not list(reading_query.stream()):
                        landlord_id = contract_doc.to_dict().get("landlordId")
                        room_id = contract_doc.to_dict().get("roomId")
                        room_number = get_room_number(room_id)
                        send_fcm_notification(
                            user_id=landlord_id,
                            title="Alert: Room Missed Submission",
                            body=f"Room {room_number} has missed the meter reading submission deadline. Their bill will not include electricity/water costs."
                        )
                        print(f"Sent alert to landlord for contract {contract_doc.id}")

        print("Scheduled job handleMeterReadingDeadlines finished.")
    except Exception as e:
        print(f"An error occurred during scheduled job handleMeterReadingDeadlines: {e}")
