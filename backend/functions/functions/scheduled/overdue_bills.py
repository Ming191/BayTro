"""Scheduled job for checking and marking overdue bills."""

from datetime import datetime, timezone
from firebase_functions import scheduler_fn

from config import db
from models import Status
from utils import send_fcm_notification


@scheduler_fn.on_schedule(schedule="every day 02:00", timezone="Asia/Ho_Chi_Minh")
def checkAndMarkOverdueBills(event: scheduler_fn.ScheduledEvent) -> None:
    """
    Runs daily to find unpaid bills that are past their due date and marks them as OVERDUE.
    It also sends notifications to both the landlord and the tenants.
    """
    print("----- START: Scheduled job 'checkAndMarkOverdueBills' -----")
    try:
        now = datetime.now(timezone.utc)
        print(f"Job running at {now.strftime('%Y-%m-%d %H:%M:%S UTC')}. Checking for overdue bills.")

        overdue_query = db.collection("bills") \
            .where("status", "==", Status.BILL_UNPAID) \
            .where("paymentDueDate", "<", now)

        overdue_bills = list(overdue_query.stream())

        if not overdue_bills:
            print("No unpaid bills are overdue. Job finished.")
            print("----- END: Scheduled job 'checkAndMarkOverdueBills' -----")
            return

        print(f"Found {len(overdue_bills)} bill(s) to mark as overdue.")
        batch = db.batch()

        for bill_doc in overdue_bills:
            bill_ref = bill_doc.reference
            bill_data = bill_doc.to_dict()
            room_name = bill_data.get("roomName", "your room")

            print(f"  -> Processing bill {bill_ref.id} for room '{room_name}'.")

            # Add the update operation to the batch
            batch.update(bill_ref, {"status": Status.BILL_OVERDUE})

            # Send a notification to the landlord
            landlord_id = bill_data.get("landlordId")
            if landlord_id:
                send_fcm_notification(
                    user_id=landlord_id,
                    title="Bill Overdue",
                    body=f"The bill for room {room_name} is now overdue. Please follow up."
                )

            # Send a notification to all tenants in the contract
            tenant_ids = bill_data.get("tenantIds", [])
            for tenant_id in tenant_ids:
                send_fcm_notification(
                    user_id=tenant_id,
                    title="Your Bill is Overdue",
                    body=f"Your bill for {room_name} is now overdue. Please make the payment as soon as possible."
                )

        # Commit all the updates at once
        batch.commit()
        print(f"Successfully processed and updated {len(overdue_bills)} overdue bills.")

    except Exception as e:
        print(f"\n[CRITICAL ERROR] An error occurred during scheduled job checkAndMarkOverdueBills: {e}")
    finally:
        print("----- END: Scheduled job 'checkAndMarkOverdueBills' -----")
