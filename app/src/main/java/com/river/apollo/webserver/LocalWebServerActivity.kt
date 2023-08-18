package com.river.apollo.webserver

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.river.apollo.R
import com.river.apollo.utils.NetworkUtils
import com.river.libstreaming.Session
import com.river.libstreaming.SessionBuilder
import com.river.libstreaming.audio.AudioQuality
import com.river.libstreaming.gl.SurfaceView
import com.river.libstreaming.rtsp.RtspServerService
import com.river.libstreaming.video.VideoQuality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException


private const val TAG = "WebServerViewActivity"

class LocalWebServerActivity : AppCompatActivity(), Session.Callback {

    private var hideControlJob: Job? = null
    private var session: Session? = null
    private lateinit var surfaceView: SurfaceView
    private lateinit var localIp: String

    companion object {
        const val DEFAULT_WEBSERVER_DELAY = 500L
        const val HIDE_CONTROL_TIMEOUT = 10_000L
        const val DEFAULT_WEBSERVER_PORT = 0
        const val DEFAULT_STREAMING_SERVER_PORT = "8281"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_server_view)
        setupWindowsFeatures()
        getLocalIp()
        initListeners()
        initViews()
        CoroutineScope(Main).launch {
            delay(DEFAULT_WEBSERVER_DELAY)
            startLocalWebServer()
            startRstServerInService()
        }
        hideControlsAfterTimeout()

    }

    override fun onDestroy() {
        super.onDestroy()
        stopAll()
    }

    private fun hideControlsAfterTimeout() {
        hideControlJob?.cancel()
        hideControlJob = CoroutineScope(Main).launch {
            delay(HIDE_CONTROL_TIMEOUT)
            findViewById<ViewGroup>(R.id.controls_container).isVisible = false
            findViewById<ViewGroup>(R.id.back_arrow).isVisible = false
        }
    }

    private fun showControls() {
        findViewById<ViewGroup>(R.id.controls_container).isVisible = true
        findViewById<ViewGroup>(R.id.back_arrow).isVisible = true
        hideControlsAfterTimeout()
    }


    private fun setupWindowsFeatures() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val decorView = window.decorView
        val uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        decorView.systemUiVisibility = uiOptions
    }

    private fun getLocalIp() {
        this.localIp = NetworkUtils.getLocalIpAddress(this)
    }

    @SuppressLint("SetTextI18n")
    private fun initViews() {
        surfaceView = findViewById(R.id.surfaceView)
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun initListeners() {

        findViewById<SurfaceView>(R.id.surfaceView).setOnTouchListener(OnTouchListener { v, event ->
            // Handle touch events here
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (findViewById<ViewGroup>(R.id.controls_container).isVisible) {
                    hideControlsAfterTimeout()
                } else {
                    showControls()
                }
            }
            true // Return true to consume the event
        })



        findViewById<ImageView>(R.id.start_webserver_bt).setOnClickListener {
            // Start the server
            startLocalWebServer()
            webServerStartedState()
            hideControlsAfterTimeout()
        }

        findViewById<ImageView>(R.id.stop_webserver_bt).setOnClickListener {
            // Stop the server
            stopWebServer()
            weServerStoppedState()
            hideControlsAfterTimeout()
        }

        findViewById<ImageView>(R.id.back_arrow).setOnClickListener {
            finish()
        }

        findViewById<ImageView>(R.id.switch_camera).setOnClickListener {
            session?.switchCamera()
            hideControlsAfterTimeout()
        }
    }


    @SuppressLint("SetTextI18n")
    private fun stopAll() {
        stopRstServer()
        stopWebServer()
    }

    private fun stopRstServer() {
        session?.stop()
        session = null
        stopService(Intent(this, RtspServerService::class.java))
    }

    private fun stopWebServer() {
        WebServer.getInstance()?.stop()
        WebServer.nullify()
    }

    @SuppressLint("ApplySharedPref", "SetTextI18n")
    private fun startLocalWebServer() {
        try {


            val streamingUrl =
                "rtsp://${localIp}:$DEFAULT_STREAMING_SERVER_PORT"


            val webServer = WebServer.getInstance(
                port = DEFAULT_WEBSERVER_PORT,
                streamingUrl = streamingUrl
            )
            webServer.setListener(object : WebServer.Listener {
                override fun onPauseStreaming() {

                }

                override fun onPlayStreaming() {

                }

                override fun onSwitchCamera() {
                    session?.switchCamera()
                }

                override fun closeActivity() {
                    runOnUiThread {
                        this@LocalWebServerActivity.finish() // Finish the activity on the UI thread
                    }

                }

                override fun stopStreaming() {
                    stopRstServer()
                }

            })

            webServer.start()


            val webServerUrl =
                "(webServerIp: http://$localIp:${webServer.listeningPort}, rtsp_server: rtsp://${localIp}:$DEFAULT_STREAMING_SERVER_PORT)"
            findViewById<TextView>(R.id.ip_tv).text = webServerUrl


            findViewById<TextView>(R.id.webserver_tv).text = "(webserver_status: started)"

            webServerStartedState()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun startRstServerInService() {
        //make sure that we stop rtsp server
        stopRstServer()
        // Sets the port of the RTSP server to 1234
        val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()
        editor.putString(RtspServerService.KEY_PORT, DEFAULT_STREAMING_SERVER_PORT)
        editor.commit()
        startSession()
        startService(Intent(this, RtspServerService::class.java))
    }

    private fun startSession() {
        SessionBuilder.getInstance()
            .setCallback(this)
            .setSurfaceView(surfaceView)
            .setContext(applicationContext)
            .setAudioQuality(AudioQuality(16000, 32000))
            .setVideoQuality(VideoQuality(320, 240, 20, 800000))
            .setAudioEncoder(SessionBuilder.AUDIO_AAC)
            .setVideoEncoder(SessionBuilder.VIDEO_H264)

    }

    private fun showProgress() {
        findViewById<View>(R.id.progress).isVisible = true
    }

    private fun hideProgress() {
        findViewById<View>(R.id.progress).isVisible = false
    }

    @SuppressLint("SetTextI18n")
    override fun onBitrateUpdate(bitrate: Long) {
        findViewById<TextView>(R.id.tv_bitrate).text = "(bitrate: $bitrate)"
    }

    @SuppressLint("SetTextI18n")
    override fun onSessionError(reason: Int, streamType: Int, e: Exception?) {
        Log.e("error", "reason $reason onSessionError", e)
        findViewById<TextView>(R.id.session_tv).text = "(session_status: error($reason))"
    }

    override fun onPreviewStarted() {
    }

    override fun onSessionConfigured() {
    }

    @SuppressLint("SetTextI18n")
    override fun onSessionStarted() {
        hideProgress()
        findViewById<TextView>(R.id.explanation_tv).isVisible = false
        findViewById<TextView>(R.id.session_tv).text = "(session_status: started)"
    }

    @SuppressLint("SetTextI18n")
    override fun onSessionStopped() {
        hideProgress()
        findViewById<TextView>(R.id.session_tv).text = "(session_status: stopped)"
        findViewById<TextView>(R.id.explanation_tv).isVisible = true

    }

    @SuppressLint("SetTextI18n")
    private fun webServerStartedState() {
        findViewById<ImageView>(R.id.start_webserver_bt)
            .setBackgroundResource(R.drawable.gray_state_button_bg)
        findViewById<ImageView>(R.id.stop_webserver_bt)
            .setBackgroundResource(R.drawable.stop_server_button_bg)
        findViewById<TextView>(R.id.webserver_tv).text = "(webserver_status: started)"
    }

    @SuppressLint("SetTextI18n")
    private fun weServerStoppedState() {
        findViewById<ImageView>(R.id.start_webserver_bt)
            .setBackgroundResource(R.drawable.run_state_button_bg)
        findViewById<ImageView>(R.id.stop_webserver_bt)
            .setBackgroundResource(R.drawable.gray_state_button_bg)
        findViewById<TextView>(R.id.webserver_tv).text = "(webserver_status: stopped)"

    }


}