package com.example.baytro.data.billing

import kotlinx.serialization.Serializable

@Serializable
enum class BillStatus {
    UNPAID,
    PAID,
    OVERDUE,
    NOT_ISSUED_YET
}

