package org.stilllauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "still_settings")

private object K {
    val introDone = booleanPreferencesKey("intro_done")
    val themeId = stringPreferencesKey("theme_id")
    val customColor = longPreferencesKey("custom_color")
    val customDark = booleanPreferencesKey("custom_dark")
    val fontId = stringPreferencesKey("font_id")
    val fontSizeSp = intPreferencesKey("font_size_sp")
    val timeFormat = stringPreferencesKey("time_format")
    val dateFormat = stringPreferencesKey("date_format")
    val swipeUpSearch = booleanPreferencesKey("swipe_up_search")
    val swipeDownNotifications = booleanPreferencesKey("swipe_down_notifications")
    val autoKeyboard = booleanPreferencesKey("auto_open_keyboard")
    val doubleTapLock = booleanPreferencesKey("double_tap_lock")
    val monitorEnabled = booleanPreferencesKey("monitor_enabled")
    val favorites = stringPreferencesKey("favorites_json")
    val hidden = stringPreferencesKey("hidden_json")
    val folders = stringPreferencesKey("folders_json")
    val renamed = stringPreferencesKey("renamed_json")
    val blocked = stringPreferencesKey("blocked_json")
    val schedules = stringPreferencesKey("schedules_json")
    val limits = stringPreferencesKey("limits_json")
    val reminderMaster = booleanPreferencesKey("reminder_master")
    val shortVideoBlocking = booleanPreferencesKey("short_video_blocking")
    val monochromeEnabled = booleanPreferencesKey("monochrome_enabled")
    val monochromePackages = stringPreferencesKey("monochrome_packages_json")
    val notificationManagerActive = booleanPreferencesKey("notification_manager_active")
    val notificationAllowed = stringPreferencesKey("notification_allowed_json")
}

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private inline fun <reified T> decode(raw: String?, fallback: T): T =
    if (raw.isNullOrBlank()) fallback
    else runCatching { json.decodeFromString<T>(raw) }.getOrDefault(fallback)

class SettingsStore private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val store = appContext.settingsDataStore

    val settings: Flow<Settings> = store.data
        .catch { emit(emptyPreferences()) }
        .map { p ->
            Settings(
                introDone = p[K.introDone] ?: false,
                themeId = p[K.themeId] ?: "system",
                customColor = p[K.customColor] ?: 0xFFF4F1EA,
                customDark = p[K.customDark] ?: false,
                fontId = p[K.fontId] ?: "default",
                fontSizeSp = p[K.fontSizeSp] ?: 16,
                timeFormat = p[K.timeFormat] ?: "system",
                dateFormat = p[K.dateFormat] ?: "system",
                swipeUpSearch = p[K.swipeUpSearch] ?: true,
                swipeDownNotifications = p[K.swipeDownNotifications] ?: true,
                autoKeyboard = p[K.autoKeyboard] ?: false,
                doubleTapLock = p[K.doubleTapLock] ?: false,
                monitorEnabled = p[K.monitorEnabled] ?: true,
                favorites = decode(p[K.favorites], emptyList<String>()),
                hidden = decode(p[K.hidden], emptyList<String>()),
                folders = decode(p[K.folders], emptyList<AppFolder>()),
                renamed = decode(p[K.renamed], emptyMap<String, String>()),
                blocked = decode(p[K.blocked], emptyList<BlockedApp>()),
                schedules = decode(p[K.schedules], emptyList<BlockSchedule>()),
                limits = decode(p[K.limits], emptyList<AppLimit>()),
                reminderMaster = p[K.reminderMaster] ?: true,
                shortVideoBlocking = p[K.shortVideoBlocking] ?: false,
                monochromeEnabled = p[K.monochromeEnabled] ?: false,
                monochromePackages = decode(p[K.monochromePackages], emptySet<String>()),
                notificationManagerActive = p[K.notificationManagerActive] ?: false,
                notificationAllowed = decode(p[K.notificationAllowed], emptySet<String>())
            )
        }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        store.edit { prefs -> block(prefs) }
    }

    suspend fun setIntroDone(value: Boolean) = edit { it[K.introDone] = value }
    suspend fun setThemeId(value: String) = edit { it[K.themeId] = value }
    suspend fun setCustomColor(value: Long) = edit { it[K.customColor] = value }
    suspend fun setCustomDark(value: Boolean) = edit { it[K.customDark] = value }
    suspend fun setFontId(value: String) = edit { it[K.fontId] = value }
    suspend fun setFontSizeSp(value: Int) = edit { it[K.fontSizeSp] = value }
    suspend fun setTimeFormat(value: String) = edit { it[K.timeFormat] = value }
    suspend fun setDateFormat(value: String) = edit { it[K.dateFormat] = value }
    suspend fun setSwipeUpSearch(value: Boolean) = edit { it[K.swipeUpSearch] = value }
    suspend fun setSwipeDownNotifications(value: Boolean) = edit { it[K.swipeDownNotifications] = value }
    suspend fun setAutoKeyboard(value: Boolean) = edit { it[K.autoKeyboard] = value }
    suspend fun setDoubleTapLock(value: Boolean) = edit { it[K.doubleTapLock] = value }
    suspend fun setMonitorEnabled(value: Boolean) = edit { it[K.monitorEnabled] = value }
    suspend fun setReminderMaster(value: Boolean) = edit { it[K.reminderMaster] = value }
    suspend fun setShortVideoBlocking(value: Boolean) = edit { it[K.shortVideoBlocking] = value }

    suspend fun setFavorites(value: List<String>) =
        edit { it[K.favorites] = json.encodeToString(value) }

    suspend fun setHidden(value: List<String>) =
        edit { it[K.hidden] = json.encodeToString(value) }

    suspend fun setFolders(value: List<AppFolder>) =
        edit { it[K.folders] = json.encodeToString(value) }

    suspend fun setRenamed(value: Map<String, String>) =
        edit { it[K.renamed] = json.encodeToString(value) }

    suspend fun setBlocked(value: List<BlockedApp>) =
        edit { it[K.blocked] = json.encodeToString(value) }

    suspend fun setSchedules(value: List<BlockSchedule>) =
        edit { it[K.schedules] = json.encodeToString(value) }

    suspend fun setLimits(value: List<AppLimit>) =
        edit { it[K.limits] = json.encodeToString(value) }

    suspend fun setMonochromeEnabled(value: Boolean) = edit { it[K.monochromeEnabled] = value }

    suspend fun setMonochromePackages(value: Set<String>) =
        edit { it[K.monochromePackages] = json.encodeToString(value) }

    suspend fun setNotificationManagerActive(value: Boolean) =
        edit { it[K.notificationManagerActive] = value }

    suspend fun setNotificationAllowed(value: Set<String>) =
        edit { it[K.notificationAllowed] = json.encodeToString(value) }

    companion object {
        @Volatile
        private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context.applicationContext).also { instance = it }
            }
    }
}
