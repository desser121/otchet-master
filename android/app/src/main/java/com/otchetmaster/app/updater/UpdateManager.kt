package com.otchetmaster.app.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.otchetmaster.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val latestVersion: String,
    val apkUrl: String?,
    val isNewer: Boolean,
)

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
)

class UpdateManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val currentVersion = BuildConfig.VERSION_NAME

    /** Запрашивает последний релиз с GitHub и определяет, доступно ли обновление. */
    suspend fun checkForUpdate(): UpdateInfo = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/desser121/otchet-master/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext UpdateInfo(latestVersion = currentVersion, apkUrl = null, isNewer = false)
            }
            val release = json.decodeFromString<GitHubRelease>(response.body!!.string())
            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
            val latest = release.tagName.removePrefix("v")
            UpdateInfo(
                latestVersion = latest,
                apkUrl = apkAsset?.browserDownloadUrl,
                isNewer = isNewerVersion(latest, currentVersion),
            )
        }
    }

    /** Скачивает APK в кэш и возвращает локальный файл. */
    suspend fun downloadApk(url: String): File = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val apkDir = File(context.cacheDir, "apk").apply { mkdirs() }
        val target = File(apkDir, "app-update.apk")

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RuntimeException("Ошибка скачивания: HTTP ${response.code}")
            response.body!!.byteStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
        target
    }

    /** Открывает системный диалог установки для скачанного APK. */
    fun installApk(apkFile: File): Boolean {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val l = latest.split(".").mapNotNull { it.toIntOrNull() }
        val c = current.split(".").mapNotNull { it.toIntOrNull() }
        val size = maxOf(l.size, c.size)
        for (i in 0 until size) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv != cv) return lv > cv
        }
        return false
    }
}
