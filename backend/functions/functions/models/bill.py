"""Bill data model and generation logic."""

from dataclasses import dataclass
from firebase_admin import firestore


@dataclass
class PaymentCodeConfig:
    """Payment code configuration."""
    DEFAULT_PREFIX = "BAYTRO"
    DEFAULT_SUFFIX_LENGTH = 6


@dataclass
class BillLineItem:
    """Represents a single line item in a bill."""
    description: str
    totalCost: int


class BillData:
    """Bill data builder for Firestore."""
    
    @staticmethod
    def create(now, contract_id: str, contract_data: dict, building_data: dict,
               room_data: dict, tenant_names: str, payment_code: str,
               bank_info: dict, line_items: list, total_cost: int,
               payment_start_date, payment_due_date) -> dict:
        """Create bill data dictionary for Firestore."""
        return {
            "status": "UNPAID",
            "month": now.month,
            "year": now.year,
            "issuedDate": firestore.SERVER_TIMESTAMP,
            "paymentStartDate": payment_start_date,
            "paymentDueDate": payment_due_date,
            "contractId": contract_id,
            "landlordId": contract_data.get("landlordId"),
            "buildingId": contract_data.get("buildingId"),
            "roomId": contract_data.get("roomId"),
            "tenantIds": contract_data.get("tenantIds", []),
            "buildingName": building_data.get("name", "N/A"),
            "roomName": room_data.get("roomNumber", "N/A"),
            "tenantName": tenant_names,
            "paymentCode": payment_code,
            "paymentDetails": bank_info,
            "lineItems": firestore.ArrayUnion(line_items),
            "totalAmount": firestore.Increment(total_cost)
        }
    
    @staticmethod
    def generate_payment_code(landlord_data: dict, contract_id: str) -> str:
        """Generate payment code from landlord template or use default."""
        template = landlord_data.get("paymentCodeTemplate", {})
        prefix = template.get("prefix", PaymentCodeConfig.DEFAULT_PREFIX).upper()
        suffix_length = min(
            int(template.get("suffixLength", PaymentCodeConfig.DEFAULT_SUFFIX_LENGTH)),
            len(contract_id)
        )
        return f"{prefix}{contract_id[-suffix_length:].upper()}"
    
    @staticmethod
    def extract_bank_info(landlord_data: dict) -> dict:
        """Extract bank information from landlord document."""
        role_data = landlord_data.get("role", {})
        return {
            "accountNumber": role_data.get("bankAccountNumber"),
            "bankCode": role_data.get("bankCode"),
            "accountHolderName": landlord_data.get("fullName")
        }
