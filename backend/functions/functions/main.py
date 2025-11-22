"""
Baytro Cloud Functions - Main Entry Point

This is the orchestrator file that imports and exports all Firebase Cloud Functions
from their respective modules. Firebase will discover and deploy all exported functions.

Modular structure:
- config.py: Firebase initialization and constants
- models/: Data models and status constants
- utils/: Helper functions (notifications, users, dates, transactions)
- endpoints/: HTTP callable functions organized by domain
- webhooks/: External webhook handlers
- scheduled/: Cron jobs and scheduled functions
"""

# Import all endpoint functions
from endpoints import (
    # QR Sessions & Tenant Linking
    generateQrSession,
    processQrScan,
    confirmTenantLink,
    declineTenantLink,
    updateFcmToken,
    
    # Meter Readings
    notifyNewMeterReading,
    approveMeterReading,
    declineMeterReading,
    
    # Bills
    markBillAsPaid,
    addManualChargeToBill,
    sendBillPaymentReminder,
    
    # Payment Codes
    isPaymentCodePrefixAvailable,
    set_payment_code_template,
    
    # Dashboard
    getLandlordDashboard,
    getTenantDashboardData,
    
    # Buildings & Rooms
    archiveBuilding,
    archiveRoom,
    getBuildingListWithStats,
    
    # Contracts
    getContractList,
    endContract,
    
    # Requests
    getRequestList,
)

# Import all scheduled functions
from scheduled import (
    cleanupExpiredQrSessions,
    handleMeterReadingDeadlines,
    generateMonthlyBills,
    checkAndMarkOverdueBills,
)

# Import all webhook handlers
from webhooks import (
    handleSePayWebhook,
)

# Export all functions for Firebase to discover
__all__ = [
    # Endpoints - QR Sessions
    'generateQrSession',
    'processQrScan',
    'confirmTenantLink',
    'declineTenantLink',
    'updateFcmToken',
    
    # Endpoints - Meter Readings
    'notifyNewMeterReading',
    'approveMeterReading',
    'declineMeterReading',
    
    # Endpoints - Bills
    'markBillAsPaid',
    'addManualChargeToBill',
    'sendBillPaymentReminder',
    
    # Endpoints - Payment Codes
    'isPaymentCodePrefixAvailable',
    'set_payment_code_template',
    
    # Endpoints - Dashboard
    'getLandlordDashboard',
    'getTenantDashboardData',
    
    # Endpoints - Buildings & Rooms
    'archiveBuilding',
    'archiveRoom',
    'getBuildingListWithStats',
    
    # Endpoints - Contracts
    'getContractList',
    'endContract',
    
    # Endpoints - Requests
    'getRequestList',
    
    # Scheduled Jobs
    'cleanupExpiredQrSessions',
    'handleMeterReadingDeadlines',
    'generateMonthlyBills',
    'checkAndMarkOverdueBills',
    
    # Webhooks
    'handleSePayWebhook',
]
