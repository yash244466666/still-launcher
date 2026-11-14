package org.stilllauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.stilllauncher.StillViewModel
import org.stilllauncher.data.Settings

sealed interface Route {
    data object Home : Route
    data class AllApps(val focusSearch: Boolean = false) : Route
    data object Settings : Route
    data object Theme : Route
    data object TextAndFont : Route
    data object DateTime : Route
    data object Gestures : Route
    data object Schedules : Route
    data class ScheduleEditor(val scheduleId: String?) : Route
    data object Limits : Route
    data class LimitEditor(val packageName: String) : Route
    data object Monochrome : Route
    data object Notifications : Route
    data object ScreenTime : Route
    data object HiddenApps : Route
    data object Permissions : Route
    data object InstallHelp : Route
    data object Diagnostics : Route
    data object About : Route
}

@Composable
fun StillApp(viewModel: StillViewModel, settings: Settings, homeRequest: Int) {
    var stack by remember { mutableStateOf(listOf<Route>(Route.Home)) }
    val push: (Route) -> Unit = { stack = stack + it }
    val pop: () -> Unit = { if (stack.size > 1) stack = stack.dropLast(1) }

    androidx.compose.runtime.LaunchedEffect(homeRequest) {
        if (homeRequest > 0) stack = listOf(Route.Home)
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    when (val route = stack.last()) {
        is Route.Home -> HomeScreen(
            viewModel = viewModel,
            settings = settings,
            onOpenAllApps = { push(Route.AllApps(true)) },
            onOpenSettings = { push(Route.Settings) },
            onSetLimit = { pkg -> push(Route.LimitEditor(pkg)) }
        )

        is Route.AllApps -> AllAppsScreen(
            viewModel = viewModel,
            settings = settings,
            focusSearch = route.focusSearch,
            onBack = pop,
            onOpenSettings = { push(Route.Settings) },
            onSetLimit = { pkg -> push(Route.LimitEditor(pkg)) }
        )

        is Route.Settings -> SettingsScreen(
            viewModel = viewModel,
            settings = settings,
            onBack = pop,
            onNavigate = push
        )

        is Route.Theme -> ThemeScreen(
            viewModel = viewModel,
            settings = settings,
            onBack = pop
        )

        is Route.TextAndFont -> TextAndFontScreen(
            viewModel = viewModel,
            settings = settings,
            onBack = pop
        )

        is Route.DateTime -> DateTimeScreen(
            viewModel = viewModel,
            settings = settings,
            onBack = pop
        )

        is Route.Gestures -> GesturesScreen(
            viewModel = viewModel,
            settings = settings,
            onBack = pop
        )

        is Route.Schedules -> SchedulesScreen(
            viewModel = viewModel,
            settings = settings,
            onBack = pop,
            onEdit = { id -> push(Route.ScheduleEditor(id)) }
        )

        is Route.ScheduleEditor -> ScheduleEditorScreen(
            viewModel = viewModel,
            settings = settings,
            scheduleId = route.scheduleId,
            onBack = pop
        )

        is Route.Limits -> LimitsScreen(
            viewModel = viewModel,
            settings = settings,
            onBack = pop,
            onEdit = { pkg -> push(Route.LimitEditor(pkg)) }
        )

        is Route.LimitEditor -> LimitEditorScreen(
            viewModel = viewModel,
            settings = settings,
            packageName = route.packageName,
            onBack = pop
        )

        is Route.Monochrome -> MonochromeScreen(
            viewModel = viewModel,
            settings = settings,
            onBack = pop
        )

        is Route.Notifications -> NotificationsScreen(
            viewModel = viewModel,
            settings = settings,
            onBack = pop
        )

        is Route.ScreenTime -> ScreenTimeScreen(
            viewModel = viewModel,
            settings = settings,
            onBack = pop
        )

        is Route.HiddenApps -> HiddenAppsScreen(
            viewModel = viewModel,
            settings = settings,
            onBack = pop
        )

        is Route.Permissions -> PermissionsScreen(
            settings = settings,
            onBack = pop,
            onOpenHelp = { push(Route.InstallHelp) }
        )

        is Route.InstallHelp -> InstallHelpScreen(onBack = pop)

        is Route.Diagnostics -> DiagnosticsScreen(settings = settings, onBack = pop)

        is Route.About -> AboutScreen(onBack = pop)
    }
}
