package com.example.baytro.utils

/**
 * Represents the result of a field validation.
 * Using a data class with clear semantics instead of a generic error holder.
 */
data class FieldError(
    val isError: Boolean,
    val message: String?
) {
    companion object {
        fun success(): FieldError = FieldError(isError = false, message = null)
        fun error(message: String): FieldError = FieldError(isError = true, message = message)
    }
}


object BuildingValidator {

    private const val ERROR_NAME_REQUIRED = "Building name is required"
    private const val ERROR_ADDRESS_REQUIRED = "Address is required"
    private const val ERROR_FLOOR_REQUIRED = "Floor is required"
    private const val ERROR_FLOOR_INVALID = "Floor must be a positive integer"
    private const val ERROR_BILLING_DATE_REQUIRED = "Billing date is required"
    private const val ERROR_BILLING_DATE_INVALID = "Billing date must be between 1 and 31"
    private const val ERROR_PAYMENT_START_REQUIRED = "Payment start is required"
    private const val ERROR_PAYMENT_START_INVALID = "Payment start must be between 1 and 31"
    private const val ERROR_PAYMENT_DUE_REQUIRED = "Payment due is required"
    private const val ERROR_PAYMENT_DUE_INVALID = "Payment due must be between 1 and 31"

    // Validation constraints
    private const val MIN_DAY_OF_MONTH = 1
    private const val MAX_DAY_OF_MONTH = 31
    private const val MIN_FLOOR = 1

    /**
     * Validates the building name.
     * @param name The building name to validate
     * @return FieldError indicating validation result
     */
    fun validateName(name: String): FieldError {
        return when {
            name.isBlank() -> FieldError.error(ERROR_NAME_REQUIRED)
            else -> FieldError.success()
        }
    }

    /**
     * Validates the building address.
     * @param address The address to validate
     * @return FieldError indicating validation result
     */
    fun validateAddress(address: String): FieldError {
        return when {
            address.isBlank() -> FieldError.error(ERROR_ADDRESS_REQUIRED)
            else -> FieldError.success()
        }
    }

    /**
     * Validates the floor number.
     * @param floor The floor number as a string to validate
     * @return FieldError indicating validation result
     */
    fun validateFloor(floor: String): FieldError {
        return when {
            floor.isBlank() -> FieldError.error(ERROR_FLOOR_REQUIRED)
            else -> {
                val value = floor.toIntOrNull()
                when {
                    value == null || value < MIN_FLOOR -> FieldError.error(ERROR_FLOOR_INVALID)
                    else -> FieldError.success()
                }
            }
        }
    }

    /**
     * Validates the billing date (day of month).
     * @param billingDate The billing date to validate
     * @return FieldError indicating validation result
     */
    fun validateBillingDate(billingDate: String): FieldError {
        return validateDayOfMonth(
            value = billingDate,
            requiredError = ERROR_BILLING_DATE_REQUIRED,
            invalidError = ERROR_BILLING_DATE_INVALID
        )
    }

    /**
     * Validates the payment start date (day of month).
     * @param paymentStart The payment start date to validate
     * @return FieldError indicating validation result
     */
    fun validatePaymentStart(paymentStart: String): FieldError {
        return validateDayOfMonth(
            value = paymentStart,
            requiredError = ERROR_PAYMENT_START_REQUIRED,
            invalidError = ERROR_PAYMENT_START_INVALID
        )
    }

    /**
     * Validates the payment due date (day of month).
     * @param paymentDue The payment due date to validate
     * @return FieldError indicating validation result
     */
    fun validatePaymentDue(paymentDue: String): FieldError {
        return validateDayOfMonth(
            value = paymentDue,
            requiredError = ERROR_PAYMENT_DUE_REQUIRED,
            invalidError = ERROR_PAYMENT_DUE_INVALID
        )
    }

    private fun validateDayOfMonth(
        value: String,
        requiredError: String,
        invalidError: String
    ): FieldError {
        return when {
            value.isBlank() -> FieldError.error(requiredError)
            else -> {
                val intValue = value.toIntOrNull()
                when {
                    intValue == null || intValue !in MIN_DAY_OF_MONTH..MAX_DAY_OF_MONTH ->
                        FieldError.error(invalidError)
                    else -> FieldError.success()
                }
            }
        }
    }
}


