package com.river.apollo.webserver

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.river.apollo.utils.NetworkUtils
import com.river.libstreaming.Session
import com.river.libstreaming.SessionBuilder
import com.river.libstreaming.audio.AudioQuality
import com.river.libstreaming.gl.SurfaceView
import com.river.libstreaming.rtsp.RtspServer
import com.river.libstreaming.video.VideoQuality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.NonCancellable.start
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException


class WebServerViewActivity : AppCompatActivity(), Session.Callback {

    private var session: Session? = null
    private var server: WebServer? = null
    private lateinit var surfaceView: SurfaceView
    private lateinit var localIp: String

    companion object {
        const val DEFAULT_WEBSERVER_PORT = 8186
        const val DEFAULT_STREAMING_SERVER_PORT = "1234"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.river.apollo.R.layout.activity_web_server_view)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        getLocalIp()
        initListeners()
        initViews()
        CoroutineScope(Main).launch {
            delay(200)
            startWebServer()
        }

    }

    private fun getLocalIp() {
        this.localIp = NetworkUtils.getLocalIpAddress(this)
    }

    private fun initViews() {
        surfaceView = findViewById(com.river.apollo.R.id.surfaceView)
        val webServerUrl = "http://$localIp:$DEFAULT_WEBSERVER_PORT"
        findViewById<TextView>(com.river.apollo.R.id.ip_tv).text = webServerUrl
    }


    private fun initListeners() {
        findViewById<ImageView>(com.river.apollo.R.id.start_webserver_bt).setOnClickListener {
            // Start the server
            startWebServer()
        }

        findViewById<ImageView>(com.river.apollo.R.id.stop_webserver_bt).setOnClickListener {
            // Stop the server
            stopWebserver()
        }
    }

    private fun stopWebserver() {
        findViewById<TextView>(com.river.apollo.R.id.ip_tv).visibility = View.GONE
        session?.stop()
        server?.stop()
        server = null
        session = null
        stopService(Intent(this, RtspServer::class.java))
    }

    @SuppressLint("ApplySharedPref")
    private fun startWebServer() {
        try {

            if (server != null) {
                Log.d("startWebServer", "server already started !!")
                return
            }

            findViewById<TextView>(com.river.apollo.R.id.ip_tv).visibility = View.VISIBLE

            // Sets the port of the RTSP server to 1234
            val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()
            editor.putString(RtspServer.KEY_PORT, DEFAULT_STREAMING_SERVER_PORT)
            editor.commit()

            session = SessionBuilder.getInstance()
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_AAC)
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
                .setSurfaceView(surfaceView)
                .setCallback(this)
                .setAudioQuality(AudioQuality(16000, 32000))
                .setVideoQuality(VideoQuality(320, 240, 20, 500000)).build()

            session?.start()

            startService(Intent(this, RtspServer::class.java))

            val streamingUrl =
                "rtsp://${NetworkUtils.getLocalIpAddress(this)}:$DEFAULT_STREAMING_SERVER_PORT"

            server = WebServer(
                port = DEFAULT_WEBSERVER_PORT,
                streamingUrl = streamingUrl
            )

            server?.start()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onBitrateUpdate(bitrate: Long) {

    }

    override fun onSessionError(reason: Int, streamType: Int, e: Exception?) {

    }

    override fun onPreviewStarted() {

    }

    override fun onSessionConfigured() {

    }

    override fun onSessionStarted() {
        findViewById<ImageView>(com.river.apollo.R.id.start_webserver_bt)
            .setBackgroundResource(com.river.apollo.R.drawable.gray_state_button_bg)
        findViewById<ImageView>(com.river.apollo.R.id.stop_webserver_bt)
            .setBackgroundResource(com.river.apollo.R.drawable.stop_server_button_bg)
    }

    override fun onSessionStopped() {
        findViewById<ImageView>(com.river.apollo.R.id.start_webserver_bt)
            .setBackgroundResource(com.river.apollo.R.drawable.run_state_button_bg)
        findViewById<ImageView>(com.river.apollo.R.id.stop_webserver_bt)
            .setBackgroundResource(com.river.apollo.R.drawable.gray_state_button_bg)
    }


}