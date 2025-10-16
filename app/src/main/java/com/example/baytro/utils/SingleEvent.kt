package com.example.baytro.utils

import androidx.lifecycle.Observer

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

    /**
     * Returns the content, even if it has been handled.
     */
    fun peekContent(): T = content
}

/**
 * An [Observer] for [SingleEvent]s, simplifying the boilerplate of checking if the event has been handled.
 *
 * [onEventUnhandledContent] is *only* called if the [SingleEvent]'s content has not been handled.
 */
class EventObserver<T>(private val onEventUnhandledContent: (T) -> Unit) : Observer<SingleEvent<T>> {
    override fun onChanged(value: SingleEvent<T>) {
        value.getContentIfNotHandled()?.let {
            onEventUnhandledContent(it)
        }
    }
}
