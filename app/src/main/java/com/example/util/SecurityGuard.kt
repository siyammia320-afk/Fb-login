package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.os.Process
import android.util.Base64
import com.example.service.FloatingOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Proxy
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

object SecurityGuard {

    // Base64 of "https://pastebin.com/raw/GJdhYP6R"
    private const val PASTEBIN_URL_B64 = "aHR0cHM6Ly9wYXN0ZWJpbi5jb20vcmF3L0dKZGhZUDZS"
    private const val PASTEBIN_URL_SHA256 = "815f0b18502f2c8d14a14637ec25123ea0a98f5b5d5b22cd6a02476cd4f53021"

    // Base64 of W-Bloks URL
    private const val WBLOKS_URL_B64 = "aHR0cHM6Ly9tLmZhY2Vib29rLmNvbS9hc3luYy93Ymxva3MvZmV0Y2gvP2FwcGlkPWNvbS5ibG9rcy53d3cuYmxva3MuY2FhLmxvZ2luLmFzeW5jLnNlbmRfbG9naW5fcmVxdWVzdCZ0eXBlPWFjdGlvbiZfX2Jrdj0yOTdmM2NiMjk3MGE0ZGRiZWE3ZTYzYzUxMmQ5NDE2NTA0N2QyMTRjZDM1ZTU5NTQ2ZTlhNDhkYWRjNTQ5NTJj"
    private const val WBLOKS_URL_SHA256 = "4e862730b28e435d1531ee41f1fae9df8a4be8aa9c6a9ac62ded647092c2c826"

    // Expected APK Signing Certificate SHA-256 (from fixed debug.keystore)
    private const val EXPECTED_CERT_SHA256 = "036e1a79edd5ace6d6556087a37b7d41c4258b4908bc6a80e856186a2e26b4e8"
    private const val EXPECTED_PACKAGE = "com.example"

    // Direct NO_PROXY client + system-only CA certificates (via network_security_config.xml)
    // prevents HttpCanary, Reqable, Charles, Fiddler, and proxy/VPN MITM tools from capturing traffic.
    private val secureHttpClient = OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    fun getVerifiedPastebinUrl(context: Context): String {
        return try {
            val decodedBytes = Base64.decode(PASTEBIN_URL_B64, Base64.DEFAULT)
            val decodedStr = String(decodedBytes, Charsets.UTF_8).trim()
            if (sha256Hex(decodedStr.toByteArray(Charsets.UTF_8)) != PASTEBIN_URL_SHA256) {
                terminateImmediately(context)
            }
            decodedStr
        } catch (_: Exception) {
            terminateImmediately(context)
            ""
        }
    }

    fun getVerifiedWBloksUrl(context: Context): String {
        return try {
            val decodedBytes = Base64.decode(WBLOKS_URL_B64, Base64.DEFAULT)
            val decodedStr = String(decodedBytes, Charsets.UTF_8).trim()
            if (sha256Hex(decodedStr.toByteArray(Charsets.UTF_8)) != WBLOKS_URL_SHA256) {
                terminateImmediately(context)
            }
            decodedStr
        } catch (_: Exception) {
            terminateImmediately(context)
            ""
        }
    }

    /**
     * Verifies APK integrity (package name, signing certificate SHA-256, encoded URLs, debugger).
     * Immediately closes the app if any modification or re-signing is detected.
     */
    fun verifySecurityOrExit(context: Context) {
        if (isTampered(context)) {
            terminateImmediately(context)
        }
    }

    private fun isTampered(context: Context): Boolean {
        try {
            // 1. Verify Package Name
            if (context.packageName != EXPECTED_PACKAGE) {
                return true
            }

            // 2. Verify Base64 URL constants haven't been modified in DEX
            val pbDecoded = String(Base64.decode(PASTEBIN_URL_B64, Base64.DEFAULT), Charsets.UTF_8).trim()
            if (sha256Hex(pbDecoded.toByteArray(Charsets.UTF_8)) != PASTEBIN_URL_SHA256) {
                return true
            }
            val wbDecoded = String(Base64.decode(WBLOKS_URL_B64, Base64.DEFAULT), Charsets.UTF_8).trim()
            if (sha256Hex(wbDecoded.toByteArray(Charsets.UTF_8)) != WBLOKS_URL_SHA256) {
                return true
            }

            // 3. Check Debugger
            if (Debug.isDebuggerConnected()) {
                return true
            }

            // 4. Verify APK Signing Certificate SHA-256
            val pm = context.packageManager
            val certBytes: ByteArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val pkgInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = pkgInfo.signingInfo
                if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners?.firstOrNull()?.toByteArray()
                    } else {
                        signingInfo.signingCertificateHistory?.firstOrNull()?.toByteArray()
                    }
                } else null
            } else {
                @Suppress("DEPRECATION")
                val pkgInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                pkgInfo.signatures?.firstOrNull()?.toByteArray()
            }

            if (certBytes != null) {
                val actualCertSha256 = sha256Hex(certBytes)
                if (!actualCertSha256.equals(EXPECTED_CERT_SHA256, ignoreCase = true)) {
                    return true
                }
            }
        } catch (_: Exception) {
            return false
        }
        return false
    }

    /**
     * Fetches the Pastebin Raw URL decoded from Base64.
     * Returns true ONLY if the content trimmed is "ON".
     * If content is "OFF", empty, or anything else, returns false.
     */
    suspend fun checkRemoteSwitchIsOn(context: Context): Boolean = withContext(Dispatchers.IO) {
        verifySecurityOrExit(context)
        val url = getVerifiedPastebinUrl(context)
        if (url.isBlank()) return@withContext false

        val request = Request.Builder()
            .url("$url?t=${System.currentTimeMillis()}")
            .cacheControl(CacheControl.FORCE_NETWORK)
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .header("Pragma", "no-cache")
            .header("Expires", "0")
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )
            .get()
            .build()

        try {
            secureHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val bodyText = response.body?.string()?.replace("\uFEFF", "")?.trim() ?: ""
                // Strictly ON only if the trimmed text is "ON"
                return@withContext bodyText.equals("ON", ignoreCase = true)
            }
        } catch (_: Exception) {
            return@withContext false
        }
    }

    fun terminateImmediately(context: Context) {
        try {
            context.stopService(Intent(context, FloatingOverlayService::class.java))
        } catch (_: Exception) {
        }
        try {
            if (context is Activity) {
                context.finishAndRemoveTask()
                context.finishAffinity()
            }
        } catch (_: Exception) {
        }
        Process.killProcess(Process.myPid())
        exitProcess(0)
    }
}
