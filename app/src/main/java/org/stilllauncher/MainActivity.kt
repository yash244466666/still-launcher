package org.stilllauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.stilllauncher.data.Settings
import org.stilllauncher.service.MonitorService
import org.stilllauncher.ui.OnboardingScreen
import org.stilllauncher.ui.StillApp
import org.stilllauncher.ui.theme.StillTheme
import org.stilllauncher.util.Diagnostics
import org.stilllauncher.util.HomeGestureGuard

class MainActivity : ComponentActivity() {

    private val viewModel: StillViewModel by viewModels()
    private var homeRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Diagnostics.record("activity", "onCreate task=$taskId ${describe(intent)}")
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val current = settings
            LaunchedEffect(current?.homeGestureGuard) {
                HomeGestureGuard.setEnabled(current?.homeGestureGuard == true)
            }
            ApplySystemBarAppearance(current)
            LaunchedEffect(current?.introDone, current?.monitorEnabled) {
                if (current?.introDone == true && current.monitorEnabled) {
                    MonitorService.start(this@MainActivity)
                } else if (current?.introDone == true && !current.monitorEnabled) {
                    MonitorService.stop(this@MainActivity)
                }
            }
            if (current == null) {
                val dark = isSystemInDarkTheme()
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(if (dark) Color(0xFF0B0B0B) else Color(0xFFFFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Still Launcher",
                        color = if (dark) Color(0xFFF2F2F2) else Color(0xFF111111),
                        fontSize = 18.sp
                    )
                }
            } else {
                StillTheme(current) {
                    if (!current.introDone) {
                        OnboardingScreen(onFinished = { viewModel.setIntroDone(true) })
                    } else {
                        StillApp(viewModel, current, homeRequest)
                    }
                }
            }
        }
    }

    @Composable
    private fun ApplySystemBarAppearance(settings: Settings?) {
        val systemDark = isSystemInDarkTheme()
        val themeId = settings?.themeId
        val dark = when {
            themeId == "black" -> true
            themeId == "white" || themeId == "eink" -> false
            themeId == "custom" -> settings?.customDark == true
            else -> systemDark
        }
        SideEffect {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Diagnostics.record("activity", "onNewIntent ${describe(intent)}")
        // A HOME or LAUNCHER delivery always returns the UI to the home
        // route. The activity itself stays alive: the task is deliberately
        // left in Recents instead of being excluded from it.
        setIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME) ||
            intent.hasCategory(Intent.CATEGORY_LAUNCHER)
        ) {
            homeRequest++
        }
    }

    override fun onResume() {
        super.onResume()
        HomeGestureGuard.onHomeResumed()
        Diagnostics.record("activity", "onResume task=$taskId")
        viewModel.refreshApps()
        val settings = viewModel.settings.value
        if (settings?.introDone == true && settings.monitorEnabled) {
            MonitorService.start(this)
        }
    }

    override fun onPause() {
        Diagnostics.record("activity", "onPause task=$taskId")
        super.onPause()
    }

    private fun describe(intent: Intent?): String {
        if (intent == null) return "intent=null"
        val categories = intent.categories?.joinToString(",").orEmpty()
        return "action=${intent.action} cats=[$categories]"
    }
}
