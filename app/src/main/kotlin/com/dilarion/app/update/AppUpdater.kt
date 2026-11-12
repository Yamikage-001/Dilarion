package com.dilarion.app.update

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.dilarion.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

/**
 * GitHub-hosted OTA update for the production Android build.
 *
 * On launch it reads android-latest.json, compares versionCode, and if a newer
 * signed APK exists the app stays on [UpdateRequiredScreen] until the user taps
 * Update. Android still shows its own install confirmation after that. The new
 * APK must be signed with the same keystore as the installed app.
 *
 * Only the production flavor updates. Staging and debug builds are left alone.
 */
object AppUpdater {
    private const val TAG = "AppUpdater"
    private const val MANIFEST_URL =
        "https://github.com/Yamikage-001/Dilarion/releases/latest/download/android-latest.json"

    data class RequiredUpdate(
        val versionName: String,
        val url: String,
        val busy: Boolean = false,
        val message: String? = null,
    )

    private val http = OkHttpClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow<RequiredUpdate?>(null)
    val state: StateFlow<RequiredUpdate?> = _state

    fun checkOnLaunch(activity: Activity) {
        if (BuildConfig.FLAVOR != "production" || BuildConfig.DEBUG) return
        if (_state.value != null) return
        scope.launch {
            try {
                val info = withContext(Dispatchers.IO) { fetchManifest() } ?: return@launch
                if (info.versionCode <= BuildConfig.VERSION_CODE) return@launch
                _state.value = RequiredUpdate(versionName = info.versionName, url = info.url)
            } catch (e: Exception) {
                Log.d(TAG, "update check skipped: ${e.message}")
            }
        }
    }

    fun beginUpdate(activity: Activity) {
        val current = _state.value ?: return
        if (current.busy) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            _state.value = current.copy(
                message = "Allow Dilarion to install updates, then tap Update again.",
            )
            activity.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${activity.packageName}"),
                )
            )
            return
        }
        _state.value = current.copy(busy = true, message = null)
        scope.launch {
            val apk = try {
                withContext(Dispatchers.IO) { downloadApk(activity, current.url) }
            } catch (e: Exception) {
                Log.e(TAG, "download failed", e)
                null
            }
            val still = _state.value
            if (apk == null || still == null) {
                _state.value = current.copy(
                    busy = false,
                    message = "Couldn't download the update. Check your connection and try again.",
                )
                return@launch
            }
            if (activity.isDestroyed) {
                _state.value = current.copy(busy = false)
                return@launch
            }
            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", apk)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                activity.startActivity(intent)
                _state.value = current.copy(busy = false, message = null)
            } catch (e: Exception) {
                Log.e(TAG, "install intent failed", e)
                _state.value = current.copy(busy = false, message = "Couldn't open the installer.")
            }
        }
    }

    private data class Manifest(val versionCode: Int, val versionName: String, val url: String)

    private fun fetchManifest(): Manifest? {
        val req = Request.Builder().url(MANIFEST_URL).header("Accept", "application/json").build()
        http.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return null
            val body = res.body?.string() ?: return null
            val j = JSONObject(body)
            val url = j.optString("url").ifBlank { return null }
            return Manifest(
                versionCode = j.optInt("versionCode", 0),
                versionName = j.optString("versionName", "?"),
                url = url,
            )
        }
    }

    private fun downloadApk(activity: Activity, url: String): File {
        val out = File(activity.cacheDir, "dilarion-update.apk")
        if (out.exists()) out.delete()
        val req = Request.Builder().url(url).build()
        http.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw IllegalStateException("HTTP ${res.code}")
            out.outputStream().use { s -> res.body?.byteStream()?.copyTo(s) }
        }
        return out
    }
}
