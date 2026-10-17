package tv.bradflix.hub

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.provider.Settings
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.KeyEvent
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private data class Provider(val name: String, val packageName: String)

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("bradflix", MODE_PRIVATE) }
    private lateinit var root: LinearLayout
    private val purple: Int get() = intArrayOf(Color.rgb(139,92,246), Color.rgb(14,165,233), Color.rgb(34,197,94), Color.rgb(244,63,94))[prefs.getInt("accent", 0).coerceIn(0, 3)]
    private val ink = Color.rgb(9, 11, 18)
    private val providers: MutableList<Provider> by lazy { readProviders() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
        checkForUpdate(showUpToDate = false)
    }

    private fun readProviders(): MutableList<Provider> {
        val raw = prefs.getString("providers", null)
        if (raw == null) return mutableListOf(
            Provider("Netflix", "com.netflix.ninja"), Provider("YouTube", "com.google.android.youtube.tv"),
            Provider("Prime Video", "com.amazon.amazonvideo.livingroom"), Provider("Disney+", "com.disney.disneyplus"),
            Provider("Plex", "com.plexapp.android"), Provider("Stremio", "com.stremio.one")
        )
        return runCatching {
            val array = JSONArray(raw)
            MutableList(array.length()) { i -> val item = array.getJSONObject(i); Provider(item.getString("name"), item.getString("package")) }
        }.getOrDefault(mutableListOf())
    }

    private fun saveProviders() {
        val array = JSONArray()
        providers.forEach { array.put(JSONObject().put("name", it.name).put("package", it.packageName)) }
        prefs.edit().putString("providers", array.toString()).apply()
    }

    private fun render() {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(44, 28, 44, 24); setBackgroundColor(ink) }
        setContentView(ScrollView(this).apply { isFillViewport = true; addView(root) })
        val title = TextView(this).apply { text = "BRADFLIX"; textSize = 32f; setTextColor(Color.WHITE); letterSpacing = .12f }
        root.addView(title)
        root.addView(TextView(this).apply { text = "Your streaming apps, organized your way"; textSize = 18f; setTextColor(Color.LTGRAY); setPadding(0, 4, 0, 24) })
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        root.addView(row)
        providers.forEach { provider ->
            val button = Button(this).apply {
                text = provider.name
                textSize = 17f
                isFocusable = true; isFocusableInTouchMode = true
                setTextColor(Color.WHITE); setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(34, 38, 52)))
                setOnFocusChangeListener { v, focused -> v.setBackgroundTintList(android.content.res.ColorStateList.valueOf(if (focused) purple else Color.rgb(34, 38, 52))) }
                setOnClickListener { launchProvider(provider) }
                setOnLongClickListener { editProvider(provider); true }
            }
            row.addView(button, LinearLayout.LayoutParams(220, 130).apply { setMargins(0, 0, 18, 0) })
        }
        if (providers.isEmpty()) root.addView(TextView(this).apply { text = "Add a provider to get started."; setTextColor(Color.WHITE); textSize = 20f })
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 24, 0, 0) }
        root.addView(actions)
        actions.addView(actionButton("Add app", ::addProvider))
        actions.addView(actionButton("Check for updates", { checkForUpdate(showUpToDate = true) }))
        actions.addView(actionButton("Customize", ::customize))
        root.addView(TextView(this).apply { text = "Select an app to open it. Long-press an app to edit or remove it. Use the Android TV remote arrows and Select button."; textSize = 15f; setTextColor(Color.GRAY); setPadding(0, 26, 0, 0) })
        root.post { root.findFocus()?.clearFocus(); (row.getChildAt(0) as? View)?.requestFocus() }
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label; isFocusable = true; setTextColor(Color.WHITE)
        setOnClickListener { action() }
        setOnFocusChangeListener { v, focused -> v.setBackgroundTintList(android.content.res.ColorStateList.valueOf(if (focused) purple else Color.rgb(34, 38, 52))) }
        setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(34, 38, 52)))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 62).apply { setMargins(0, 0, 14, 0) }
    }

    private fun launchProvider(provider: Provider) {
        val intent = packageManager.getLaunchIntentForPackage(provider.packageName)
        if (intent != null) startActivity(intent) else AlertDialog.Builder(this).setMessage("${provider.name} is not installed. Install it from your device's app store, then try again.").setPositiveButton("OK", null).show()
    }

    private fun addProvider() = providerDialog(null)
    private fun editProvider(provider: Provider) {
        AlertDialog.Builder(this).setTitle(provider.name).setItems(arrayOf("Edit", "Remove")) { _, which ->
            if (which == 0) providerDialog(provider) else AlertDialog.Builder(this).setMessage("Remove ${provider.name} from BradFlix?").setNegativeButton("Cancel", null).setPositiveButton("Remove") { _, _ -> providers.remove(provider); saveProviders(); render() }.show()
        }.show()
    }

    private fun providerDialog(old: Provider?) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 8, 32, 0) }
        val name = EditText(this).apply { hint = "Display name"; setText(old?.name ?: ""); isSingleLine = true }
        val pkg = EditText(this).apply { hint = "Android app package name"; setText(old?.packageName ?: ""); isSingleLine = true }
        box.addView(name); box.addView(pkg)
        AlertDialog.Builder(this).setTitle(if (old == null) "Add a provider app" else "Edit provider app").setView(box)
            .setNegativeButton("Cancel", null).setPositiveButton("Save") { _, _ ->
                val newName = name.text.toString().trim(); val newPkg = pkg.text.toString().trim()
                if (newName.isNotEmpty() && newPkg.matches(Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"))) {
                    if (old != null) providers.remove(old)
                    providers.add(Provider(newName, newPkg)); saveProviders(); render()
                } else Toast.makeText(this, "Enter a name and a valid package name", Toast.LENGTH_LONG).show()
            }.show()
    }

    private fun customize() {
        val options = arrayOf("Violet", "Ocean", "Forest", "Rose")
        val colors = intArrayOf(Color.rgb(139,92,246), Color.rgb(14,165,233), Color.rgb(34,197,94), Color.rgb(244,63,94))
        AlertDialog.Builder(this).setTitle("Accent color").setSingleChoiceItems(options, prefs.getInt("accent", 0)) { dialog, which ->
            prefs.edit().putInt("accent", which).apply(); dialog.dismiss(); render()
        }.setNegativeButton("Done", null).show()
    }

    private fun checkForUpdate(showUpToDate: Boolean) {
        Thread {
            val latest = runCatching {
                val connection = URL("https://api.github.com/repos/jedixscum/BradFlix-Android-TV/releases/latest").openConnection() as HttpURLConnection
                connection.connectTimeout = 7000; connection.readTimeout = 7000
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            }.getOrNull()
            runOnUiThread {
                if (latest == null || latest.optBoolean("draft", true) || latest.optBoolean("prerelease", true)) {
                    if (showUpToDate) Toast.makeText(this, "No successful build is available yet.", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                val tag = latest.optString("tag_name")
                val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "0.0.0"
                val asset = latest.optJSONArray("assets")?.let { assets -> (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull { it.optString("name").endsWith(".apk") } }
                if (asset == null || tag == prefs.getString("dismissed_update", "") || !isNewer(tag.removePrefix("v"), currentVersion)) {
                    if (showUpToDate) Toast.makeText(this, "BradFlix is up to date.", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                AlertDialog.Builder(this).setTitle("BradFlix update available")
                    .setMessage("Version $tag was built successfully on GitHub Actions. Download and install it now?")
                    .setPositiveButton("Update") { _, _ -> installUpdate(asset.getString("browser_download_url")) }
                    .setNegativeButton("Later") { _, _ -> prefs.edit().putString("dismissed_update", tag).apply() }.show()
            }
        }.start()
    }

    private fun installUpdate(downloadUrl: String) {
        Thread {
            val result = runCatching {
                val connection = URL(downloadUrl).openConnection(); connection.connectTimeout = 15000; connection.readTimeout = 30000
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
                    val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.updates", apk)
                    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(intent)
                }.onFailure { Toast.makeText(this, "Update download failed: ${it.message}", Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) { checkForUpdate(showUpToDate = true); return true }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        if (::root.isInitialized && prefs.getBoolean("install_after_permission", false) &&
            (android.os.Build.VERSION.SDK_INT < 26 || packageManager.canRequestPackageInstalls())) {
            prefs.edit().putBoolean("install_after_permission", false).apply()
            val apk = java.io.File(cacheDir, "bradflix-update.apk")
            if (apk.exists()) {
                val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.updates", apk)
                startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            }
        }
    }

    private fun isNewer(candidate: String, installed: String): Boolean {
        val a = candidate.split('.').map { it.toIntOrNull() ?: 0 }
        val b = installed.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val left = a.getOrElse(i) { 0 }; val right = b.getOrElse(i) { 0 }
            if (left != right) return left > right
        }
        return false
    }
}
