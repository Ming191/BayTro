"""Meter reading data models and utilities."""

from datetime import datetime, timedelta, timezone
from typing import Optional


class MeterReadingCalculator:
    """Meter reading consumption and billing calculator."""
    
    @staticmethod
    def get_previous_readings(db, contract_id: str, contract_data: dict, transaction=None) -> tuple[int, int]:
        """Get previous meter reading values or initial values from contract.
        
        Returns:
            Tuple of (electricity_value, water_value)
        """
        from firebase_admin import firestore
        from models import Status
        
        query = db.collection("meter_readings").where("contractId", "==", contract_id).where("status", "==", Status.METER_APPROVED).order_by("createdAt", direction=firestore.Query.DESCENDING).limit(1)
        previous_readings = list(query.stream(transaction=transaction))
        
        if not previous_readings:
            return (
                contract_data.get("initialElectricityReading", 0),
                contract_data.get("initialWaterReading", 0)
            )
        
        prev_data = previous_readings[0].to_dict()
        return (
            prev_data.get("electricityValue", 0),
            prev_data.get("waterValue", 0)
        )
    
    @staticmethod
    def get_utility_prices(services: list[dict]) -> tuple[int, int]:
        """Extract electricity and water prices from services.
        
        Returns:
            Tuple of (electricity_price, water_price)
        """
        price_elec = next((int(s.get("price", "0")) for s in services if s.get("name", "").lower() == "electricity"), 0)
        price_water = next((int(s.get("price", "0")) for s in services if s.get("name", "").lower() == "water"), 0)
        return price_elec, price_water
    
    @staticmethod
    def calculate_consumption(new_elec: int, new_water: int, old_elec: int, old_water: int) -> tuple[int, int]:
        """Calculate consumption from readings.
        
        Returns:
            Tuple of (electricity_consumption, water_consumption)
        """
        elec_consumption = new_elec - old_elec
        water_consumption = new_water - old_water
        
        if elec_consumption < 0 or water_consumption < 0:
            from firebase_functions import https_fn as https
            raise https.HttpsError(code=https.FunctionsErrorCode.INVALID_ARGUMENT, message="New reading cannot be less than previous.")
        
        return elec_consumption, water_consumption
    
    @staticmethod
    def calculate_bill_month(now: datetime, billing_date: int) -> tuple[int, int]:
        """Calculate which month/year the bill should be assigned to.
        
        Returns:
            Tuple of (month, year)
        """
        if now.day > billing_date:
            next_month_date = now.replace(day=28) + timedelta(days=4)
            return next_month_date.month, next_month_date.year
        return now.month, now.year
    
    @staticmethod
    def create_bill_items(reading_id: str, old_elec: int, new_elec: int, old_water: int, new_water: int,
                         elec_consumption: int, water_consumption: int, 
                         price_elec: int, price_water: int) -> tuple[dict, dict, int]:
        """Create electricity and water bill line items.
        
        Returns:
            Tuple of (electricity_item, water_item, total_cost)
        """
        elec_cost = elec_consumption * price_elec
        water_cost = water_consumption * price_water
        total_cost = elec_cost + water_cost
        
        bill_item_elec = {
            "description": f"Electricity ({old_elec} to {new_elec})",
            "quantity": elec_consumption,
            "pricePerUnit": price_elec,
            "totalCost": elec_cost,
            "readingId": reading_id
        }
        
        bill_item_water = {
            "description": f"Water ({old_water} to {new_water})",
            "quantity": water_consumption,
            "pricePerUnit": price_water,
            "totalCost": water_cost,
            "readingId": reading_id
        }
        
        return bill_item_elec, bill_item_water, total_cost


class MeterReadingValidator:
    """Meter reading validation utilities."""
    
    @staticmethod
    def validate_reading_data(reading_data: dict):
        """Validate that reading has required fields."""
        landlord_id = reading_data.get("landlordId")
        tenant_id = reading_data.get("tenantId")
        room_id = reading_data.get("roomId")
        
        if not all([landlord_id, tenant_id, room_id]):
            raise Exception("Reading data is incomplete.")
        
        return landlord_id, tenant_id, room_id
    
    @staticmethod
    def validate_bill_status(bill_doc, bill_month_str: str):
        """Validate that bill can accept new charges."""
        from firebase_functions import https_fn as https
        
        if bill_doc.exists:
            bill_status = bill_doc.to_dict().get("status")
            if bill_status not in ["NOT_ISSUED_YET", None]:
                raise https.HttpsError(
                    code=https.FunctionsErrorCode.FAILED_PRECONDITION,
                    message=f"Cannot add charges. The bill for {bill_month_str} has already been issued with status '{bill_status}'."
                )
