"""Configuration and constants for the application."""

import firebase_admin
from firebase_admin import firestore, credentials

# Initialize Firebase
cred = credentials.Certificate("service-account.json")
firebase_admin.initialize_app(cred)
db = firestore.client()

# Constants
QR_CODE_VALIDITY_MINUTES = 5
