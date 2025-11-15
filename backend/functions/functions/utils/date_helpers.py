"""Date and time calculation utilities."""

from datetime import datetime, timezone


def calculate_payment_dates(now, building_data):
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
