package org.stilllauncher

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Trampoline for the app icon.
 *
 * The home activity deliberately does not advertise CATEGORY_LAUNCHER: a
 * launcher activity with both HOME and LAUNCHER can be started from the app
 * icon into a second, standard recents task. Some gesture implementations
 * then animate to that task instead of the home task. This activity opens the
 * HOME intent for this package and finishes without drawing anything.
 */
class OpenActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val home = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .setPackage(packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(home) }
        finish()
    }
}
