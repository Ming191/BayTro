"""Bill management endpoints."""

from firebase_functions import https_fn as https
from firebase_admin import firestore

from config import db
from utils import send_fcm_notification, update_bill_in_transaction


@https.on_call()
def markBillAsPaid(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    landlord_uid = req.auth.uid
    bill_id = req.data.get("billId")
    paid_amount = req.data.get("paidAmount")
    payment_method = req.data.get("paymentMethod")
    if not all([isinstance(bill_id, str), isinstance(paid_amount, (int, float)), isinstance(payment_method, str)]):
        raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="Invalid arguments provided (billId, paidAmount, paymentMethod).")
    if paid_amount <= 0:
        raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="Paid amount must be positive.")

    try:
        bill_ref = db.collection("bills").document(bill_id)

        @firestore.transactional
        def _update_bill(transaction):
            bill_doc = bill_ref.get(transaction=transaction)

            if not bill_doc.exists:
                raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="Bill not found.")

            bill_data = bill_doc.to_dict()
            if bill_data.get("landlordId") != landlord_uid:
                raise https.HttpsError(code=https.FunctionsErrorCode.PERMISSION_DENIED, message="You do not have permission to modify this bill.")

            current_status = bill_data.get("status")
            if current_status == "PAID":
                print(f"Bill {bill_id} is already marked as PAID. No action taken.")
                return False, []

            if current_status not in ["UNPAID", "OVERDUE"]:
                 raise https.HttpsError(code=https.FunctionsErrorCode.FAILED_PRECONDITION, message=f"Cannot mark bill as paid. Current status is '{current_status}'.")

            transaction.update(bill_ref, {
                "status": "PAID",
                "paymentDate": firestore.SERVER_TIMESTAMP,
                "paidAmount": paid_amount,
                "paymentMethod": payment_method.upper()
            })

            print(f"Bill {bill_id} successfully updated to PAID.")
            return True, bill_data.get("tenantIds", [])

        transaction = db.transaction()
        updated, tenant_ids_to_notify = _update_bill(transaction)

        if updated and tenant_ids_to_notify:
            bill_doc_after_update = bill_ref.get()
            room_name = bill_doc_after_update.to_dict().get("roomName", "your room")
            for tenant_id in tenant_ids_to_notify:
                send_fcm_notification(
                    user_id=tenant_id,
                    title="Payment Confirmed",
                    body=f"Your landlord has confirmed the payment for the bill of {room_name}."
                )
            print(f"Sent 'paid' notification to {len(tenant_ids_to_notify)} tenant(s).")
        return {"status": "success", "message": "Bill status updated successfully."}
    except https.HttpsError as e:
        raise e
    except Exception as e:
        print(f"Error in markBillAsPaid: {e}")
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An internal error occurred.")


@https.on_call()
def addManualChargeToBill(req: https.CallableRequest) -> dict:
    """
    Cho phép chủ nhà thêm một khoản phí thủ công (line item) vào một hóa đơn
    chưa được thanh toán.
    """
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    landlord_uid = req.auth.uid
    bill_id = req.data.get("billId")
    description = req.data.get("description")
    amount = req.data.get("amount")

    if not all([isinstance(bill_id, str), isinstance(description, str), isinstance(amount, (int, float))]):
        raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="Invalid arguments: 'billId', 'description', and 'amount' are required.")
    if not description.strip() or amount <= 0:
        raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="Description cannot be empty and amount must be positive.")

    try:
        bill_ref = db.collection("bills").document(bill_id)

        @firestore.transactional
        def _add_charge(transaction):
            bill_doc = bill_ref.get(transaction=transaction)

            if not bill_doc.exists:
                raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="Bill not found.")

            bill_data = bill_doc.to_dict()
            if bill_data.get("landlordId") != landlord_uid:
                raise https.HttpsError(code=https.FunctionsErrorCode.PERMISSION_DENIED, message="You do not have permission to modify this bill.")

            current_status = bill_data.get("status")
            if current_status == "PAID":
                raise https.HttpsError(code=https.FunctionsErrorCode.FAILED_PRECONDITION, message="Cannot add charges to a bill that has already been paid.")

            manual_item = {
                "description": description.strip(),
                "quantity": 1,
                "pricePerUnit": float(amount),
                "totalCost": float(amount),
                "isManual": True
            }

            transaction.update(bill_ref, {
                "lineItems": firestore.ArrayUnion([manual_item]),
                "totalAmount": firestore.Increment(float(amount))
            })
            
            print(f"Manual charge '{description}' added to bill {bill_id}.")
            return bill_data.get("tenantIds", [])
        
        transaction = db.transaction()
        tenant_ids_to_notify = _add_charge(transaction)

        if tenant_ids_to_notify:
            bill_doc_after_update = bill_ref.get()
            room_name = bill_doc_after_update.to_dict().get("roomName", "your room")
            new_total = bill_doc_after_update.to_dict().get("totalAmount")

            for tenant_id in tenant_ids_to_notify:
                send_fcm_notification(
                    user_id=tenant_id,
                    title="Bill Updated",
                    body=f"A new charge ('{description.strip()}') has been added to your bill for {room_name}. The new total is {new_total:,.0f} ₫."
                )
            print(f"Sent 'charge added' notification to {len(tenant_ids_to_notify)} tenant(s).")

        return {"status": "success", "message": "Charge added successfully."}

    except https.HttpsError as e:
        raise e
    except Exception as e:
        print(f"Error in addManualChargeToBill: {e}")
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An internal error occurred.")


@https.on_call()
def sendBillPaymentReminder(request: https.CallableRequest) -> dict:
    try:
        if not request.auth:
            raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User must be authenticated.")

        landlord_id = request.auth.uid
        data = request.data
        bill_id = data.get("billId")
        custom_message = data.get("customMessage", "")

        if not bill_id:
            raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="billId is required.")

        print(f"[sendBillPaymentReminder] Landlord {landlord_id} sending payment reminder for bill {bill_id}")

        bill_ref = db.collection("bills").document(bill_id)
        bill_snapshot = bill_ref.get()

        if not bill_snapshot.exists:
            raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="Bill not found.")

        bill_data = bill_snapshot.to_dict()

        if bill_data.get("landlordId") != landlord_id:
            raise https.HttpsError(code=https.FunctionsErrorCode.PERMISSION_DENIED, message="You do not have permission to send reminders for this bill.")

        bill_status = bill_data.get("status")
        if bill_status == "PAID":
            raise https.HttpsError(code=https.FunctionsErrorCode.FAILED_PRECONDITION, message="Cannot send reminder for a bill that has already been paid.")

        tenant_ids = bill_data.get("tenantIds", [])
        if not tenant_ids:
            raise https.HttpsError(code=https.FunctionsErrorCode.FAILED_PRECONDITION, message="No tenants associated with this bill.")

        room_name = bill_data.get("roomName", "your room")
        total_amount = bill_data.get("totalAmount", 0)
        payment_due_date = bill_data.get("paymentDueDate", "N/A")
        payment_code = bill_data.get("paymentCode", "N/A")
        month = bill_data.get("month", "N/A")
        year = bill_data.get("year", "N/A")

        notification_title = "Payment Reminder"
        
        if custom_message:
            notification_body = custom_message
        else:
            notification_body = (
                f"Reminder: Your bill for {room_name} ({month}/{year}) is due on {payment_due_date}. "
                f"Total amount: {total_amount:,.0f} ₫. Payment code: {payment_code}. "
                f"Please make payment at your earliest convenience."
            )

        notification_data = {
            "type": "bill_reminder",
            "billId": bill_id,
            "roomName": room_name,
            "totalAmount": str(total_amount),
            "paymentDueDate": payment_due_date,
            "paymentCode": payment_code,
            "month": str(month),
            "year": str(year)
        }

        notifications_sent = 0
        for tenant_id in tenant_ids:
            try:
                send_fcm_notification(
                    user_id=tenant_id,
                    title=notification_title,
                    body=notification_body,
                    data=notification_data
                )
                notifications_sent += 1
            except Exception as e:
                print(f"[ERROR] Failed to send notification to tenant {tenant_id}: {e}")

        print(f"[sendBillPaymentReminder] Sent {notifications_sent}/{len(tenant_ids)} notifications for bill {bill_id}")

        return {
            "status": "success",
            "message": f"Payment reminder sent to {notifications_sent} tenant(s).",
            "notificationsSent": notifications_sent,
            "totalTenants": len(tenant_ids)
        }

    except https.HttpsError as e:
        raise e
    except Exception as e:
        print(f"[ERROR] in sendBillPaymentReminder: {e}")
        import traceback
        traceback.print_exc()
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An internal error occurred while sending the payment reminder.")
