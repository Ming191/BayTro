"""SePay webhook handler."""

import os
from firebase_functions import https_fn as https, options
from firebase_admin import firestore

from config import db
from utils import send_fcm_notification, update_bill_in_transaction


options.set_global_options(secrets=["SEPAY_API_KEY"])


@https.on_request(secrets=["SEPAY_API_KEY"])
def handleSePayWebhook(req: https.Request) -> https.Response:
    """Handle SePay webhook to update bill payment status."""
    try:
        expected_api_key = os.environ.get("SEPAY_API_KEY")
        if not expected_api_key:
            print("[CRITICAL] Secret 'SEPAY_API_KEY' not found in environment variables.")
            return https.Response("Server Configuration Error", status=500)
    except Exception as e:
        print(f"[CRITICAL] Error reading SEPAY_API_KEY: {e}")
        return https.Response("Server Configuration Error", status=500)

    auth_header = req.headers.get("Authorization", "")
    if not auth_header.startswith("Apikey "):
        print(f"[SECURITY] Unauthorized attempt: Invalid Authorization header format. IP: {req.remote_addr}")
        return https.Response("Unauthorized", status=401)

    received_api_key = auth_header.split(" ")[1]
    if received_api_key != expected_api_key:
        print(f"[SECURITY] Unauthorized webhook attempt. IP: {req.remote_addr}")
        return https.Response("Unauthorized", status=401)

    if req.method != "POST":
        return https.Response("Method Not Allowed", status=405)

    try:
        data = req.get_json()
        print(f"[INFO] Received webhook data: {data}")

        payment_code = data.get("code")
        if not payment_code:
            content = data.get("content", "").strip()
            if content:
                payment_code = content.split(" ")[0]
        transfer_amount = data.get("transferAmount")
        sepay_transaction_id = data.get("id")

        if not all([payment_code, isinstance(transfer_amount, (int, float)), sepay_transaction_id is not None]):
            print("[WARN] Webhook ignored: Missing or invalid key fields.")
            return https.Response('{"success": true}', status=200, mimetype='application/json')

        processed_query = db.collection("bills").where("sepayTransactionId", "==", sepay_transaction_id).limit(1).get()
        if processed_query:
            print(f"[INFO] Webhook ignored: SePay transaction ID {sepay_transaction_id} already processed.")
            return https.Response('{"success": true}', status=200, mimetype='application/json')
            
        bills_query = db.collection("bills").where("paymentCode", "==", payment_code).limit(1).get()
        
        if not bills_query:
            print(f"[WARN] Webhook ignored: No bill found with paymentCode '{payment_code}'.")
            return https.Response('{"success": true}', status=200, mimetype='application/json')

        bill_doc_snapshot = bills_query[0]
        bill_data = bill_doc_snapshot.to_dict()
        bill_ref = bill_doc_snapshot.reference

        if bill_data.get("status") == "PAID":
            print(f"[INFO] Webhook ignored: Bill {bill_ref.id} is already PAID.")
            return https.Response('{"success": true}', status=200, mimetype='application/json')

        if abs(bill_data.get("totalAmount", 0) - transfer_amount) > 1:
            print(f"[ALERT] Amount mismatch for bill {bill_ref.id}. Expected {bill_data.get('totalAmount')}, got {transfer_amount}.")
            send_fcm_notification(bill_data.get("landlordId"), "Payment Alert: Amount Mismatch", f"A payment for {bill_data.get('roomName', 'a room')} was received, but the amount was incorrect.")
            return https.Response('{"success": true}', status=200, mimetype='application/json')

        transaction = db.transaction()
        update_data = {
            "status": "PAID",
            "paymentDate": firestore.SERVER_TIMESTAMP,
            "paidAmount": transfer_amount,
            "paymentMethod": "BANK_TRANSFER_AUTO",
            "sepayTransactionId": sepay_transaction_id
        }

        original_bill_data = update_bill_in_transaction(transaction, bill_ref, update_data)

        if original_bill_data:
            landlord_id = original_bill_data.get("landlordId")
            tenant_ids = original_bill_data.get("tenantIds", [])
            room_name = original_bill_data.get("roomName", "your room")

            send_fcm_notification(landlord_id, "Payment Received", f"Payment for {room_name} has been confirmed automatically.")
            for tenant_id in tenant_ids:
                send_fcm_notification(tenant_id, "Payment Successful", f"Your payment for {room_name} has been confirmed.")

            print(f"[SUCCESS] Bill {bill_ref.id} updated to PAID.")

        return https.Response('{"success": true}', status=200, mimetype='application/json')

    except Exception as e:
        print(f"[CRITICAL] Error processing webhook: {e}")
        # Trả về 500 để SePay có thể retry
        return https.Response("Internal Server Error", status=500)
