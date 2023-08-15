package com.river.apollo.webserver

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.river.apollo.R
import com.river.apollo.utils.NetworkUtils
import com.river.apollo.utils.ServiceUtils.isRtspServerRunning
import com.river.libstreaming.Session
import com.river.libstreaming.SessionBuilder
import com.river.libstreaming.audio.AudioQuality
import com.river.libstreaming.gl.SurfaceView
import com.river.libstreaming.rtsp.RtspServer
import com.river.libstreaming.video.VideoQuality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

private const val TAG = "WebServerViewActivity"
class WebServerViewActivity : AppCompatActivity(), Session.Callback {

    private var session: Session? = null
    private var server: WebServer? = null
    private lateinit var surfaceView: SurfaceView
    private lateinit var localIp: String

    companion object {
        const val DEFAULT_WEBSERVER_PORT = 8080
        const val DEFAULT_STREAMING_SERVER_PORT = "1234"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_server_view)
        setupWindowsFeatures()
        getLocalIp()
        initListeners()
        initViews()
        CoroutineScope(Main).launch {
            delay(200)
            startWebServer()
        }

    }

    private fun setupWindowsFeatures() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val decorView = window.decorView
        val uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        decorView.systemUiVisibility = uiOptions
    }

    private fun getLocalIp() {
        this.localIp = NetworkUtils.getLocalIpAddress(this)
    }

    private fun initViews() {
        surfaceView = findViewById(R.id.surfaceView)
        val webServerUrl = "http://$localIp:$DEFAULT_WEBSERVER_PORT"
        findViewById<TextView>(R.id.ip_tv).text = webServerUrl
    }


    private fun initListeners() {
        findViewById<ImageView>(R.id.start_webserver_bt).setOnClickListener {
            // Start the server
            startWebServer()
        }

        findViewById<ImageView>(R.id.stop_webserver_bt).setOnClickListener {
            // Stop the server
            stopWebserver()
        }

        findViewById<ImageView>(R.id.switch_camera).setOnClickListener {
            session?.switchCamera()
        }
    }

    private fun stopWebserver() {
        findViewById<TextView>(R.id.ip_tv).visibility = View.GONE
        session?.stop()
        server?.stop()
        server = null
        session = null
        stopService(Intent(this, RtspServer::class.java))
    }

    @SuppressLint("ApplySharedPref")
    private fun startWebServer() {
        try {

            findViewById<TextView>(R.id.ip_tv).visibility = View.VISIBLE

            startSession()

            if (!isRtspServerRunning(this@WebServerViewActivity, RtspServer::class.java)) {
                // Sets the port of the RTSP server to 1234
                val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()
                editor.putString(RtspServer.KEY_PORT, DEFAULT_STREAMING_SERVER_PORT)
                editor.commit()
                startService(Intent(this, RtspServer::class.java))
            }

            if (server?.isAlive == false) {
                val streamingUrl =
                    "rtsp://${localIp}:$DEFAULT_STREAMING_SERVER_PORT"

                server = WebServer(
                    port = DEFAULT_WEBSERVER_PORT,
                    streamingUrl = streamingUrl
                )
                server?.start()
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun startSession() {
        session = SessionBuilder.getInstance()
            .setContext(getApplicationContext())
            .setAudioEncoder(SessionBuilder.AUDIO_AAC)
            .setVideoEncoder(SessionBuilder.VIDEO_H264)
            .setSurfaceView(surfaceView)
            .setCallback(this)
            .setAudioQuality(AudioQuality(16000, 32000))
            .setVideoQuality(VideoQuality(320, 240, 20, 500000)).build()
        session?.start()
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
        sessionStartedState()
    }

    override fun onSessionStopped() {
        sessionStoppedState()
    }

    private fun sessionStartedState() {
        findViewById<ImageView>(R.id.start_webserver_bt)
            .setBackgroundResource(R.drawable.gray_state_button_bg)
        findViewById<ImageView>(R.id.stop_webserver_bt)
            .setBackgroundResource(R.drawable.stop_server_button_bg)
    }

    private fun sessionStoppedState() {
        findViewById<ImageView>(R.id.start_webserver_bt)
            .setBackgroundResource(R.drawable.run_state_button_bg)
        findViewById<ImageView>(R.id.stop_webserver_bt)
            .setBackgroundResource(R.drawable.gray_state_button_bg)
    }


}