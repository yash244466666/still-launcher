package org.stilllauncher.util

import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Small in-memory event log used to diagnose OEM launcher and gesture
 * behaviour (blank overviews, home intents that restart the task, and so on).
 *
 * It stores window package and class names plus activity lifecycle marks. It
 * never records screen content, notification text or app usage, is not written
 * to disk, and is never transmitted anywhere. Closing the process clears it.
 */
object Diagnostics {

    private const val CAPACITY = 200
    private val lock = Any()
    private val buffer = ArrayDeque<String>(CAPACITY)
    private val clock = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    fun record(tag: String, message: String) {
        synchronized(lock) {
            buffer.addLast("${clock.format(Date())}  $tag  $message")
            while (buffer.size > CAPACITY) buffer.removeFirst()
            _lines.value = buffer.toList()
        }
    }

    fun clear() {
        synchronized(lock) {
            buffer.clear()
            _lines.value = emptyList()
        }
    }
}
