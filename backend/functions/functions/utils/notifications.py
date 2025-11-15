"""Firebase Cloud Messaging notification utilities."""

from firebase_admin import messaging
from config import db


def send_fcm_notification(user_id: str, title: str, body: str, data: dict = None):
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
