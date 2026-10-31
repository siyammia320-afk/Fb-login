package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val remoteVersionCode: Int,
    val versionName: String,
    val downloadUrl: String
)

object AppUpdateManager {

    private val httpClient = OkHttpClient.Builder()
        .proxy(java.net.Proxy.NO_PROXY)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        val repo = BuildConfig.GITHUB_REPO.ifBlank { "siyammia320-afk/Fb-login" }
        val apiUrl = "https://api.github.com/repos/$repo/releases/latest"

        val request = Request.Builder()
            .url(apiUrl)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "FB-Login-Updater")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)

                val tagName = json.optString("tag_name", "") // e.g., "v12"
                val remoteVersionCode = tagName.filter { it.isDigit() }.toIntOrNull() ?: return@withContext null
                val versionTitle = json.optString("name", "v1.0.$remoteVersionCode")

                val assets = json.optJSONArray("assets") ?: return@withContext null
                var apkDownloadUrl = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    val url = asset.optString("browser_download_url", "")
                    if (name.endsWith(".apk", ignoreCase = true) && url.isNotBlank()) {
                        apkDownloadUrl = url
                        break
                    }
                }

                if (apkDownloadUrl.isNotBlank() && remoteVersionCode > BuildConfig.VERSION_CODE) {
                    return@withContext UpdateInfo(
                        remoteVersionCode = remoteVersionCode,
                        versionName = versionTitle,
                        downloadUrl = apkDownloadUrl
                    )
                }
            }
        } catch (_: Exception) {
        }
        return@withContext null
    }

    suspend fun downloadUpdateApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "FB-Login-Updater")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body ?: return@withContext null
                val totalBytes = body.contentLength()

                val baseDir = context.externalCacheDir ?: context.cacheDir
                val updateDir = File(baseDir, "update")
                if (!updateDir.exists()) {
                    updateDir.mkdirs()
                }
                val apkFile = File(updateDir, "app-update.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var downloadedBytes = 0L
                        var lastReportedProgress = -1

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0) {
                                val progress = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                                if (progress != lastReportedProgress) {
                                    lastReportedProgress = progress
                                    withContext(Dispatchers.Main) {
                                        onProgress(progress)
                                    }
                                }
                            }
                        }
                        output.flush()
                    }
                }
                return@withContext apkFile
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    fun canInstallUnknownApps(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallUnknownAppsSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Toast.makeText(
                context,
                "Allow 'Install unknown apps' permission, then tap UPDATE NOW",
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Install Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
