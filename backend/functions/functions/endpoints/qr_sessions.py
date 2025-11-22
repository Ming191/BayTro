"""QR code session and tenant linking endpoints."""

import logging
from datetime import datetime, timedelta, timezone
from firebase_functions import https_fn as https
from firebase_admin import firestore

from config import db, QR_CODE_VALIDITY_MINUTES
from models import Status
from utils import send_fcm_notification

logger = logging.getLogger(__name__)


def _validate_session_id(session_id) -> str:
    if not isinstance(session_id, str) or not session_id:
        raise https.HttpsError(
            code=https.FunctionsErrorCode.INVALID_ARGUMENT,
            message="Invalid 'sessionId'."
        )
    return session_id


def _validate_contract_ownership(contract_id: str, user_id: str):
    contract_doc = db.collection("contracts").document(contract_id).get()
    if not contract_doc.exists or contract_doc.to_dict().get("landlordId") != user_id:
        raise https.HttpsError(
            code=https.FunctionsErrorCode.PERMISSION_DENIED,
            message="User does not have permission for this contract."
        )


def _validate_session_status(session_data: dict, expected_status: str, error_message: str):
    if session_data.get("status") != expected_status:
        raise https.HttpsError(
            code=https.FunctionsErrorCode.FAILED_PRECONDITION, 
            message=error_message
            )


def _check_session_expiry(session_ref, session_data: dict):
    expires_at = session_data.get("expiresAt")
    if expires_at is None or datetime.now(timezone.utc) > expires_at:
        session_ref.update({"status": Status.EXPIRED})
        raise https.HttpsError(
            code=https.FunctionsErrorCode.DEADLINE_EXCEEDED,
            message="QR code has expired."
        )


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
        logger.info(f"Generating QR session for user={uid}, contract={contract_id}")
        _validate_contract_ownership(contract_id, uid)
        
        expiration_time = datetime.now(timezone.utc) + timedelta(minutes=QR_CODE_VALIDITY_MINUTES)
        session_data = {
            "contractId": contract_id,
            "inviterId": uid,
            "status": Status.PENDING,
            "createdAt": firestore.SERVER_TIMESTAMP,
            "expiresAt": expiration_time
        }
        _, session_ref = db.collection("qr_sessions").add(session_data)
        
        logger.info(f"QR session created: session_id={session_ref.id}, user={uid}")
        return {"status": "success", "data": {"sessionId": session_ref.id}}
    
    except https.HttpsError:
        raise
    except Exception as e:
        logger.error(f"generateQrSession failed for user={uid}, contract={contract_id}: {e}")
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
    session_id = _validate_session_id(req.data.get("sessionId"))
    
    try:
        logger.info(f"Processing QR scan: tenant={tenant_uid}, session={session_id}")
        session_ref = db.collection("qr_sessions").document(session_id)
        session_doc = session_ref.get()
        
        if not session_doc.exists:
            raise https.HttpsError(
                code=https.FunctionsErrorCode.NOT_FOUND,
                message="QR code is invalid or expired."
            )
        
        session_data = session_doc.to_dict()
        _validate_session_status(session_data, Status.PENDING, "QR code has been used.")
        _check_session_expiry(session_ref, session_data)
        
        session_ref.update({
            "scannedByTenantId": tenant_uid,
            "status": Status.SCANNED,
            "scannedAt": firestore.SERVER_TIMESTAMP
        })
        
        send_fcm_notification(
            user_id=session_data.get("inviterId"),
            title="New Join Request",
            body="A tenant has scanned your invitation.",
            data={
                "screen": "ContractDetails",
                "contractId": session_data.get("contractId")
            }
        )
        
        logger.info(f"QR scan processed: tenant={tenant_uid}, session={session_id}")
        return {
            "status": "success", 
            "message": "Scan successful. Please wait for confirmation."}
    
    except https.HttpsError:
        raise
    except Exception as e:
        logger.error(f"processQrScan failed for tenant={tenant_uid}, session={session_id}: {e}")
        raise https.HttpsError(
            code=https.FunctionsErrorCode.INTERNAL, 
            message="An internal server error occurred."
            )


@firestore.transactional
def _confirm_tenant_transaction(transaction, session_id: str, landlord_uid: str):
    session_ref = db.collection("qr_sessions").document(session_id)
    session_doc = session_ref.get(transaction=transaction)
    
    if not session_doc.exists:
        raise https.HttpsError(code=https.FunctionsErrorCode.NOT_FOUND, message="Session does not exist.")
    
    session_data = session_doc.to_dict()
    
    if session_data.get("inviterId") != landlord_uid:
        raise https.HttpsError(
            code=https.FunctionsErrorCode.PERMISSION_DENIED, 
            message="You cannot confirm this invitation."
            )
    
    if session_data.get("status") != Status.SCANNED:
        raise https.HttpsError(
            code=https.FunctionsErrorCode.FAILED_PRECONDITION, 
            message="Invitation not ready for confirmation."
            )
    
    tenant_id = session_data.get("scannedByTenantId")
    contract_id = session_data.get("contractId")
    
    if not tenant_id or not contract_id:
        raise https.HttpsError(
            code=https.FunctionsErrorCode.INTERNAL,
            message="Session data is corrupted."
        )
    
    active_contracts_query = (
        db.collection("contracts")
        .where("status", "==", Status.CONTRACT_ACTIVE)
        .where("tenantIds", "array_contains", tenant_id)
    )
    active_contracts = list(active_contracts_query.stream(transaction=transaction))
    if len(active_contracts) > 0:
        raise https.HttpsError(
            code=https.FunctionsErrorCode.FAILED_PRECONDITION,
            message="This tenant is already in another active contract."
        )
    
    transaction.update(
        db.collection("contracts").document(contract_id),
        {
            "tenantIds": firestore.ArrayUnion([tenant_id]),
            "status": Status.CONTRACT_ACTIVE
        }
    )
    transaction.update(
        session_ref,
        {
            "status": Status.CONFIRMED,
            "confirmedAt": firestore.SERVER_TIMESTAMP
        }
    )
    
    return tenant_id, contract_id


@https.on_call()
def confirmTenantLink(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(
            code=https.FunctionsErrorCode.UNAUTHENTICATED,
            message="User not authenticated."
        )

    landlord_uid = req.auth.uid
    session_id = _validate_session_id(req.data.get("sessionId"))
    
    try:
        logger.info(f"Confirming tenant link: landlord={landlord_uid}, session={session_id}")
        tenant_id, contract_id = _confirm_tenant_transaction(
            db.transaction(), session_id, landlord_uid
        )
        
        send_fcm_notification(
            user_id=tenant_id,
            title="Invitation Confirmed!",
            body="You have been successfully added to the contract.",
            data={
                "screen": "ContractDetails",
                "contractId": contract_id
            }
        )
        
        logger.info(
            f"Tenant linked: tenant={tenant_id}, contract={contract_id}, landlord={landlord_uid}"
        )
        return {"status": "success", "message": "Tenant linked successfully."}
    
    except https.HttpsError:
        raise
    except Exception as e:
        logger.error(
            f"confirmTenantLink failed for landlord={landlord_uid}, session={session_id}: {e}"
        )
        raise https.HttpsError(
            code=https.FunctionsErrorCode.INTERNAL,
            message="A critical error occurred."
        )


@https.on_call()
def declineTenantLink(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(
            code=https.FunctionsErrorCode.UNAUTHENTICATED,
            message="User not authenticated."
        )

    landlord_uid = req.auth.uid
    session_id = _validate_session_id(req.data.get("sessionId"))
    
    try:
        logger.info(f"Declining tenant link: landlord={landlord_uid}, session={session_id}")
        session_ref = db.collection("qr_sessions").document(session_id)
        session_doc = session_ref.get()
        
        if not session_doc.exists:
            raise https.HttpsError(
                code=https.FunctionsErrorCode.NOT_FOUND,
                message="Session does not exist."
            )
        
        session_data = session_doc.to_dict()
        
        if session_data.get("inviterId") != landlord_uid:
            raise https.HttpsError(
                code=https.FunctionsErrorCode.PERMISSION_DENIED,
                message="You cannot decline this invitation."
            )
        
        if session_data.get("status") != Status.SCANNED:
            raise https.HttpsError(
                code=https.FunctionsErrorCode.FAILED_PRECONDITION,
                message="This invitation cannot be declined."
            )
        
        session_ref.update({
            "status": Status.DECLINED,
            "declinedAt": firestore.SERVER_TIMESTAMP
        })
        
        logger.info(f"Tenant link declined: landlord={landlord_uid}, session={session_id}")
        return {"status": "success", "message": "Request has been declined."}
    
    except https.HttpsError:
        raise
    except Exception as e:
        logger.error(
            f"declineTenantLink failed for landlord={landlord_uid}, session={session_id}: {e}"
        )
        raise https.HttpsError(
            code=https.FunctionsErrorCode.INTERNAL,
            message="An internal error occurred."
        )


@https.on_call()
def updateFcmToken(req: https.CallableRequest) -> dict:
    if req.auth is None:
        raise https.HttpsError(
            code=https.FunctionsErrorCode.UNAUTHENTICATED,
            message="User not authenticated."
        )

    uid = req.auth.uid
    new_token = req.data.get("fcmToken")

    if not isinstance(new_token, str) or not new_token:
        logger.warning(f"Invalid FCM token received for user={uid}")
        raise https.HttpsError(
            code=https.FunctionsErrorCode.INVALID_ARGUMENT,
            message="Invalid 'fcmToken'."
        )

    try:
        logger.info(f"Updating FCM token for user={uid}, token_length={len(new_token)}")
        db.collection("users").document(uid).update({"fcmToken": new_token})
        logger.info(f"FCM token updated successfully for user={uid}")
        return {"status": "success"}
    except Exception as e:
        logger.error(f"updateFcmToken failed for user={uid}: {e}")
        raise https.HttpsError(
            code=https.FunctionsErrorCode.INTERNAL,
            message="Failed to update token."
        )
