"""Helper functions for user and room data retrieval."""

from config import db


def get_user_name(user_id: str, default: str = "A user") -> str:
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


def get_room_number(room_id: str, default: str = "a room") -> str:
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
