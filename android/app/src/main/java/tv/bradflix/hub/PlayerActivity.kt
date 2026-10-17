package tv.bradflix.hub

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import org.json.JSONObject

/** Full-screen in-app video playback with Android TV's standard D-pad controls. */
class PlayerActivity : Activity() {
    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var payload: JSONObject? = null
    private var resultSent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)

        val data = runCatching { JSONObject(intent.getStringExtra(EXTRA_PAYLOAD) ?: "") }.getOrNull()
        val streamUrl = data?.optString("url").orEmpty()
        val uri = runCatching { Uri.parse(streamUrl) }.getOrNull()
        if (data == null || uri == null || uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank()) {
            Toast.makeText(this, "This video link is not supported.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        payload = data
        val view = PlayerView(this).apply {
            useController = true
            controllerAutoShow = true
            controllerShowTimeoutMs = 5000
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            setBackgroundColor(android.graphics.Color.BLACK)
            contentDescription = data.optString("title", "BradFlix player")
        }
        playerView = view
        setContentView(FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            addView(view, FrameLayout.LayoutParams(-1, -1))
        })

        val headers = mutableMapOf<String, String>()
        val headerJson = data.optJSONObject("headers")
        if (headerJson != null) {
            val keys = headerJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = headerJson.optString(key)
                if (key.isNotBlank() && value.isNotBlank()) headers[key] = value
            }
        }
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(headers)
        val sourceFactory = DefaultDataSource.Factory(this, httpFactory)
        val exo = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(sourceFactory))
            .build()
        player = exo
        view.player = exo
        applyTrackPreferences(exo, data)
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) finishWithProgress()
            }

            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(this@PlayerActivity, "Could not play this source: ${error.errorCodeName}", Toast.LENGTH_LONG).show()
                finishWithProgress()
            }
        })
        exo.setMediaItem(makeMediaItem(uri, data))
        val startSeconds = data.optDouble("start", 0.0).coerceAtLeast(0.0)
        if (startSeconds > 0) exo.seekTo((startSeconds * 1000).toLong())
        exo.prepare()
        exo.playWhenReady = true
    }

    private fun applyTrackPreferences(exo: ExoPlayer, data: JSONObject) {
        val builder = exo.trackSelectionParameters.buildUpon()
        data.optString("audioLanguage").takeIf { it.isNotBlank() }?.let { builder.setPreferredAudioLanguage(it) }
        data.optString("subtitleLanguage").takeIf { it.isNotBlank() }?.let { builder.setPreferredTextLanguage(it) }
        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, data.optBoolean("subtitlesOff", false))
        exo.trackSelectionParameters = builder.build()
    }

    private fun makeMediaItem(uri: Uri, data: JSONObject): MediaItem {
        val builder = MediaItem.Builder()
            .setUri(uri)
            .setMediaId(data.optString("title", "BradFlix"))
        when (uri.path.orEmpty().substringAfterLast('.', "").lowercase()) {
            "m3u8" -> builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            "mpd" -> builder.setMimeType(MimeTypes.APPLICATION_MPD)
        }
        val subtitles = data.optJSONArray("subtitles")
        val subtitleConfigurations = mutableListOf<MediaItem.SubtitleConfiguration>()
        if (subtitles != null) {
            for (i in 0 until subtitles.length()) {
                val subtitleUrl = subtitles.optString(i)
                val subtitleUri = runCatching { Uri.parse(subtitleUrl) }.getOrNull() ?: continue
                if (subtitleUri.scheme !in listOf("http", "https") || subtitleUri.host.isNullOrBlank()) continue
                val extension = subtitleUri.path.orEmpty().substringAfterLast('.', "").lowercase()
                val mime = when (extension) {
                    "vtt" -> MimeTypes.TEXT_VTT
                    "srt" -> MimeTypes.APPLICATION_SUBRIP
                    "ssa", "ass" -> MimeTypes.TEXT_SSA
                    else -> continue
                }
                val subtitle = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
                    .setMimeType(mime)
                    .setLanguage(data.optString("subtitleLanguage").ifBlank { "und" })
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .setLabel("Subtitle ${i + 1}")
                    .build()
                subtitleConfigurations.add(subtitle)
            }
        }
        builder.setSubtitleConfigurations(subtitleConfigurations)
        return builder.build()
    }

    private fun finishWithProgress() {
        if (resultSent) return
        resultSent = true
        val exo = player
        val result = JSONObject().apply {
            put("profileId", payload?.optString("profileId", ""))
            put("context", payload?.optJSONObject("context") ?: JSONObject())
            put("position", exo?.currentPosition?.coerceAtLeast(0) ?: 0)
            val duration = exo?.duration ?: C.TIME_UNSET
            put("duration", if (duration == C.TIME_UNSET || duration < 0) 0 else duration)
        }
        setResult(RESULT_OK, intent.putExtra(EXTRA_RESULT, result.toString()))
        finish()
    }

    override fun onBackPressed() {
        finishWithProgress()
    }

    override fun onStop() {
        playerView?.player = null
        player?.release()
        player = null
        super.onStop()
    }

    companion object {
        const val EXTRA_PAYLOAD = "tv.bradflix.hub.player_payload"
        const val EXTRA_RESULT = "tv.bradflix.hub.player_result"
    }
}
