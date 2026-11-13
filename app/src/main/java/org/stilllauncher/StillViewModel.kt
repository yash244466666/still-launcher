package org.stilllauncher

import android.app.Application
import android.content.pm.LauncherApps
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.stilllauncher.data.AppFolder
import org.stilllauncher.data.AppLimit
import org.stilllauncher.data.AppRepository
import org.stilllauncher.data.BlockSchedule
import org.stilllauncher.data.BlockedApp
import org.stilllauncher.data.LauncherApp
import org.stilllauncher.data.Settings
import org.stilllauncher.data.SettingsStore
import org.stilllauncher.service.MonitorService

class StillViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SettingsStore.get(application)
    private val repository = AppRepository(application)

    val settings: StateFlow<Settings?> = store.settings.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null
    )

    private val _apps = MutableStateFlow<List<LauncherApp>>(emptyList())
    val apps: StateFlow<List<LauncherApp>> = _apps.asStateFlow()

    init {
        refreshApps()
        observePackageChanges()
    }

    fun refreshApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _apps.value = repository.loadApps()
        }
    }

    fun displayName(packageName: String): String {
        val renamed = settings.value?.renamed?.get(packageName)
        if (!renamed.isNullOrBlank()) return renamed
        return _apps.value.firstOrNull { it.packageName == packageName }?.label
            ?: repository.labelFor(packageName)
    }

    fun launch(app: LauncherApp) = repository.launch(app)

    fun launchPackage(packageName: String) {
        _apps.value.firstOrNull { it.packageName == packageName }?.let(repository::launch)
            ?: repository.launchPackage(packageName)
    }

    fun openAppInfo(packageName: String) = repository.openAppInfo(packageName)
    fun uninstall(packageName: String) = repository.uninstall(packageName)

    fun toggleFavorite(packageName: String) {
        val s = current()
        val updated = if (packageName in s.favorites) {
            s.favorites - packageName
        } else {
            s.favorites + packageName
        }
        write { this.setFavorites(updated) }
    }

    fun rename(packageName: String, name: String) {
        val s = current()
        val map = s.renamed.toMutableMap()
        if (name.isBlank()) map.remove(packageName) else map[packageName] = name.trim()
        write { this.setRenamed(map) }
    }

    fun hide(packageName: String) {
        val s = current()
        write {
            setHidden((s.hidden + packageName).distinct())
            setFavorites(s.favorites - packageName)
        }
    }

    fun unhide(packageName: String) {
        val s = current()
        write { this.setHidden(s.hidden - packageName) }
    }

    fun addBlock(packageName: String, durationMillis: Long) {
        val s = current()
        val until = System.currentTimeMillis() + durationMillis
        val updated = s.blocked
            .filterNot { it.packageName == packageName || it.blockedUntil <= System.currentTimeMillis() } +
            BlockedApp(packageName, until)
        write { this.setBlocked(updated) }
        MonitorService.start(getApplication())
    }

    fun endBlock(packageName: String) {
        val s = current()
        write { setBlocked(s.blocked.filterNot { it.packageName == packageName }) }
    }

    fun saveSchedule(schedule: BlockSchedule) {
        val s = current()
        val updated = s.schedules.filterNot { it.id == schedule.id } + schedule
        write { this.setSchedules(updated) }
        MonitorService.start(getApplication())
    }

    fun deleteSchedule(id: String) {
        val s = current()
        write { setSchedules(s.schedules.filterNot { it.id == id }) }
    }

    fun saveLimit(limit: AppLimit) {
        val s = current()
        val updated = s.limits.filterNot { it.packageName == limit.packageName } + limit
        write { this.setLimits(updated) }
        MonitorService.start(getApplication())
    }

    fun deleteLimit(packageName: String) {
        val s = current()
        write { setLimits(s.limits.filterNot { it.packageName == packageName }) }
    }

    fun createFolder(name: String, packageName: String) {
        val s = current()
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val folders = s.folders.map { folder ->
            folder.copy(packages = folder.packages - packageName)
        }
        val existing = folders.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        val updated = if (existing != null) {
            folders.map {
                if (it.name.equals(trimmed, ignoreCase = true)) {
                    it.copy(packages = (it.packages + packageName).distinct())
                } else it
            }
        } else {
            folders + AppFolder(trimmed, listOf(packageName))
        }
        write {
            setFolders(updated)
            setFavorites(s.favorites - packageName)
        }
    }

    fun moveToFolder(packageName: String, folderName: String) {
        val s = current()
        val updated = s.folders.map { folder ->
            if (folder.name == folderName) {
                folder.copy(packages = (folder.packages + packageName).distinct())
            } else {
                folder.copy(packages = folder.packages - packageName)
            }
        }
        write {
            setFolders(updated)
            setFavorites(s.favorites - packageName)
        }
    }

    fun deleteFolder(name: String) {
        val s = current()
        write { setFolders(s.folders.filterNot { it.name == name }) }
    }

    fun renameFolder(oldName: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val s = current()
        write {
            setFolders(
                s.folders.map {
                    if (it.name == oldName) it.copy(name = trimmed) else it
                }
            )
        }
    }

    fun setIntroDone(value: Boolean) = write { this.setIntroDone(value) }
    fun setThemeId(value: String) = write { this.setThemeId(value) }
    fun setCustomColor(value: Long) = write { this.setCustomColor(value) }
    fun setCustomDark(value: Boolean) = write { this.setCustomDark(value) }
    fun setFontId(value: String) = write { this.setFontId(value) }
    fun setFontSizeSp(value: Int) = write { this.setFontSizeSp(value) }
    fun setTimeFormat(value: String) = write { this.setTimeFormat(value) }
    fun setDateFormat(value: String) = write { this.setDateFormat(value) }
    fun setSwipeUpSearch(value: Boolean) = write { this.setSwipeUpSearch(value) }
    fun setSwipeDownNotifications(value: Boolean) = write { this.setSwipeDownNotifications(value) }
    fun setAutoKeyboard(value: Boolean) = write { this.setAutoKeyboard(value) }
    fun setDoubleTapLock(value: Boolean) = write { this.setDoubleTapLock(value) }
    fun setReminderMaster(value: Boolean) = write { this.setReminderMaster(value) }
    fun setShortVideoBlocking(value: Boolean) = write { this.setShortVideoBlocking(value) }

    fun setMonitorEnabled(value: Boolean) {
        write { this.setMonitorEnabled(value) }
        if (value) MonitorService.start(getApplication()) else MonitorService.stop(getApplication())
    }

    fun setMonochromeEnabled(value: Boolean) = write { this.setMonochromeEnabled(value) }

    fun toggleMonochromePackage(packageName: String) {
        val s = current()
        val updated = if (packageName in s.monochromePackages) {
            s.monochromePackages - packageName
        } else {
            s.monochromePackages + packageName
        }
        write { this.setMonochromePackages(updated) }
    }

    fun setNotificationManagerActive(value: Boolean) = write { this.setNotificationManagerActive(value) }

    fun setNotificationAllowed(value: Set<String>) = write { this.setNotificationAllowed(value) }

    private fun current(): Settings = settings.value ?: Settings()

    private fun write(block: suspend SettingsStore.() -> Unit) {
        viewModelScope.launch {
            store.block()
        }
    }

    private fun observePackageChanges() {
        val launcherApps = getApplication<Application>().getSystemService(LauncherApps::class.java)
            ?: return
        launcherApps.registerCallback(object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) = refreshApps()
            override fun onPackageAdded(packageName: String, user: UserHandle) = refreshApps()
            override fun onPackageChanged(packageName: String, user: UserHandle) = refreshApps()
            override fun onPackagesAvailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean
            ) = refreshApps()

            override fun onPackagesUnavailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean
            ) = refreshApps()
        })
    }
}
