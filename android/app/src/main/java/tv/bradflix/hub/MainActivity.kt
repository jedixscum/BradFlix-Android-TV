package tv.bradflix.hub

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.webkit.WebViewAssetLoader
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("bradflix", MODE_PRIVATE) }
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
        webView = WebView(this).apply {
            setBackgroundColor(android.graphics.Color.rgb(9, 11, 18))
            isFocusable = true
            isFocusableInTouchMode = true
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.mediaPlaybackRequiresUserGesture = true
            addJavascriptInterface(AndroidBridge(), "BradFlixBridge")
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest) =
                    assetLoader.shouldInterceptRequest(request.url)

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    if (uri.host == "appassets.androidplatform.net") return false
                    if (uri.scheme == "https" || uri.scheme == "http") {
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                        return true
                    }
                    return true
                }
            }
        }
        setContentView(webView)
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
        checkForUpdate(showUpToDate = false)
    }

    private inner class AndroidBridge {
        @JavascriptInterface
        fun play(payload: String): Boolean {
            val data = runCatching { JSONObject(payload) }.getOrNull() ?: return false
            val raw = data.optString("url")
            val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return false
            if (uri.scheme !in listOf("https", "http")) return false
            val path = uri.path.orEmpty().lowercase()
            val mime = when {
                path.endsWith(".m3u8") -> "application/x-mpegURL"
                path.endsWith(".mpd") -> "application/dash+xml"
                path.endsWith(".mkv") -> "video/x-matroska"
                path.endsWith(".mp4") -> "video/mp4"
                else -> "video/*"
            }
            runOnUiThread {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime)
                        .putExtra(Intent.EXTRA_TITLE, data.optString("title", "BradFlix"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(Intent.createChooser(intent, "Choose a video player"))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(this@MainActivity, "Install a compatible video player to play this source.", Toast.LENGTH_LONG).show()
                }
            }
            return true
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_MENU) {
            checkForUpdate(showUpToDate = true)
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onBackPressed() {
        if (!::webView.isInitialized) { super.onBackPressed(); return }
        webView.evaluateJavascript("window.bradflixHandleBack ? window.bradflixHandleBack() : false") { handled ->
            if (handled != "true") super.onBackPressed()
        }
    }

    private fun checkForUpdate(showUpToDate: Boolean) {
        Thread {
            val latest = runCatching {
                val connection = URL("https://api.github.com/repos/jedixscum/BradFlix-Android-TV/releases/latest").openConnection() as HttpURLConnection
                connection.connectTimeout = 7000
                connection.readTimeout = 7000
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            }.getOrNull()
            runOnUiThread {
                if (latest == null || latest.optBoolean("draft", true) || latest.optBoolean("prerelease", true)) {
                    if (showUpToDate) Toast.makeText(this, "No published successful build is available yet.", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                val tag = latest.optString("tag_name")
                val version = packageManager.getPackageInfo(packageName, 0).versionName ?: "0.0.0"
                val asset = latest.optJSONArray("assets")?.let { assets ->
                    (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull { it.optString("name").endsWith(".apk") }
                }
                if (asset == null || tag == prefs.getString("dismissed_update", "") || !isNewer(tag.removePrefix("v"), version)) {
                    if (showUpToDate) Toast.makeText(this, "BradFlix is up to date.", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                AlertDialog.Builder(this)
                    .setTitle("BradFlix update available")
                    .setMessage("Version $tag passed its GitHub Actions build and was published. Download and install it now?")
                    .setPositiveButton("Update") { _, _ -> installUpdate(asset.getString("browser_download_url")) }
                    .setNegativeButton("Later") { _, _ -> prefs.edit().putString("dismissed_update", tag).apply() }
                    .show()
            }
        }.start()
    }

    private fun installUpdate(downloadUrl: String) {
        Thread {
            val result = runCatching {
                val connection = URL(downloadUrl).openConnection()
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                val apk = java.io.File(cacheDir, "bradflix-update.apk")
                connection.getInputStream().use { input -> apk.outputStream().use { input.copyTo(it) } }
                apk
            }
            runOnUiThread {
                result.onSuccess { apk ->
                    if (android.os.Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
                        prefs.edit().putBoolean("install_after_permission", true).apply()
                        startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                        Toast.makeText(this, "Allow BradFlix to install updates, then return here.", Toast.LENGTH_LONG).show()
                        return@onSuccess
                    }
                    val uri = FileProvider.getUriForFile(this, "$packageName.updates", apk)
                    val intent = Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(intent)
                }.onFailure { Toast.makeText(this, "Update download failed: ${it.message}", Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized && prefs.getBoolean("install_after_permission", false) &&
            (android.os.Build.VERSION.SDK_INT < 26 || packageManager.canRequestPackageInstalls())) {
            prefs.edit().putBoolean("install_after_permission", false).apply()
            val apk = java.io.File(cacheDir, "bradflix-update.apk")
            if (apk.exists()) {
                val uri = FileProvider.getUriForFile(this, "$packageName.updates", apk)
                startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            }
        }
    }

    private fun isNewer(candidate: String, installed: String): Boolean {
        val a = candidate.split('.').map { it.toIntOrNull() ?: 0 }
        val b = installed.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val left = a.getOrElse(i) { 0 }
            val right = b.getOrElse(i) { 0 }
            if (left != right) return left > right
        }
        return false
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.removeJavascriptInterface("BradFlixBridge")
            webView.destroy()
        }
        super.onDestroy()
    }
}
