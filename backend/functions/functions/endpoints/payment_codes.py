"""Payment code management endpoints."""

import logging
from firebase_functions import https_fn as https
from firebase_admin import firestore

from config import db

logger = logging.getLogger(__name__)


def _validate_prefix(prefix: str, user_id: str) -> str:
    cleaned = prefix.strip().upper()
    if not cleaned or len(cleaned) < 3:
        raise https.HttpsError(code=https.HttpsErrorCode.INVALID_ARGUMENT, message="Prefix must be at least 3 characters.")
    if not user_id:
        raise https.HttpsError(code=https.HttpsErrorCode.UNAUTHENTICATED, message="User must be authenticated.")
    return cleaned


@https.on_call()
def isPaymentCodePrefixAvailable(request: https.CallableRequest) -> dict:
    user_id = request.auth.uid if request.auth else None
    prefix = _validate_prefix(request.data.get("prefix", ""), user_id)

    try:
        logger.info(f"Checking prefix availability: user={user_id}, prefix={prefix}")
        doc = db.collection("paymentCodePrefixes").document(prefix).get()
        is_available = not doc.exists or doc.to_dict().get("ownerId") == user_id
        logger.info(f"Prefix availability checked: prefix={prefix}, available={is_available}")
        return {"isAvailable": is_available}
    except https.HttpsError:
        raise
    except Exception as e:
        logger.error(f"isPaymentCodePrefixAvailable failed: user={user_id}, prefix={prefix}, error={e}", exc_info=True)
        raise https.HttpsError(code=https.HttpsErrorCode.INTERNAL, message="An error occurred while verifying the prefix.")


def _validate_suffix_length(suffix_length: int) -> int:
    try:
        length = int(suffix_length)
        if not (4 <= length <= 8):
            raise ValueError
        return length
    except (TypeError, ValueError):
        raise https.HttpsError(code=https.HttpsErrorCode.INVALID_ARGUMENT, message="Suffix length must be between 4 and 8.")


@firestore.transactional
def _update_template_transaction(trans, user_ref, prefix_ref, user_id: str, prefix: str, suffix_length: int):
    prefix_doc = trans.get(prefix_ref)
    if prefix_doc.exists and prefix_doc.to_dict().get("ownerId") != user_id:
        raise https.HttpsError(code=https.HttpsErrorCode.ALREADY_EXISTS, message=f"Prefix '{prefix}' is already in use.")

    user_doc = trans.get(user_ref)
    old_prefix = user_doc.to_dict().get("role", {}).get("paymentCodeTemplate", {}).get("prefix")

    if old_prefix and old_prefix != prefix:
        trans.delete(db.collection("paymentCodePrefixes").document(old_prefix))

    trans.update(user_ref, {"role.paymentCodeTemplate": {"prefix": prefix, "suffixLength": suffix_length}})
    trans.set(prefix_ref, {"ownerId": user_id})


@https.on_call(max_instances=5)
def set_payment_code_template(request: https.CallableRequest) -> dict:
    if request.auth is None:
        raise https.HttpsError(code=https.HttpsErrorCode.UNAUTHENTICATED, message="User must be logged in.")
    
    user_id = request.auth.uid
    prefix = _validate_prefix(request.data.get("prefix", ""), user_id)
    suffix_length = _validate_suffix_length(request.data.get("suffixLength"))

    logger.info(f"Setting payment code template: user={user_id}, prefix={prefix}, suffix_length={suffix_length}")

    user_ref = db.collection("users").document(user_id)
    prefix_ref = db.collection("paymentCodePrefixes").document(prefix)

    try:
        transaction = db.transaction()
        _update_template_transaction(transaction, user_ref, prefix_ref, user_id, prefix, suffix_length)
        logger.info(f"Payment code template set: user={user_id}, prefix={prefix}")
        return {"success": True}
    except https.HttpsError:
        raise
    except Exception as e:
        logger.error(f"set_payment_code_template failed: user={user_id}, prefix={prefix}, error={e}", exc_info=True)
        raise https.HttpsError(code=https.HttpsErrorCode.INTERNAL, message="An internal error occurred while saving the template.")
