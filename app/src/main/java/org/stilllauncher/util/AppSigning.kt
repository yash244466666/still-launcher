package org.stilllauncher.util

import android.content.Context
import android.content.pm.PackageManager
import java.security.MessageDigest

/**
 * SHA-256 fingerprint of the certificate that signed this installed APK.
 * It can be compared with `apksigner verify --print-certs` and with the value
 * published in the README for official release builds.
 */
fun signingCertificateSha256(context: Context): String? = runCatching {
    val info = context.packageManager.getPackageInfo(
        context.packageName,
        PackageManager.GET_SIGNING_CERTIFICATES
    )
    val signer = info.signingInfo?.apkContentsSigners?.firstOrNull()
        ?: return@runCatching null
    MessageDigest.getInstance("SHA-256")
        .digest(signer.toByteArray())
        .joinToString(":") { "%02X".format(it.toInt() and 0xFF) }
}.getOrNull()
