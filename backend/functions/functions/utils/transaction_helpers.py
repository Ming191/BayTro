"""Firestore transaction helper functions."""

from firebase_admin import firestore


@firestore.transactional
def update_bill_in_transaction(transaction, bill_ref, update_data):
    """Helper to update a bill inside a transaction.
    
    Args:
        transaction: Firestore transaction object
        bill_ref: Bill document reference
        update_data: Dictionary of fields to update
        
    Returns:
        Previous bill snapshot (dict) or None if already PAID
    """
    bill_snapshot = bill_ref.get(transaction=transaction)
    if bill_snapshot.exists and bill_snapshot.to_dict().get("status") == "PAID":
        print(f"[INFO] Transaction aborted: Bill {bill_ref.id} was already marked as PAID.")
        return None

    transaction.update(bill_ref, update_data)
    return bill_snapshot.to_dict()


def process_service_charges(services, num_tenants):
    """Process service charges and calculate costs.
    
    Args:
        services: List of service dictionaries
        num_tenants: Number of tenants in the contract
        
    Returns:
        Tuple of (line_items, total_cost)
    """
    cost = 0
    items = []
    
    for service in services:
        try:
            price = int(service.get("price", "0"))
            if price <= 0 or service.get("status") != "ACTIVE":
                continue

            metric = service.get("metric", "").upper()
            name = service.get("name")

            if metric == "PERSON":
                service_cost = price * num_tenants
                description = f"{name} ({num_tenants} people)"
                items.append({
                    "description": description,
                    "quantity": num_tenants,
                    "pricePerUnit": price,
                    "totalCost": service_cost
                })
                cost += service_cost
            elif metric not in ["KWH", "M3"]:
                items.append({
                    "description": name,
                    "totalCost": price
                })
                cost += price
        except (ValueError, TypeError):
            continue
    
    return items, cost
