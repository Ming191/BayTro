"""Endpoints package."""

from .qr_sessions import generateQrSession, processQrScan, confirmTenantLink, declineTenantLink, updateFcmToken
from .meter_readings import notifyNewMeterReading, approveMeterReading, declineMeterReading
from .bills import markBillAsPaid, addManualChargeToBill, sendBillPaymentReminder
from .payment_codes import isPaymentCodePrefixAvailable, set_payment_code_template
from .dashboard import getLandlordDashboard, getTenantDashboardData
from .buildings import archiveBuilding, archiveRoom, getBuildingListWithStats
from .contracts import getContractList, endContract
from .requests import getRequestList

__all__ = [
    'generateQrSession',
    'processQrScan',
    'confirmTenantLink',
    'declineTenantLink',
    'updateFcmToken',
    'notifyNewMeterReading',
    'approveMeterReading',
    'declineMeterReading',
    'markBillAsPaid',
    'addManualChargeToBill',
    'sendBillPaymentReminder',
    'isPaymentCodePrefixAvailable',
    'set_payment_code_template',
    'getLandlordDashboard',
    'getTenantDashboardData',
    'archiveBuilding',
    'archiveRoom',
    'getBuildingListWithStats',
    'getContractList',
    'endContract',
    'getRequestList',
]
