package com.example.baytro.utils

data class FieldError(val isError: Boolean, val message: String?)

object BuildingValidator {
    fun validateName(name: String): FieldError {
        val invalid = name.isBlank()
        return FieldError(invalid, if (invalid) "Building name is required" else null)
    }

    fun validateAddress(address: String): FieldError {
        val invalid = address.isBlank()
        return FieldError(invalid, if (invalid) "Address is required" else null)
    }

    fun validateFloor(floor: String): FieldError {
        if (floor.isBlank()) return FieldError(true, "Floor is required")
        val value = floor.toIntOrNull()
        if (value == null || value <= 0) return FieldError(true, "Floor must be a positive integer")
        return FieldError(false, null)
    }

    fun validateBillingDate(billingDate: String): FieldError {
        if (billingDate.isBlank()) return FieldError(true, "Billing date is required")
        val value = billingDate.toIntOrNull()
        if (value == null || value <= 0) return FieldError(true, "Billing date must be a positive integer")
        return FieldError(false, null)
    }

    fun validatePaymentStart(paymentStart: String): FieldError {
        if (paymentStart.isBlank()) return FieldError(true, "Payment start is required")
        val value = paymentStart.toIntOrNull()
        if (value == null || value <= 0) return FieldError(true, "Payment start must be a positive integer")
        return FieldError(false, null)
    }

    fun validatePaymentDue(paymentDue: String): FieldError {
        if (paymentDue.isBlank()) return FieldError(true, "Payment due is required")
        val value = paymentDue.toIntOrNull()
        if (value == null || value <= 0) return FieldError(true, "Payment due must be a positive integer")
        return FieldError(false, null)
    }
}


