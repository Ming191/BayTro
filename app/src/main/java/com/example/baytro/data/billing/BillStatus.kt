package com.example.baytro.data.billing

import kotlinx.serialization.Serializable

// Represents the status of a bill.
@Serializable
enum class BillStatus {
    UNPAID,       // Bill is issued but not yet paid.
    PAID,         // Bill has been paid.
    OVERDUE,      // Bill is unpaid and past its due date.
    NOT_ISSUED_YET // Costs are accumulating (e.g., from meter readings) but the bill hasn't been finalized with fixed charges yet.
}

