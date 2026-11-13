package org.stilllauncher.service

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Recognises the short-video surfaces of apps that are commonly used for
 * endless scrolling. The probes are view IDs exposed by each app's UI; they can
 * change when the app updates, so the feature is opt-in and fails open.
 */
object ShortVideoDetector {

    private val viewIds = mapOf(
        "com.google.android.youtube" to listOf(
            "com.google.android.youtube:id/reel_recycler"
        ),
        "com.instagram.android" to listOf(
            "com.instagram.android:id/clips_viewer_view_pager",
            "com.instagram.android:id/clips_ufi_component"
        ),
        "com.snapchat.android" to listOf(
            "com.snapchat.android:id/spotlight_container",
            "com.snapchat.android:id/opera_viewer"
        )
    )

    fun matches(packageName: String): Boolean = packageName in viewIds

    fun isShowing(packageName: String, root: AccessibilityNodeInfo?): Boolean {
        val ids = viewIds[packageName] ?: return false
        if (root == null) return false
        return runCatching {
            ids.any { id ->
                root.findAccessibilityNodeInfosByViewId(id)?.any { node ->
                    runCatching { node.isVisibleToUser }.getOrDefault(false)
                } == true
            }
        }.getOrDefault(false)
    }
}
