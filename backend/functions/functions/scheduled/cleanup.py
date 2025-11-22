"""Scheduled job for cleaning up expired QR sessions."""

from datetime import datetime, timedelta, timezone
from firebase_functions import scheduler_fn

from config import db
from models import Status


@scheduler_fn.on_schedule(schedule="every day 01:00", timezone="Asia/Ho_Chi_Minh")
def cleanupExpiredQrSessions(event: scheduler_fn.ScheduledEvent) -> None:
    print("Running scheduled job: cleanupExpiredQrSessions")
    try:
        now = datetime.now(timezone.utc)
        one_week_ago = now - timedelta(days=7)
        expired_query = db.collection("qr_sessions").where("expiresAt", "<=", now)
        stale_scanned_query = db.collection("qr_sessions").where("status", "==", Status.SCANNED).where("scannedAt", "<=", one_week_ago)

        docs_to_delete_refs = set()
        for query in [expired_query, stale_scanned_query]:
            for doc in query.stream():
                docs_to_delete_refs.add(doc.reference)

        if not docs_to_delete_refs:
            print("No expired/stale sessions to clean up.")
            return

        batch, count = db.batch(), 0
        for doc_ref in docs_to_delete_refs:
            batch.delete(doc_ref)
            count += 1
            if count >= 499:
                batch.commit()
                batch = db.batch()
        if count % 499 > 0: batch.commit()

        print(f"Scheduled job finished. Cleaned up {len(docs_to_delete_refs)} sessions.")
    except Exception as e:
        print(f"An error occurred during scheduled job: {e}")
