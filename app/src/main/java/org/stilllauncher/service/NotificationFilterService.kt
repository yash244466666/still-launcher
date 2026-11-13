package org.stilllauncher.service

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.stilllauncher.data.Settings
import org.stilllauncher.data.SettingsStore

data class FilteredNotification(
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long
)

object FilteredNotificationStore {
    private val _items = MutableStateFlow<List<FilteredNotification>>(emptyList())
    val items: StateFlow<List<FilteredNotification>> = _items.asStateFlow()

    fun add(item: FilteredNotification) {
        _items.update { current -> (listOf(item) + current).take(50) }
    }

    fun clear() {
        _items.value = emptyList()
    }
}

class NotificationFilterService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var settings: Settings = Settings()

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            SettingsStore.get(this@NotificationFilterService).settings.collect {
                settings = it
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        filterExisting()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn != null) filter(sbn)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun filterExisting() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        active.forEach(::filter)
    }

    private fun filter(sbn: StatusBarNotification) {
        val current = settings
        if (!current.notificationManagerActive) return
        val allowed = current.notificationAllowed
        if (allowed.isEmpty()) return
        if (sbn.packageName == packageName) return
        if (sbn.packageName in allowed) return

        val flags = sbn.notification?.flags ?: 0
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) return
        if (flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return

        runCatching { cancelNotification(sbn.key) }

        val extras = sbn.notification?.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras?.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
            ?: ""
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: ""

        FilteredNotificationStore.add(
            FilteredNotification(
                packageName = sbn.packageName,
                title = title,
                text = text,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}
