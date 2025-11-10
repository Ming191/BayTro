package com.example.baytro.utils

/**
 * A wrapper for data that should be consumed only once.
 *
 * This is useful for events like navigation, showing a Snackbar, etc.
 */
class SingleEvent<out T>(private val content: T) {

    private var hasBeenHandled = false

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

}

