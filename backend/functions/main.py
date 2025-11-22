import os
import string
from random import random
from datetime import datetime, timedelta, timezone

import firebase_admin
from firebase_admin import firestore, messaging, credentials
from firebase_functions import https_fn as https, scheduler_fn, options

cred = credentials.Certificate("service-account.json")
firebase_admin.initialize_app(cred)
db = firestore.client()

QR_CODE_VALIDITY_MINUTES = 5

class Status:
    """Status constants for various entities in the system."""
    PENDING = "PENDING"
    SCANNED = "SCANNED"
    CONFIRMED = "CONFIRMED"
    DECLINED = "DECLINED"
    EXPIRED = "EXPIRED"
    DELETED = "DELETED"
    METER_PENDING = "PENDING"
    METER_APPROVED = "APPROVED"
    METER_DECLINED = "DECLINED"
    CONTRACT_ACTIVE = "ACTIVE"
    BILL_UNPAID = "UNPAID"
    BILL_PAID = "PAID"
    BILL_OVERDUE = "OVERDUE"
    BILL_NOT_ISSUED_YET = "NOT_ISSUED_YET"


# ============================================================================
# HELPER FUNCTIONS
# ============================================================================

def _send_fcm_notification(user_id: str, title: str, body: str, data: dict = None):
    """Send Firebase Cloud Messaging notification to a user.

    Args:
        user_id: The ID of the user to send notification to
        title: Notification title
        body: Notification body text
        data: Optional dictionary of additional data to include
    """
    if not user_id:
        print(f"[FCM] Warning: user_id is empty, cannot send notification")
        return

    print(f"[FCM] Attempting to send notification to user: {user_id}")
    print(f"[FCM] Title: {title}")
    print(f"[FCM] Body: {body}")
    print(f"[FCM] Data: {data}")

    try:
        user_doc = db.collection("users").document(user_id).get()

        if not user_doc.exists:
            print(f"[FCM] ERROR: User document {user_id} does not exist!")
            return

        user_data = user_doc.to_dict()
        fcm_token = user_data.get("fcmToken")

        print(f"[FCM] User document exists: {user_doc.exists}")
        print(f"[FCM] FCM token in database: {fcm_token[:20] if fcm_token else 'None'}...")

        if fcm_token:
            cleaned_data = {}
            if data:
                for key, value in data.items():
                    if value is not None:
                        cleaned_data[key] = str(value)

            print(f"[FCM] Cleaned data payload: {cleaned_data}")

            message = messaging.Message(
                notification=messaging.Notification(title=title, body=body),
                data=cleaned_data,
                token=fcm_token
            )

            print(f"[FCM] Sending message with token: {fcm_token[:20]}...")
            response = messaging.send(message)
            print(f"[FCM] Notification sent successfully! Response: {response}")
        else:
            print(f"[FCM] Warning: User {user_id} has no FCM token in database.")
    except Exception as e:
        print(f"[FCM] Failed to send notification to user {user_id}.")
        print(f"[FCM] Error type: {type(e).__name__}")
        print(f"[FCM] Error message: {str(e)}")
        import traceback
        print(f"[FCM] Traceback: {traceback.format_exc()}")


def _get_user_name(user_id: str, default: str = "A user") -> str:
    """Fetch user's full name from database.

    Args:
        user_id: The user ID to lookup
        default: Default value if user not found or has no name

    Returns:
        User's full name or default value
    """
    if not user_id:
        return default
    try:
        doc = db.collection("users").document(user_id).get()
        return doc.to_dict().get("fullName", default) if doc.exists else default
    except:
        return default


def _get_room_number(room_id: str, default: str = "a room") -> str:
    """Fetch room number from database.

    Args:
        room_id: The room ID to lookup
        default: Default value if room not found

    Returns:
        Room number or default value
    """
    if not room_id:
        return default
    try:
        doc = db.collection("rooms").document(room_id).get()
        return doc.to_dict().get("roomNumber", default) if doc.exists else default
    except:
        return default


def _calculate_payment_dates(now, building_data):
    """Calculate payment start and due dates for next billing cycle.

    Args:
        now: Current datetime
        building_data: Building document data containing paymentStart and paymentDue

    Returns:
        Tuple of (payment_start_date, payment_due_date)
    """
    current_month = now.month
    current_year = now.year

    next_month = current_month + 1
    next_year = current_year
    if next_month > 12:
        next_month = 1
        next_year += 1

    payment_start_day = building_data.get("paymentStart", 1)
    payment_due_day = building_data.get("paymentDue", 5)

    payment_start_date = datetime(next_year, next_month, payment_start_day, tzinfo=timezone.utc)
    payment_due_date = datetime(next_year, next_month, payment_due_day, tzinfo=timezone.utc)
    return payment_start_date, payment_due_date


@firestore.transactional
def _update_bill_in_transaction(transaction, bill_ref, update_data):
    """Helper to update a bill inside a transaction.

    Args:
        transaction: Firestore transaction object
        bill_ref: Bill document reference
        update_data: Dictionary of fields to update

    Returns:
        Previous bill snapshot (dict) or None if already PAID
    """
    bill_snapshot = bill_ref.get(transaction=transaction)
    if bill_snapshot.exists and bill_snapshot.to_dict().get("status") == "PAID":
        print(f"[INFO] Transaction aborted: Bill {bill_ref.id} was already marked as PAID.")
        return None

    transaction.update(bill_ref, update_data)
    return bill_snapshot.to_dict()


# ============================================================================
# API ENDPOINTS - QR CODE & TENANT LINKING
# ============================================================================

@https.on_call()
def generateQrSession(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(
            code=https.FunctionsErrorCode.UNAUTHENTICATED,
            message="User not authenticated."
        )

    uid = req.auth.uid
    contract_id = req.data.get("contractId")

    if not isinstance(contract_id, str) or not contract_id:
        raise https.HttpsError(
            code=https.FunctionsErrorCode.INVALID_ARGUMENT,
            message="Invalid 'contractId'."
        )

    try:
        contract_doc = db.collection("contracts").document(contract_id).get()
        if not contract_doc.exists or contract_doc.to_dict().get("landlordId") != uid:
            raise https.HttpsError(
                code=https.FunctionsErrorCode.PERMISSION_DENIED,
                message="User does not have permission for this contract."
            )

        expiration_time = datetime.now(timezone.utc) + timedelta(minutes=QR_CODE_VALIDITY_MINUTES)
        session_data = {
            "contractId": contract_id,
            "inviterId": uid,
            "status": Status.PENDING,
            "createdAt": firestore.SERVER_TIMESTAMP,
            "expiresAt": expiration_time
        }
        _, session_ref = db.collection("qr_sessions").add(session_data)

        return {"status": "success", "data": {"sessionId": session_ref.id}}

    except https.HttpsError as e:
        raise e
    except Exception as e:
        print(f"Error in generateQrSession: {e}")
        raise https.HttpsError(
            code=https.FunctionsErrorCode.INTERNAL,
            message="An internal error occurred."
        )

@https.on_call()
def processQrScan(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(
            code=https.FunctionsErrorCode.UNAUTHENTICATED,
            message="User not authenticated."
        )

    tenant_uid = req.auth.uid
    session_id = req.data.get("sessionId")

    if not isinstance(session_id, str) or not session_id:
        raise https.HttpsError(
            code=https.FunctionsErrorCode.INVALID_ARGUMENT,
            message="Invalid 'sessionId'."
        )

    try:
        session_ref = db.collection("qr_sessions").document(session_id)
        session_doc = session_ref.get()

        if not session_doc.exists:
            raise https.HttpsError(
                code=https.FunctionsErrorCode.NOT_FOUND,
                message="QR code is invalid or expired."
            )

        session_data = session_doc.to_dict()

        if session_data.get("status") != Status.PENDING:
            status = session_data.get("status", "unknown")
            message = "QR code has been used." if status != Status.EXPIRED else "QR code has expired."
            code = https.FunctionsErrorCode.FAILED_PRECONDITION if status != Status.EXPIRED else https.FunctionsErrorCode.DEADLINE_EXCEEDED
            raise https.HttpsError(code=code, message=message)

        if session_data.get("expiresAt") is None or datetime.now(timezone.utc) > session_data.get("expiresAt"):
            session_ref.update({"status": Status.EXPIRED})
            raise https.HttpsError(
                code=https.FunctionsErrorCode.DEADLINE_EXCEEDED,
                message="QR code has expired."
            )

        session_ref.update({
            "scannedByTenantId": tenant_uid,
            "status": Status.SCANNED,
            "scannedAt": firestore.SERVER_TIMESTAMP
        })

        _send_fcm_notification(
            user_id=session_data.get("inviterId"),
            title="New Join Request",
            body="A tenant has scanned your invitation.",
            data={
                "screen": "ContractDetails",
                "contractId": session_data.get("contractId")
            }
        )

        return {"status": "success", "message": "Scan successful. Please wait for confirmation."}

    except https.HttpsError as e:
        raise e
    except Exception as e:
        print(f"Error in processQrScan: {e}")
        raise https.HttpsError(
            code=https.FunctionsErrorCode.INTERNAL,
            message="An internal server error occurred."
        )

@https.on_call()
def confirmTenantLink(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    landlord_uid, session_id = req.auth.uid, req.data.get("sessionId")
    if not isinstance(session_id, str) or not session_id: raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="Invalid 'sessionId'.")
    try:
        @firestore.transactional
        def _transaction(transaction):
            session_ref = db.collection("qr_sessions").document(session_id)
            session_doc = session_ref.get(transaction=transaction)
            if not session_doc.exists: raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="Session does not exist.")
            session_data = session_doc.to_dict()
            if session_data.get("inviterId") != landlord_uid: raise https.HttpsError(code=https.FunctionsErrorCode.PERMISSION_DENIED, message="You cannot confirm this invitation.")
            if session_data.get("status") != Status.SCANNED: raise https.HttpsError(code=https.FunctionsErrorCode.FAILED_PRECONDITION, message="Invitation not ready for confirmation.")
            tenant_id, contract_id = session_data.get("scannedByTenantId"), session_data.get("contractId")
            if not tenant_id or not contract_id: raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="Session data is corrupted.")
            active_contracts_query = db.collection("contracts").where("status", "==", Status.CONTRACT_ACTIVE).where("tenantIds", "array_contains", tenant_id)
            if len(list(active_contracts_query.stream(transaction=transaction))) > 0:
                raise https.HttpsError(code=https.FunctionsErrorCode.FAILED_PRECONDITION, message="This tenant is already in another active contract.")
            transaction.update(db.collection("contracts").document(contract_id), {"tenantIds": firestore.ArrayUnion([tenant_id]), "status": Status.CONTRACT_ACTIVE})
            transaction.update(session_ref, {"status": Status.CONFIRMED, "confirmedAt": firestore.SERVER_TIMESTAMP})
            return tenant_id, contract_id
        tenant_id, contract_id = _transaction(db.transaction())
        _send_fcm_notification(user_id=tenant_id, title="Invitation Confirmed!", body="You have been successfully added to the contract.", data={"screen": "ContractDetails", "contractId": contract_id})
        return {"status": "success", "message": "Tenant linked successfully."}
    except https.HttpsError as e: raise e
    except Exception as e:
        print(f"Error in confirmTenantLink: {e}")
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="A critical error occurred.")

@https.on_call()
def declineTenantLink(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    landlord_uid, session_id = req.auth.uid, req.data.get("sessionId")
    if not isinstance(session_id, str) or not session_id:
        raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="Invalid 'sessionId'.")
    try:
        session_ref = db.collection("qr_sessions").document(session_id)
        session_doc = session_ref.get()
        if not session_doc.exists: raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="Session does not exist.")
        session_data = session_doc.to_dict()
        if session_data.get("inviterId") != landlord_uid:
            raise https.HttpsError(code=https.FunctionsErrorCode.PERMISSION_DENIED, message="You cannot decline this invitation.")
        if session_data.get("status") != Status.SCANNED:
            raise https.HttpsError(code=https.FunctionsErrorCode.FAILED_PRECONDITION, message="This invitation cannot be declined.")
        session_ref.update({"status": Status.DECLINED, "declinedAt": firestore.SERVER_TIMESTAMP})
        return {"status": "success", "message": "Request has been declined."}
    except https.HttpsError as e: raise e
    except Exception as e:
        print(f"Error in declineTenantLink: {e}")
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An internal error occurred.")

@https.on_call()
def updateFcmToken(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    uid, new_token = req.auth.uid, req.data.get("fcmToken")

    print(f"[FCM Token Update] Received request for user: {uid}")
    print(f"[FCM Token Update] New token: {new_token[:20] if new_token else 'None'}...")
    print(f"[FCM Token Update] Token length: {len(new_token) if new_token else 0}")

    if not isinstance(new_token, str) or not new_token:
        print(f"[FCM Token Update] ❌ Invalid token received")
        raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="Invalid 'fcmToken'.")

    try:
        print(f"[FCM Token Update] Updating token in Firestore for user {uid}...")
        db.collection("users").document(uid).update({"fcmToken": new_token})
        print(f"[FCM Token Update] ✅ Token updated successfully in Firestore")

        # Verify the token was saved
        user_doc = db.collection("users").document(uid).get()
        saved_token = user_doc.to_dict().get("fcmToken") if user_doc.exists else None
        print(f"[FCM Token Update] Verification - Token in DB: {saved_token[:20] if saved_token else 'None'}...")

        return {"status": "success"}
    except Exception as e:
        print(f"[FCM Token Update] ❌ Error updating FCM token for user {uid}: {e}")
        import traceback
        print(f"[FCM Token Update] Traceback: {traceback.format_exc()}")
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="Failed to update token.")

@https.on_call()
def notifyNewMeterReading(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    reading_id = req.data.get("readingId")
    if not isinstance(reading_id, str) or not reading_id:
        raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="Invalid 'readingId'.")
    try:
        reading_doc = db.collection("meter_readings").document(reading_id).get()
        if not reading_doc.exists: raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="Meter reading not found.")
        reading_data = reading_doc.to_dict()
        landlord_id, tenant_id, room_id = reading_data.get("landlordId"), reading_data.get("tenantId"), reading_data.get("roomId")
        if not all([landlord_id, tenant_id, room_id]): raise Exception("Reading data is incomplete.")
        tenant_name, room_number = _get_user_name(tenant_id), _get_room_number(room_id)
        _send_fcm_notification(user_id=landlord_id, title="New Meter Reading", body=f"{tenant_name} submitted a reading for room {room_number}.", data={"screen": "PendingRequests"})
        return {"status": "success", "message": "Notification sent."}
    except Exception as e:
        print(f"Error in notifyNewMeterReading: {e}")
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="Failed to send notification.")

@https.on_call()
def approveMeterReading(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    landlord_uid_caller = req.auth.uid
    reading_id = req.data.get("readingId")

    if not isinstance(reading_id, str) or not reading_id:
        raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="Invalid 'readingId'.")

    try:
        @firestore.transactional
        def _transaction(transaction):
            reading_ref = db.collection("meter_readings").document(reading_id)
            reading_doc = reading_ref.get(transaction=transaction)
            if not reading_doc.exists:
                raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="Meter reading not found.")

            reading_data = reading_doc.to_dict()
            if reading_data.get("status") != Status.METER_PENDING:
                raise https.HttpsError(code=https.FunctionsErrorCode.FAILED_PRECONDITION, message="This reading is not in a pending state.")

            contract_id = reading_data.get("contractId")
            contract_ref = db.collection("contracts").document(contract_id)
            contract_doc = contract_ref.get(transaction=transaction)
            contract_data = contract_doc.to_dict()

            landlord_id = contract_data.get("landlordId")
            if not contract_doc.exists or landlord_id != landlord_uid_caller:
                raise https.HttpsError(code=https.FunctionsErrorCode.PERMISSION_DENIED, message="You do not have permission to approve this reading.")

            building_id = contract_data.get("buildingId")
            if not building_id:
                raise Exception(f"Contract {contract_id} is missing a buildingId.")

            building_doc = db.collection("buildings").document(building_id).get(transaction=transaction)
            if not building_doc.exists:
                raise Exception(f"Building {building_id} not found.")

            billing_date = building_doc.to_dict().get("billingDate", 25)

            now = datetime.now(timezone.utc)
            bill_month, bill_year = now.month, now.year

            if now.day > billing_date:
                print(f"Approval date (day {now.day}) is after billing date (day {billing_date}). Assigning to next month's bill.")
                next_month_date = now.replace(day=28) + timedelta(days=4)
                bill_month, bill_year = next_month_date.month, next_month_date.year

            bill_month_str = f"{bill_year:04d}-{bill_month:02d}"
            bill_id = f"{bill_month_str}-{contract_id}"

            bill_ref = db.collection("bills").document(bill_id)
            bill_doc = bill_ref.get(transaction=transaction)
            if bill_doc.exists:
                bill_status = bill_doc.to_dict().get("status")
                if bill_status not in ["NOT_ISSUED_YET", None]:
                    raise https.HttpsError(code=https.FunctionsErrorCode.FAILED_PRECONDITION,
                                           message=f"Cannot add charges. The bill for {bill_month_str} has already been issued with status '{bill_status}'.")

            previous_reading_query = db.collection("meter_readings").where("contractId", "==", contract_id).where("status", "==", Status.METER_APPROVED).order_by("createdAt", direction=firestore.Query.DESCENDING).limit(1)
            previous_readings = list(previous_reading_query.stream(transaction=transaction))

            # Use initial readings from contract if no previous readings exist
            if not previous_readings:
                # First reading - use initial values from contract
                old_elec_value = contract_data.get("initialElectricityReading", 0)
                old_water_value = contract_data.get("initialWaterReading", 0)

                print(f"First meter reading for contract {contract_id}. Using initial values: elec={old_elec_value}, water={old_water_value}")
            else:
                # Subsequent readings - use previous approved reading
                old_elec_value = previous_readings[0].to_dict().get("electricityValue", 0)
                old_water_value = previous_readings[0].to_dict().get("waterValue", 0)

            room_id = contract_data.get("roomId")

            building_services_query = db.collection("buildings").document(building_id).collection("services").stream(transaction=transaction)
            building_services_list = [s.to_dict() for s in building_services_query]

            room_services_query = db.collection("rooms").document(room_id).collection("extraServices").stream(transaction=transaction)
            room_services_list = [s.to_dict() for s in room_services_query]

            all_services = building_services_list + room_services_list

            price_elec = next((int(s.get("price", "0")) for s in all_services if s.get("name", "").lower() == "electricity"), 0)
            price_water = next((int(s.get("price", "0")) for s in all_services if s.get("name", "").lower() == "water"), 0)

            new_elec_value = reading_data.get("electricityValue", 0)
            new_water_value = reading_data.get("waterValue", 0)
            elec_consumption = new_elec_value - old_elec_value
            water_consumption = new_water_value - old_water_value

            if elec_consumption < 0 or water_consumption < 0:
                raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="New reading cannot be less than previous.")

            elec_cost = elec_consumption * price_elec
            water_cost = water_consumption * price_water
            total_cost = elec_cost + water_cost

            bill_item_elec = {
                "description": f"Electricity ({old_elec_value} to {new_elec_value})",
                "quantity": elec_consumption,
                "pricePerUnit": price_elec,
                "totalCost": elec_cost,
                "readingId": reading_id
            }
            bill_item_water = {
                "description": f"Water ({old_water_value} to {new_water_value})",
                "quantity": water_consumption,
                "pricePerUnit": price_water,
                "totalCost": water_cost,
                "readingId": reading_id
            }

            transaction.set(bill_ref, {
                "month": bill_month,
                "year": bill_year,
                "status": "NOT_ISSUED_YET",
                "contractId": contract_id,
                "landlordId": landlord_id,
                "buildingId": building_id,
                "roomId": contract_data.get("roomId"),
                "roomName": reading_data.get("roomName"),
                "buildingName": reading_data.get("buildingName"),
                "lineItems": firestore.ArrayUnion([bill_item_elec, bill_item_water]),
                "totalAmount": firestore.Increment(total_cost)
            }, merge=True)

            transaction.update(reading_ref, {
                "status": Status.METER_APPROVED,
                "approvedAt": firestore.SERVER_TIMESTAMP,
                "electricityConsumption": elec_consumption,
                "waterConsumption": water_consumption,
                "electricityCost": elec_cost,
                "waterCost": water_cost,
                "totalCost": total_cost
            })

            return contract_data.get("tenantIds", []), contract_id

        tenant_ids_result, contract_id_result = _transaction(db.transaction())

        print(f"[approveMeterReading] Sending notifications to {len(tenant_ids_result)} tenant(s)")
        for tenant_id in tenant_ids_result:
            print(f"[approveMeterReading] Sending notification to tenant: {tenant_id}")
            _send_fcm_notification(
                user_id=tenant_id,
                title="Meter Reading Approved",
                body="Your meter reading has been approved by the landlord.",
                data={
                    "screen": "MeterReadingHistory",
                    "contractId": contract_id_result
                }
            )

        return {"status": "success", "message": "Reading approved and bill updated."}

    except https.HttpsError as e:
        raise e
    except Exception as e:
        print(f"A critical error occurred in approveMeterReading: {e}")
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An internal server error occurred.")

@https.on_call()
def declineMeterReading(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    landlord_uid, reading_id, reason = req.auth.uid, req.data.get("readingId"), req.data.get("reason")

    if not isinstance(reading_id, str) or not reading_id:
        raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="Invalid 'readingId'.")
    if not isinstance(reason, str) or not reason:
        raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="A reason must be provided.")
    try:
        reading_ref = db.collection("meter_readings").document(reading_id)
        reading_doc = reading_ref.get()
        if not reading_doc.exists: raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="Meter reading not found.")
        reading_data = reading_doc.to_dict()
        contract_doc = db.collection("contracts").document(reading_data.get("contractId")).get()
        if not contract_doc.exists or contract_doc.to_dict().get("landlordId") != landlord_uid: raise https.HttpsError(code=https.FunctionsErrorCode.PERMISSION_DENIED, message="You do not have permission for this contract.")
        if reading_data.get("status") != Status.METER_PENDING: raise https.HttpsError(code=https.FunctionsErrorCode.FAILED_PRECONDITION, message="This reading is not in a pending state.")
        reading_ref.update({"status": Status.METER_DECLINED, "declinedAt": firestore.SERVER_TIMESTAMP, "declineReason": reason})
        _send_fcm_notification(
            user_id=reading_data.get("tenantId"),
            title="Meter Reading Declined",
            body=f"Your reading was declined. Reason: {reason}",
            data={
                "screen": "MeterReadingHistory",
                "contractId": reading_data.get("contractId")
            }
        )
        return {"status": "success", "message": "The reading has been declined."}
    except https.HttpsError as e: raise e
    except Exception as e:
        print(f"Error in declineMeterReading: {e}")
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An internal error occurred.")

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


# ============================================================================
# SCHEDULED JOBS - METER READING & BILL GENERATION
# ============================================================================

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
                            _send_fcm_notification(
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
                        room_number = _get_room_number(room_id)
                        _send_fcm_notification(
                            user_id=landlord_id,
                            title="Alert: Room Missed Submission",
                            body=f"Room {room_number} has missed the meter reading submission deadline. Their bill will not include electricity/water costs."
                        )
                        print(f"Sent alert to landlord for contract {contract_doc.id}")

        print("Scheduled job handleMeterReadingDeadlines finished.")
    except Exception as e:
        print(f"An error occurred during scheduled job handleMeterReadingDeadlines: {e}")

@scheduler_fn.on_schedule(schedule="every day 01:00", timezone="Asia/Ho_Chi_Minh")
def generateMonthlyBills(event: scheduler_fn.ScheduledEvent) -> None:
    print("----- START: Scheduled job 'generateMonthlyBills' -----")
    try:
        now = datetime.now(timezone.utc)
        billing_day_of_month = now.day
        print(f"Job running for date: {now.strftime('%Y-%m-%d')}. Checking for billingDate: {billing_day_of_month}")

        due_buildings_query = db.collection("buildings").where("billingDate", "==", billing_day_of_month)
        due_buildings = list(due_buildings_query.stream())

        if not due_buildings:
            print("No buildings have a billing date set for today. Job finished.")
            return

        print(f"Found {len(due_buildings)} building(s) to process.")

        for building_doc in due_buildings:
            building_data = building_doc.to_dict()
            building_id = building_doc.id
            print(f"\n[Processing Building] ID: {building_id}, Name: {building_data.get('name', 'N/A')}")

            active_contracts_query = db.collection("contracts") \
                .where("buildingId", "==", building_id) \
                .where("status", "==", Status.CONTRACT_ACTIVE)

            for contract_doc in active_contracts_query.stream():
                contract_data = contract_doc.to_dict()
                contract_id = contract_doc.id
                print(f"  -> [Processing Contract] ID: {contract_id}")

                landlord_id = contract_data.get("landlordId")
                room_id = contract_data.get("roomId")
                tenant_ids = contract_data.get("tenantIds", [])
                number_of_tenants = len(tenant_ids)

                if not room_id or not landlord_id:
                    print(f"    [SKIP] Contract {contract_id} is missing roomId or landlordId.")
                    continue

                room_doc = db.collection("rooms").document(room_id).get()
                landlord_user_doc = db.collection("users").document(landlord_id).get()

                if not room_doc.exists:
                    print(f"    [SKIP] Contract {contract_id}: Room {room_id} not found.")
                    continue

                room_data = room_doc.to_dict()
                building_name = building_data.get("name", "N/A")
                room_name = room_data.get("roomNumber", "N/A")

                tenant_names_list = [name for t_id in tenant_ids if (name := _get_user_name(t_id, default=None)) is not None]
                all_tenants_name = ", ".join(tenant_names_list) if tenant_names_list else "N/A"

                line_items_to_add = []
                total_fixed_cost = 0

                # 1. Thêm tiền thuê nhà
                rental_fee = contract_data.get("rentalFee", 0)
                if rental_fee > 0:
                    line_items_to_add.append({"description": "Monthly Rent", "totalCost": rental_fee})
                    total_fixed_cost += rental_fee

                def process_service_list(services, num_tenants):
                    cost = 0
                    items = []
                    for service in services:
                        try:
                            price = int(service.get("price", "0"))
                            if price <= 0 or service.get("status") != "ACTIVE": continue

                            metric = service.get("metric", "").upper()
                            name = service.get("name")

                            if metric == "PERSON":
                                service_cost = price * num_tenants
                                description = f"{name} ({num_tenants} people)"
                                items.append({"description": description, "quantity": num_tenants, "pricePerUnit": price, "totalCost": service_cost})
                                cost += service_cost
                            elif metric not in ["KWH", "M3"]: # Bỏ qua điện nước
                                items.append({"description": name, "totalCost": price})
                                cost += price
                        except (ValueError, TypeError): continue
                    return items, cost

                building_services = [s.to_dict() for s in building_doc.reference.collection("services").stream()]
                building_service_items, building_service_cost = process_service_list(building_services, number_of_tenants)
                line_items_to_add.extend(building_service_items)
                total_fixed_cost += building_service_cost

                room_services = [s.to_dict() for s in room_doc.reference.collection("extraServices").stream()]
                room_service_items, room_service_cost = process_service_list(room_services, number_of_tenants)
                line_items_to_add.extend(room_service_items)
                total_fixed_cost += room_service_cost

                print(f"    Calculated {len(line_items_to_add)} fixed charge items, total: {total_fixed_cost}")

                # --- LOGIC TẠO MÃ THANH TOÁN ĐÃ SỬA LẠI ---
                bill_id = f"{now.strftime('%Y-%m')}-{contract_id}"
                payment_code = None
                if landlord_user_doc.exists:
                    landlord_data = landlord_user_doc.to_dict()
                    template = landlord_data.get("paymentCodeTemplate")

                    prefix = template.get("prefix", "BAYTRO").upper() if template else "BAYTRO"
                    suffix_length = int(template.get("suffixLength", 6)) if template else 6

                    suffix = contract_id[-suffix_length:].upper()
                    payment_code = f"{prefix}{suffix}"
                    print(f"    Generated Payment Code: {payment_code}")
                else:
                    print(f"    [ERROR] Cannot generate payment code because landlord document {landlord_id} was not found.")

                if total_fixed_cost > 0:
                    bill_ref = db.collection("bills").document(bill_id)
                    payment_start_date, payment_due_date = _calculate_payment_dates(now, building_data)

                    bank_info_map = {}
                    if landlord_user_doc.exists:
                        role_data = landlord_user_doc.to_dict().get("role", {})
                        bank_info_map = {
                            "accountNumber": role_data.get("bankAccountNumber"),
                            "bankCode": role_data.get("bankCode"),
                            "accountHolderName": landlord_user_doc.to_dict().get("fullName")
                        }

                    bill_data_to_set = {
                        "status": "UNPAID", # Sử dụng String thay vì Enum
                        "month": now.month,
                        "year": now.year,
                        "issuedDate": firestore.SERVER_TIMESTAMP,
                        "paymentStartDate": payment_start_date,
                        "paymentDueDate": payment_due_date,
                        "contractId": contract_id,
                        "landlordId": landlord_id,
                        "buildingId": building_id,
                        "roomId": room_id,
                        "tenantIds": tenant_ids,
                        "buildingName": building_name,
                        "roomName": room_name,
                        "tenantName": all_tenants_name,
                        "paymentCode": payment_code,
                        "paymentDetails": bank_info_map,
                        "lineItems": firestore.ArrayUnion(line_items_to_add),
                        "totalAmount": firestore.Increment(total_fixed_cost)
                    }

                    bill_ref.set(bill_data_to_set, merge=True)
                    print(f"    [SUCCESS] Upserted bill {bill_id}.")
                else:
                    print(f"    [INFO] No fixed charges to add for contract {contract_id}. Skipping bill update.")

        print("\n----- END: Scheduled job 'generateMonthlyBills' finished successfully. -----")
    except Exception as e:
        print(f"\n[CRITICAL ERROR] An error occurred during scheduled job generateMonthlyBills: {e}")
        import traceback
        traceback.print_exc()
        print("----- END: Scheduled job 'generateMonthlyBills' with errors. -----")


# ============================================================================
# API ENDPOINTS - BILL MANAGEMENT
# ============================================================================

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
                _send_fcm_notification(
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
                _send_fcm_notification(
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

# ============================================================================
# WEBHOOK HANDLERS
# ============================================================================

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
            _send_fcm_notification(bill_data.get("landlordId"), "Payment Alert: Amount Mismatch", f"A payment for {bill_data.get('roomName', 'a room')} was received, but the amount was incorrect.")
            return https.Response('{"success": true}', status=200, mimetype='application/json')

        transaction = db.transaction()
        update_data = {
            "status": "PAID",
            "paymentDate": firestore.SERVER_TIMESTAMP,
            "paidAmount": transfer_amount,
            "paymentMethod": "BANK_TRANSFER_AUTO",
            "sepayTransactionId": sepay_transaction_id
        }

        original_bill_data = _update_bill_in_transaction(transaction, bill_ref, update_data)

        if original_bill_data:
            landlord_id = original_bill_data.get("landlordId")
            tenant_ids = original_bill_data.get("tenantIds", [])
            room_name = original_bill_data.get("roomName", "your room")

            _send_fcm_notification(landlord_id, "Payment Received", f"Payment for {room_name} has been confirmed automatically.")
            for tenant_id in tenant_ids:
                _send_fcm_notification(tenant_id, "Payment Successful", f"Your payment for {room_name} has been confirmed.")

            print(f"[SUCCESS] Bill {bill_ref.id} updated to PAID.")

        return https.Response('{"success": true}', status=200, mimetype='application/json')

    except Exception as e:
        print(f"[CRITICAL] Error processing webhook: {e}")
        # Trả về 500 để SePay có thể retry
        return https.Response("Internal Server Error", status=500)


# ============================================================================
# API ENDPOINTS - PAYMENT CODE MANAGEMENT
# ============================================================================

@https.on_call()
def isPaymentCodePrefixAvailable(request: https.CallableRequest) -> dict:
    """
    [ĐÃ SỬA ĐỔI] Kiểm tra xem một tiền tố (prefix) đã được sử dụng chưa.
    Phương pháp này hiệu quả hơn bằng cách truy vấn trực tiếp vào collection 'paymentCodePrefixes'.
    """
    prefix_to_check = request.data.get("prefix", "").strip().upper()
    current_user_id = request.auth.uid if request.auth else None

    if not prefix_to_check or len(prefix_to_check) < 3 or not current_user_id:
        raise https.HttpsError(
            code=https.HttpsErrorCode.INVALID_ARGUMENT,
            message="Prefix phải có ít nhất 3 ký tự và người dùng phải được xác thực."
        )

    print(f"Checking prefix '{prefix_to_check}' using the optimized method for user '{current_user_id}'.")
    prefix_ref = db.collection("paymentCodePrefixes").document(prefix_to_check)

    try:
        prefix_doc = prefix_ref.get()

        if not prefix_doc.exists:
            is_available = True
        else:
            owner_id = prefix_doc.to_dict().get("ownerId")
            is_available = (owner_id == current_user_id)

        print(f"Result for '{prefix_to_check}': isAvailable = {is_available}")
        return {"isAvailable": is_available}

    except Exception as e:
        print(f"An error occurred while checking prefix: {e}")
        raise https.HttpsError(
            code=https.HttpsErrorCode.INTERNAL,
            message="An error occurred while verifying the prefix."
        )

@https.on_call(max_instances=5)
def set_payment_code_template(request: https.CallableRequest) -> dict:

    if request.auth is None:
        raise https.HttpsError(
            code=https.HttpsErrorCode.UNAUTHENTICATED,
            message="User must be logged in to perform this action."
        )
    user_id = request.auth.uid
    print(f"Request received from authenticated user: {user_id}")

    try:
        prefix = request.data.get("prefix", "").strip().upper()
        suffix_length = int(request.data.get("suffixLength"))
    except (TypeError, ValueError):
         raise https.HttpsError(
            code=https.HttpsErrorCode.INVALID_ARGUMENT,
            message="Invalid data types for prefix or suffixLength."
        )

    if not prefix or len(prefix) < 3 or not (4 <= suffix_length <= 8):
        raise https.HttpsError(
            code=https.HttpsErrorCode.INVALID_ARGUMENT,
            message="Prefix must be at least 3 characters and suffix length must be between 4 and 8."
        )

    user_ref = db.collection("users").document(user_id)
    prefix_ref = db.collection("paymentCodePrefixes").document(prefix)

    transaction = db.transaction()
    @firestore.transactional
    def _update_in_transaction(trans, user_ref, prefix_ref, new_prefix):
        new_prefix_doc = trans.get(prefix_ref)
        if new_prefix_doc.exists and new_prefix_doc.to_dict().get("ownerId") != user_id:
            raise https.HttpsError(
                code=https.HttpsErrorCode.ALREADY_EXISTS,
                message=f"The prefix '{new_prefix}' is already in use by another user."
            )

        user_doc = trans.get(user_ref)
        user_data = user_doc.to_dict()
        old_prefix = user_data.get("role", {}).get("paymentCodeTemplate", {}).get("prefix")

        if old_prefix and old_prefix != new_prefix:
            print(f"User '{user_id}' is changing prefix. Deleting old prefix '{old_prefix}'.")
            old_prefix_ref = db.collection("paymentCodePrefixes").document(old_prefix)
            trans.delete(old_prefix_ref)

        new_template = {"prefix": new_prefix, "suffixLength": suffix_length}
        trans.update(user_ref, {"role.paymentCodeTemplate": new_template})
        trans.set(prefix_ref, {"ownerId": user_id})
    try:
        print(f"Starting transaction for user '{user_id}' with new prefix '{prefix}'...")
        _update_in_transaction(transaction, user_ref, prefix_ref, prefix)
        print(f"Transaction for user '{user_id}' completed successfully.")
        return {"success": True}
    except https.HttpsError as e:
        print(f"Transaction aborted with a known error: {e.message}")
        raise e
    except Exception as e:
        print(f"An unexpected error occurred during the transaction: {e}")
        raise https.HttpsError(
            code=https.HttpsErrorCode.INTERNAL,
            message="An internal error occurred while saving the template."
        )

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
                _send_fcm_notification(
                    user_id=landlord_id,
                    title="Bill Overdue",
                    body=f"The bill for room {room_name} is now overdue. Please follow up."
                )

            # Send a notification to all tenants in the contract
            tenant_ids = bill_data.get("tenantIds", [])
            for tenant_id in tenant_ids:
                _send_fcm_notification(
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


# ============================================================================
# API ENDPOINTS - DASHBOARD & ANALYTICS
# ============================================================================

@https.on_call()
def getLandlordDashboard(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    landlord_id = req.auth.uid
    print(f"----- START: getLandlordDashboard for landlord: {landlord_id} -----")

    try:
        now = datetime.now(timezone.utc)
        current_month = now.month
        current_year = now.year

        # --- 1. LẤY DỮ LIỆU GỐC TỪ FIRESTORE (THEO CÁCH TUẦN TỰ VÀ AN TOÀN) ---
        print("Step 1: Fetching raw data from Firestore...")

        buildings_query = db.collection("buildings").where("userId", "==", landlord_id).stream()
        buildings_filtered = [building for building in buildings_query if building.to_dict().get("status") not in ["ARCHIVED", Status.DELETED]]
        building_ids = [building.id for building in buildings_filtered]

        user_doc = db.collection("users").document(landlord_id).get()
        pending_readings = list(db.collection("meter_readings").where("landlordId", "==", landlord_id).where("status", "==", Status.METER_PENDING).stream())
        active_contracts = list(db.collection("contracts").where("landlordId", "==", landlord_id).where("status", "==", Status.CONTRACT_ACTIVE).stream())
        if not building_ids:
            all_rooms = []
        else:
            all_rooms_query = db.collection("rooms").where("buildingId", "in", building_ids).stream()
            all_rooms = [r for r in all_rooms_query if r.to_dict().get("status") not in ["ARCHIVED", Status.DELETED]]
        join_requests = list(db.collection("qr_sessions").where("inviterId", "==", landlord_id).where("status", "==", Status.SCANNED).stream())
        bills_this_month_query = db.collection("bills").where("landlordId", "==", landlord_id).where("month", "==", current_month).where("year", "==", current_year).stream()
        bills_this_month = [b for b in bills_this_month_query if b.to_dict().get("status") != Status.DELETED]

        print(f"  -> Fetched {len(pending_readings)} pending readings.")
        print(f"  -> Fetched {len(join_requests)} join requests.")
        print(f"  -> Fetched {len(active_contracts)} active contracts.")
        print(f"  -> Fetched {len(all_rooms)} total rooms.")
        print(f"  -> Fetched {len(bills_this_month)} bills for this month ({current_month}/{current_year}).")

        print("\n--- [DEBUG] Listing Fetched Rooms ---")

        if not all_rooms:
            print("  -> No rooms were found for the given building IDs.")
        else:
            print(f"  -> Found a total of {len(all_rooms)} rooms. Details below:")
            # Lặp qua từng phòng và in thông tin chi tiết
            for index, room in enumerate(all_rooms):
                room_data = room.to_dict()
                print(f"  {index + 1}. Room ID: {room.id}")
                print(f"     Building ID: {room_data.get('buildingId', 'N/A')}")
                print(f"     Room Name/Number: {room_data.get('name', 'N/A')}")
                print(f"     Status: {room_data.get('status', 'N/A')}")

        print("--- [DEBUG] End of Room List ---\n")

        # --- 2. XỬ LÝ VÀ TỔNG HỢP DỮ LIỆU ---
        print("\nStep 2: Aggregating data...")

        pending_readings_count = len(pending_readings)
        new_join_requests_count = len(join_requests)
        overdue_bills_count = len([b for b in bills_this_month if b.to_dict().get("status") == "OVERDUE"])
        total_pending_actions = pending_readings_count + new_join_requests_count + overdue_bills_count
        print(f"  -> Action Inbox: {total_pending_actions} total actions.")

        total_revenue_this_month = sum(b.to_dict().get("paidAmount", 0) for b in bills_this_month if b.to_dict().get("status") == "PAID")
        total_unpaid_amount = sum(b.to_dict().get("totalAmount", 0) for b in bills_this_month if b.to_dict().get("status") in ["UNPAID", "OVERDUE"])
        print(f"  -> Financials: Revenue this month = {total_revenue_this_month}, Unpaid = {total_unpaid_amount}.")

        total_room_count = len(all_rooms)
        occupied_room_count = len(active_contracts)
        occupancy_rate = (occupied_room_count / total_room_count) * 100 if total_room_count > 0 else 0
        print(f"  -> Operations: Occupancy = {occupied_room_count}/{total_room_count} rooms ({occupancy_rate:.2f}%).")

        print("  -> Fetching revenue history for the last 6 months...")
        revenue_history = []
        for i in range(6):
            month_to_check = now.month - i
            year_to_check = now.year
            if month_to_check <= 0:
                month_to_check += 12
                year_to_check -= 1

            monthly_bills_query = db.collection("bills").where("landlordId", "==", landlord_id).where("month", "==", month_to_check).where("year", "==", year_to_check).where("status", "==", "PAID").stream()
            monthly_revenue = sum(b.to_dict().get("paidAmount", 0) for b in monthly_bills_query)
            month_short_name = datetime(year_to_check, month_to_check, 1).strftime("%b")
            revenue_history.append({"month": month_short_name, "revenue": monthly_revenue})

        revenue_history.reverse()

        print("\nStep 3: Fetching additional metrics...")

        pending_requests = list(db.collection("requests").where("landlordId", "==", landlord_id).where("status", "==", "PENDING").stream())
        pending_requests_count = len(pending_requests)
        print(f"  -> Pending requests: {pending_requests_count}")

        unique_tenant_ids = set()
        for contract in active_contracts:
            tenant_ids_in_contract = contract.to_dict().get("tenantIds", [])
            unique_tenant_ids.update(tenant_ids_in_contract)
        total_tenants_count = len(unique_tenant_ids)
        print(f"  -> Total unique tenants: {total_tenants_count}")

        total_buildings_count = len(building_ids)
        print(f"  -> Total buildings: {total_buildings_count}")

        active_contracts_count = len(active_contracts)
        print(f"  -> Active contracts: {active_contracts_count}")

        upcoming_deadlines_count = 0
        today = now.day
        for building_id in building_ids:
            building_doc = db.collection("buildings").document(building_id).get()
            if building_doc.exists:
                billing_date = building_doc.to_dict().get("billingDate")
                if billing_date and 0 <= (billing_date - today) <= 3:
                    upcoming_deadlines_count += 1
        print(f"  -> Upcoming billing deadlines (within 3 days): {upcoming_deadlines_count}")

        seven_days_ago = now - timedelta(days=7)
        recent_payments = list(db.collection("bills").where("landlordId", "==", landlord_id).where("status", "==", "PAID").where("paymentDate", ">=", seven_days_ago).stream())
        recent_payments_count = len(recent_payments)
        print(f"  -> Recent payments (last 7 days): {recent_payments_count}")

        print("\nStep 4: Finalizing response data.")

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
            "totalBuildingsCount": total_buildings_count,
            "activeContractsCount": active_contracts_count,
            "upcomingBillingDeadlinesCount": upcoming_deadlines_count,
            "recentPaymentsCount": recent_payments_count,
            "monthlyRevenueHistory": revenue_history,
            "username": user_doc.to_dict().get("fullName", "") if user_doc.exists else ""
        }

        print(f"Successfully prepared dashboard data for landlord {landlord_id}.")
        print("----- END: getLandlordDashboard -----")
        return {"status": "success", "data": dashboard_data}

    except Exception as e:
        print(f"[CRITICAL ERROR] in getLandlordDashboard for landlord {landlord_id}: {e}")
        print("----- END: getLandlordDashboard with errors -----")
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An error occurred while fetching dashboard data.")


# ============================================================================
# API ENDPOINTS - ARCHIVE & DATA MANAGEMENT
# ============================================================================

@https.on_call()
def archiveBuilding(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    landlord_uid = req.auth.uid
    building_id = req.data.get("buildingId")

    if not isinstance(building_id, str) or not building_id:
        raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="A valid 'buildingId' must be provided.")

    print(f"User {landlord_uid} is attempting to archive building {building_id}.")

    try:
        building_ref = db.collection("buildings").document(building_id)
        building_doc = building_ref.get()

        if not building_doc.exists:
            raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="Building not found.")

        if building_doc.to_dict().get("userId") != landlord_uid:
            raise https.HttpsError(code=https.FunctionsErrorCode.PERMISSION_DENIED, message="You do not have permission to archive this building.")

        if building_doc.to_dict().get("status") == "ARCHIVED":
            print(f"Building {building_id} is already archived. No action taken.")
            return {"status": "success", "message": "Building was already archived."}

        active_contracts_query = db.collection("contracts") \
            .where("buildingId", "==", building_id) \
            .where("status", "==", Status.CONTRACT_ACTIVE)

        active_contracts_found = list(active_contracts_query.limit(1).stream())

        if len(active_contracts_found) > 0:
            print(f"[FAIL] Archive denied for building {building_id}: Active contracts still exist.")
            raise https.HttpsError(code=https.FunctionsErrorCode.FAILED_PRECONDITION, message="Cannot archive a building that still has active contracts. Please end all contracts in this building first.")

        print(f"Safety check passed. Proceeding to archive building {building_id} and its rooms.")
        batch = db.batch()

        batch.update(building_ref, {"status": "ARCHIVED"})

        rooms_in_building_query = db.collection("rooms").where("buildingId", "==", building_id)
        rooms_to_archive = list(rooms_in_building_query.stream())

        for room_doc in rooms_to_archive:
            batch.update(room_doc.reference, {"status": "ARCHIVED"})

        batch.commit()
        print(f"[SUCCESS] Archived building {building_id} and {len(rooms_to_archive)} associated room(s).")
        return {"status": "success", "message": "Building and all its rooms have been successfully archived."}

    except https.HttpsError as e:
        raise e
    except Exception as e:
        print(f"[CRITICAL ERROR] in archiveBuilding: {e}")
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An internal server error occurred while archiving the building.")

@https.on_call()
def archiveRoom(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    landlord_uid = req.auth.uid
    room_id = req.data.get("roomId")

    if not isinstance(room_id, str) or not room_id:
        raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="A valid 'roomId' must be provided.")

    print(f"User {landlord_uid} is attempting to archive room {room_id}.")

    try:
        room_ref = db.collection("rooms").document(room_id)
        room_doc = room_ref.get()

        if not room_doc.exists:
            raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="Room not found.")

        room_data = room_doc.to_dict()
        building_id = room_data.get("buildingId")

        if not building_id:
            raise https.HttpsError(code=https.FunctionsErrorCode.FAILED_PRECONDITION, message="Room has no associated building.")

        building_doc = db.collection("buildings").document(building_id).get()
        if not building_doc.exists or building_doc.to_dict().get("userId") != landlord_uid:
            raise https.HttpsError(code=https.FunctionsErrorCode.PERMISSION_DENIED, message="You do not have permission to archive this room.")

        if room_data.get("status") == "ARCHIVED":
            print(f"Room {room_id} is already archived. No action taken.")
            return {"status": "success", "message": "Room was already archived."}

        active_contracts_query = db.collection("contracts") \
            .where("roomId", "==", room_id) \
            .where("status", "==", Status.CONTRACT_ACTIVE)

        active_contracts_found = list(active_contracts_query.limit(1).stream())

        if len(active_contracts_found) > 0:
            print(f"[FAIL] Archive denied for room {room_id}: Active contract still exists.")
            raise https.HttpsError(code=https.FunctionsErrorCode.FAILED_PRECONDITION, message="Cannot archive a room that has an active contract. Please end the contract first.")

        print(f"Safety check passed. Proceeding to archive room {room_id}.")
        room_ref.update({"status": "ARCHIVED"})

        print(f"[SUCCESS] Archived room {room_id}.")
        return {"status": "success", "message": "Room has been successfully archived."}

    except https.HttpsError as e:
        raise e
    except Exception as e:
        print(f"[CRITICAL ERROR] in archiveRoom: {e}")
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An internal server error occurred while archiving the room.")

@https.on_call()
def getBuildingListWithStats(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    landlord_id = req.auth.uid

    search_query = req.data.get("searchQuery", "").strip().lower()
    status_filter = req.data.get("statusFilter")
    print(f"----- START: getBuildingListWithStats for landlord: {landlord_id} with filters: query='{search_query}', status='{status_filter}' -----")

    try:
        now = datetime.now(timezone.utc)
        current_month = now.month
        current_year = now.year

        print("\nStep 1: Fetching initial data from Firestore...")

        all_buildings_query = db.collection("buildings").where("userId", "==", landlord_id).stream()
        all_buildings = [doc for doc in all_buildings_query if doc.to_dict().get("status") not in ["ARCHIVED", Status.DELETED]]
        building_ids = [doc.id for doc in all_buildings]
        print(f"  -> Found {len(all_buildings)} non-archived and non-deleted buildings.")

        if not building_ids:
            return {"buildings": []}

        print("\nStep 2: Fetching related rooms, contracts, and bills...")
        all_rooms = []
        all_contracts = []
        paid_bills_this_month = []

        for i in range(0, len(building_ids), 30):
            chunk_ids = building_ids[i:i + 30]
            rooms_query_raw = db.collection("rooms").where("buildingId", "in", chunk_ids).stream()
            rooms_filtered = [r for r in rooms_query_raw if r.to_dict().get("status") not in ["ARCHIVED", Status.DELETED]]
            contracts_query = db.collection("contracts").where("buildingId", "in", chunk_ids).where("status", "==", Status.CONTRACT_ACTIVE).stream()

            bills_query = db.collection("bills") \
                .where("landlordId", "==", landlord_id) \
                .where("buildingId", "in", chunk_ids) \
                .where("month", "==", current_month) \
                .where("year", "==", current_year) \
                .where("status", "==", "PAID") \
                .stream()

            all_rooms.extend(rooms_filtered)
            all_contracts.extend(list(contracts_query))
            paid_bills_this_month.extend(list(bills_query))

        print(f"  -> Fetched {len(all_rooms)} rooms, {len(all_contracts)} active contracts, and {len(paid_bills_this_month)} paid bills for this month.")

        # --- 3. TỔNG HỢP DỮ LIỆU ---
        print("\nStep 3: Aggregating stats for each building...")

        buildings_with_stats = []
        for building_doc in all_buildings:
            building_id = building_doc.id
            building_data = building_doc.to_dict()

            rooms_in_building = [r for r in all_rooms if r.to_dict().get("buildingId") == building_id]
            contracts_in_building = [c for c in all_contracts if c.to_dict().get("buildingId") == building_id]

            # --- TÍNH TOÁN DOANH THU CHO TỪNG TÒA NHÀ ---
            revenue_for_building = sum(
                b.to_dict().get("paidAmount", 0)
                for b in paid_bills_this_month
                if b.to_dict().get("buildingId") == building_id
            )

            total_rooms = len(rooms_in_building)
            occupied_rooms = len(set(c.to_dict().get("roomId") for c in contracts_in_building))

            buildings_with_stats.append({
                "building": building_data,
                "totalRooms": total_rooms,
                "occupiedRooms": occupied_rooms,
                "revenue": revenue_for_building # << THÊM TRƯỜNG MỚI
            })
            print(f"  -> Stats for '{building_data.get('name')}': {occupied_rooms}/{total_rooms} rooms, Revenue: {revenue_for_building}")

        # --- 4. LỌC TRÊN SERVER ---
        print(f"\nStep 4: Filtering aggregated list (found {len(buildings_with_stats)} items)...")

        # a. Lọc theo trạng thái
        filtered_list_status = buildings_with_stats
        if status_filter and status_filter != "ALL":
            filtered_list_status = [b for b in filtered_list_status if b["building"].get("status") == status_filter]
        print(f"  -> After status filter '{status_filter}': {len(filtered_list_status)} items remain.")

        # b. Lọc theo từ khóa tìm kiếm
        filtered_list_search = filtered_list_status
        if search_query:
            filtered_list_search = [
                b for b in filtered_list_status
                if search_query in b["building"].get("name", "").lower()
                or search_query in b["building"].get("address", "").lower()
            ]
        print(f"  -> After search filter '{search_query}': {len(filtered_list_search)} items remain.")

        # c. Sắp xếp kết quả cuối cùng
        filtered_list_search.sort(key=lambda x: x["building"].get("name", ""))

        print(f"\nStep 5: Returning final list with {len(filtered_list_search)} buildings.")
        print("----- END: getBuildingListWithStats (Success) -----")

        return {"buildings": filtered_list_search}

    except Exception as e:
        print(f"\n[CRITICAL ERROR] in getBuildingListWithStats for landlord {landlord_id}:")
        import traceback
        traceback.print_exc()
        print("----- END: getBuildingListWithStats (Error) -----")
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An error occurred while fetching the building list.")


@https.on_call()
def getContractList(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    landlord_id = req.auth.uid

    status_filter = req.data.get("statusFilter") # "ACTIVE", "PENDING", "ENDED"
    building_id_filter = req.data.get("buildingIdFilter") # ID của tòa nhà hoặc None
    search_query = req.data.get("searchQuery", "").strip().lower()

    print(f"Fetching contracts for {landlord_id} with filters: status='{status_filter}', building='{building_id_filter}', query='{search_query}'")

    try:
        # --- 1. FETCH NON-ARCHIVED AND NON-DELETED BUILDINGS FIRST ---
        buildings_query = db.collection("buildings").where("userId", "==", landlord_id).stream()
        buildings_filtered = [b for b in buildings_query if b.to_dict().get("status") not in ["ARCHIVED", Status.DELETED]]
        non_archived_building_ids = [b.id for b in buildings_filtered]

        if not non_archived_building_ids:
            print("No non-archived buildings found for this landlord.")
            return {"contracts": []}

        # --- 2. XÂY DỰNG QUERY HỢP ĐỒNG CƠ BẢN ---

        contracts_query = db.collection("contracts").where("landlordId", "==", landlord_id)

        if status_filter == "ACTIVE":
            contracts_query = contracts_query.where("status", "in", ["ACTIVE", "OVERDUE"])
        elif status_filter in ["PENDING", "ENDED"]:
            contracts_query = contracts_query.where("status", "==", status_filter)
        # Nếu không có status_filter, mặc định lấy ACTIVE
        else:
            contracts_query = contracts_query.where("status", "in", ["ACTIVE", "OVERDUE"])

        if building_id_filter:
            # Only include if the building is not archived
            if building_id_filter in non_archived_building_ids:
                contracts_query = contracts_query.where("buildingId", "==", building_id_filter)
            else:
                print(f"Building {building_id_filter} is archived. No contracts returned.")
                return {"contracts": []}

        all_contracts = list(contracts_query.stream())

        # Filter out contracts from archived buildings
        all_contracts = [c for c in all_contracts if c.to_dict().get("buildingId") in non_archived_building_ids]

        if not all_contracts:
            return {"contracts": []}

        room_ids = list(set(c.to_dict().get("roomId") for c in all_contracts if c.to_dict().get("roomId")))

        rooms_map = {}
        if room_ids:
            for i in range(0, len(room_ids), 30):
                chunk_ids = room_ids[i:i + 30]
                rooms_snapshot = db.collection("rooms").where("__name__", "in", chunk_ids).stream()
                for room in rooms_snapshot:
                    rooms_map[room.id] = room.to_dict().get("roomNumber", "N/A")

        # --- 4. TỔNG HỢP, LỌC TÌM KIẾM, VÀ TRẢ VỀ ---

        contracts_with_rooms = []
        for contract_doc in all_contracts:
            contract_data = contract_doc.to_dict()
            contract_data['id'] = contract_doc.id
            room_number = rooms_map.get(contract_data.get("roomId"), "N/A")

            matches_search = True
            if search_query:
                contract_num = contract_data.get("contractNumber", "").lower()
                if not (search_query in contract_num or search_query in room_number.lower()):
                    matches_search = False

            if matches_search:
                contracts_with_rooms.append({
                    "contract": contract_data,
                    "roomNumber": room_number
                })

        def parse_date(date_str):
            if not date_str:
                return datetime.min
            try:
                return datetime.strptime(date_str, "%d/%m/%Y")
            except:
                return datetime.min

        contracts_with_rooms.sort(
            key=lambda x: parse_date(x["contract"].get("startDate")),
            reverse=True
        )

        print(f"Returning {len(contracts_with_rooms)} contracts after processing.")
        return {"contracts": contracts_with_rooms}

    except Exception as e:
        print(f"Error in getContractList for landlord {landlord_id}: {e}")
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An error occurred while fetching contracts.")

@https.on_call()
def getRequestList(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(
            code=https.FunctionsErrorCode.UNAUTHENTICATED,
            message="User not authenticated."
        )

    user_id = req.auth.uid
    building_id_filter = req.data.get("buildingIdFilter")
    limit = int(req.data.get("limit", 10))
    start_after = req.data.get("startAfter")

    print(f"Fetching requests for user {user_id}, building filter: {building_id_filter}, limit={limit}, start_after={start_after}")

    try:
        user_doc = db.collection("users").document(user_id).get()
        if not user_doc.exists:
            raise https.HttpsError(
                code=https.FunctionsErrorCode.NOT_FOUND,
                message="User not found."
            )

        user_role = user_doc.to_dict().get("role", {}).get("type")
        requests_query = None

        if user_role == "Landlord":
            print(f"  -> User is a Landlord. Fetching requests.")
            requests_query = (
                db.collection("requests")
                .where("landlordId", "==", user_id)
                .order_by("createdAt", direction=firestore.Query.DESCENDING)
                .limit(limit)
            )

            if building_id_filter:
                room_docs = db.collection("rooms").where("buildingId", "==", building_id_filter).stream()
                room_ids_in_building = [
                    doc.id for doc in room_docs
                    if doc.to_dict().get("status") not in ["ARCHIVED", Status.DELETED]
                ]
                if not room_ids_in_building:
                    return {"requests": [], "nextCursor": None}
                requests_query = requests_query.where("roomId", "in", room_ids_in_building)

        elif user_role == "Tenant":
            print(f"  -> User is a Tenant. Fetching requests.")
            requests_query = (
                db.collection("requests")
                .where("tenantId", "==", user_id)
                .order_by("createdAt", direction=firestore.Query.DESCENDING)
                .limit(limit)
            )

        else:
            print(f"  [WARN] User {user_id} has an unknown or missing role. Returning empty list.")
            return {"requests": [], "nextCursor": None}

        if start_after:
            last_doc = db.collection("requests").document(start_after).get()
            if last_doc.exists:
                requests_query = requests_query.start_after(last_doc)

        all_requests = list(requests_query.stream())
        print(f"  -> Found {len(all_requests)} initial requests.")

        if not all_requests:
            return {"requests": [], "nextCursor": None}

        tenant_ids = list(set(req_doc.to_dict().get("tenantId") for req_doc in all_requests))
        landlord_ids = list(set(req_doc.to_dict().get("landlordId") for req_doc in all_requests))
        room_ids = list(set(req_doc.to_dict().get("roomId") for req_doc in all_requests))

        users_map = {
            doc.id: doc.to_dict()
            for doc in db.collection("users")
            .where("__name__", "in", tenant_ids + landlord_ids)
            .stream()
        }
        rooms_map = {
            doc.id: doc.to_dict()
            for doc in db.collection("rooms")
            .where("__name__", "in", room_ids)
            .stream()
            if doc.to_dict().get("status") not in ["ARCHIVED", Status.DELETED]
        }

        building_ids = list(set(
            room.get("buildingId") for room in rooms_map.values() if room.get("buildingId")
        ))
        buildings_map = {
            doc.id: doc.to_dict()
            for doc in db.collection("buildings")
            .where("__name__", "in", building_ids)
            .stream()
        }

        full_requests_info = []
        for request_doc in all_requests:
            request_data = request_doc.to_dict()
            request_data["id"] = request_doc.id

            tenant = users_map.get(request_data.get("tenantId"))
            landlord = users_map.get(request_data.get("landlordId"))
            room = rooms_map.get(request_data.get("roomId"))
            building = buildings_map.get(room.get("buildingId")) if room else None

            if not all([tenant, landlord, room, building]):
                print(f"    [SKIP] Request {request_doc.id} due to missing related data.")
                continue

            full_requests_info.append({
                "request": request_data,
                "tenantName": tenant.get("fullName", "N/A"),
                "tenantPhoneNumber": tenant.get("phoneNumber", ""),
                "roomName": room.get("roomNumber", "N/A"),
                "buildingName": building.get("name", "N/A"),
                "landlordName": landlord.get("fullName", "N/A"),
                "landlordPhoneNumber": landlord.get("phoneNumber", "")
            })

        full_requests_info.sort(
            key=lambda x: x["request"].get("createdAt").timestamp() if x["request"].get("createdAt") else 0,
            reverse=True
        )

        next_cursor = full_requests_info[-1]["request"]["id"] if full_requests_info else None

        print(f"  Returning {len(full_requests_info)} requests. Next cursor: {next_cursor}")
        return {
            "requests": full_requests_info,
            "nextCursor": next_cursor
        }

    except Exception as e:
        print(f"[CRITICAL ERROR] in getRequestList for user {user_id}: {e}")
        import traceback
        traceback.print_exc()
        raise https.HttpsError(
            code=https.FunctionsErrorCode.INTERNAL,
            message="An error occurred while fetching requests."
        )

@https.on_call()
def getTenantDashboardData(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    tenant_id = req.auth.uid
    print(f"----- START: getTenantDashboardData for tenant: {tenant_id} -----")

    try:
        # --- 1. LẤY DỮ LIỆU CƠ BẢN ---
        print("Step 1: Fetching user and active contract...")
        user_doc = db.collection("users").document(tenant_id).get()
        if not user_doc.exists:
            raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="User not found.")

        # --- 2. TÌM HỢP ĐỒNG ĐANG HOẠT ĐỘNG ---
        active_contract_query = db.collection("contracts") \
            .where("tenantIds", "array_contains", tenant_id) \
            .where("status", "==", Status.CONTRACT_ACTIVE) \
            .limit(1)

        active_contracts = list(active_contract_query.stream())

        if not active_contracts:
            print(f"  -> No active contract found for tenant {tenant_id}. Returning user info only.")
            return {"status": "success", "data": {"user": user_doc.to_dict()}}

        contract_doc = active_contracts[0]
        contract_data = contract_doc.to_dict()
        contract_data['id'] = contract_doc.id
        contract_id = contract_doc.id
        print(f"  -> Found active contract: {contract_id}")

        print("Step 2: Fetching dependent data (room, building, services, readings, bill)...")
        room_id = contract_data.get("roomId")
        building_id = contract_data.get("buildingId")

        room_doc = db.collection("rooms").document(room_id).get() if room_id else None
        building_doc = db.collection("buildings").document(building_id).get() if building_id else None

        services = []
        if building_id:
            services_query = db.collection("buildings").document(building_id).collection("services").where("status", "==", "ACTIVE").stream()
            services = [doc.to_dict() for doc in services_query]
            print(f"  -> Fetched {len(services)} active services from building's subcollection.")

        extra_services = []
        if room_id:
            extra_services_query = db.collection("rooms").document(room_id).collection("extraServices").where("status", "==", "ACTIVE").stream()
            extra_services = [doc.to_dict() for doc in extra_services_query]
            print(f"  -> Fetched {len(extra_services)} active extra services from room's subcollection.")

        last_reading_query = db.collection("meter_readings").where("contractId", "==", contract_id).where("status", "==", Status.METER_APPROVED).order_by("createdAt", direction=firestore.Query.DESCENDING).limit(1).stream()
        last_reading_doc = next(last_reading_query, None)

        current_bill_query = db.collection("bills").where("contractId", "==", contract_id).where("status", "in", ["UNPAID", "OVERDUE"]).order_by("issuedDate", direction=firestore.Query.DESCENDING).limit(1).stream()
        current_bill_doc = next(current_bill_query, None)

        # --- 4. TỔNG HỢP KẾT QUẢ ---
        print("Step 3: Aggregating final response data...")

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

        print(f"  -> Successfully prepared dashboard data for tenant {tenant_id}.")
        print("----- END: getTenantDashboardData (Success) -----")
        return {"status": "success", "data": dashboard_data}

    except Exception as e:
        print(f"[CRITICAL ERROR] in getTenantDashboardData for tenant {tenant_id}: {e}")
        import traceback
        traceback.print_exc()
        print("----- END: getTenantDashboardData (Error) -----")
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An error occurred while fetching dashboard data.")


@https.on_call()
def endContract(req: https.CallableRequest) -> dict:
    """
    Kết thúc một hợp đồng đang hoạt động.
    Hàm này sẽ kiểm tra các hóa đơn chưa thanh toán và dọn dẹp các dữ liệu liên quan
    như phòng, yêu cầu ghi chỉ số, và các yêu cầu sửa chữa.
    """
    if req.auth is None:
        raise https.HttpsError(code=https.FunctionsErrorCode.UNAUTHENTICATED, message="User not authenticated.")

    landlord_uid = req.auth.uid
    contract_id = req.data.get("contractId")
    force_end = req.data.get("forceEnd", False)

    if not isinstance(contract_id, str) or not contract_id:
        raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="A valid 'contractId' must be provided.")

    print(f"User {landlord_uid} is attempting to end contract {contract_id}. Force end: {force_end}")

    try:
        contract_ref = db.collection("contracts").document(contract_id)
        contract_doc = contract_ref.get()

        # --- BƯỚC 1: KIỂM TRA QUYỀN VÀ TRẠNG THÁI HỢP ĐỒNG ---
        if not contract_doc.exists:
            raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="Contract not found.")

        contract_data = contract_doc.to_dict()

        if contract_data.get("landlordId") != landlord_uid:
            raise https.HttpsError(code=https.FunctionsErrorCode.PERMISSION_DENIED, message="You do not have permission to end this contract.")

        if contract_data.get("status") == "ENDED":
            print(f"Contract {contract_id} is already ended. No action taken.")
            return {"status": "success", "message": "Contract was already ended."}

        if contract_data.get("status") != Status.CONTRACT_ACTIVE:
            raise https.HttpsError(code=https.FunctionsErrorCode.FAILED_PRECONDITION, message=f"Cannot end contract with status '{contract_data.get('status')}'. Only ACTIVE contracts can be ended.")

        # --- BƯỚC 2: KIỂM TRA ĐIỀU KIỆN HÓA ĐƠN CHƯA THANH TOÁN ---
        unpaid_bills_query = db.collection("bills").where("contractId", "==", contract_id).where("status", "in", ["UNPAID", "OVERDUE"])
        unpaid_bills = list(unpaid_bills_query.stream())

        if unpaid_bills and not force_end:
            total_unpaid = sum(bill.to_dict().get("totalAmount", 0) for bill in unpaid_bills)
            print(f"[WARN] Blocked ending contract {contract_id}: It has {len(unpaid_bills)} unpaid bill(s) totaling {total_unpaid}.")
            return {
                "status": "warning",
                "message": f"This contract has {len(unpaid_bills)} unpaid bill(s) totaling {total_unpaid:,.0f} ₫. To proceed, please confirm that you want to end the contract anyway.",
                "unpaidBillsCount": len(unpaid_bills),
                "totalUnpaidAmount": total_unpaid
            }

        # --- BƯỚC 3: THU THẬP TẤT CẢ DỮ LIỆU CẦN DỌN DẸP (ĐỌC TRƯỚC) ---
        print(f"Proceeding to end contract {contract_id}. Fetching related documents to clean up...")

        room_id = contract_data.get("roomId")

        pending_readings_to_cancel = list(db.collection("meter_readings").where("contractId", "==", contract_id).where("status", "==", Status.METER_PENDING).stream())
        pending_sessions_to_expire = list(db.collection("qr_sessions").where("contractId", "==", contract_id).where("status", "in", [Status.PENDING, Status.SCANNED]).stream())

        pending_requests_to_cancel = []
        if room_id:
            # Sửa lỗi: Query theo roomId thay vì contractId
            pending_requests_query = db.collection("requests") \
                .where("roomId", "==", room_id) \
                .where("status", "in", ["PENDING", "IN_PROGRESS"]) # Hủy cả các yêu cầu đang xử lý
            pending_requests_to_cancel = list(pending_requests_query.stream())

        print(f"  -> Found {len(pending_readings_to_cancel)} pending readings to cancel.")
        print(f"  -> Found {len(pending_sessions_to_expire)} pending QR sessions to expire.")
        print(f"  -> Found {len(pending_requests_to_cancel)} pending maintenance requests to cancel.")

        # --- BƯỚC 4: THỰC THI TẤT CẢ CÁC THAO TÁC GHI BẰNG BATCH ---
        batch = db.batch()

        # a. Cập nhật Hợp đồng
        batch.update(contract_ref, {
            "status": "ENDED",
            "endedAt": firestore.SERVER_TIMESTAMP,
            "endedBy": landlord_uid
        })

        # b. Cập nhật Phòng
        if room_id:
            room_ref = db.collection("rooms").document(room_id)
            batch.update(room_ref, {"status": "AVAILABLE"})
            print(f"  -> Room {room_id} status will be set to AVAILABLE.")

        # c. Hủy các bản ghi đang chờ
        for reading_doc in pending_readings_to_cancel:
            batch.update(reading_doc.reference, {"status": "CANCELLED", "cancelledReason": "Contract ended"})

        for session_doc in pending_sessions_to_expire:
            batch.update(session_doc.reference, {"status": "EXPIRED"})

        for request_doc in pending_requests_to_cancel:
            batch.update(request_doc.reference, {"status": "CANCELLED", "cancelledReason": "Contract ended", "cancelledAt": firestore.SERVER_TIMESTAMP})

        batch.commit()
        print(f"[SUCCESS] Batch commit successful. Contract {contract_id} has been ended.")

        # --- BƯỚC 5: GỬI THÔNG BÁO ---
        tenant_ids = contract_data.get("tenantIds", [])
        room_name = contract_data.get("roomName", contract_data.get("name", "your room"))

        for tenant_id in tenant_ids:
            _send_fcm_notification(
                user_id=tenant_id,
                title="Contract Ended",
                body=f"Your contract for {room_name} has been ended by the landlord."
            )

        if tenant_ids:
            print(f"  -> Sent 'contract ended' notifications to {len(tenant_ids)} tenant(s).")

        # --- BƯỚC 6: TRẢ VỀ KẾT QUẢ ---
        warnings = []
        if unpaid_bills and force_end:
            warnings.append(f"{len(unpaid_bills)} unpaid bill(s) remain associated with this ended contract.")
        if pending_requests_to_cancel:
            warnings.append(f"{len(pending_requests_to_cancel)} pending request(s) were cancelled.")

        return {
            "status": "success",
            "message": "Contract has been successfully ended.",
            "warnings": warnings if warnings else None
        }

    except https.HttpsError as e:
        raise e
    except Exception as e:
        print(f"[CRITICAL ERROR] in endContract for contract {contract_id}: {e}")
        import traceback
        traceback.print_exc()
        raise https.HttpsError(code=https.FunctionsErrorCode.INTERNAL, message="An internal server error occurred while ending the contract.")


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
                _send_fcm_notification(
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