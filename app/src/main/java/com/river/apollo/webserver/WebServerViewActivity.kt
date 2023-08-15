package com.river.apollo.webserver

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
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
import com.river.apollo.utils.ServiceUtils.isRtspServerRunning
import com.river.libstreaming.Session
import com.river.libstreaming.SessionBuilder
import com.river.libstreaming.audio.AudioQuality
import com.river.libstreaming.gl.SurfaceView
import com.river.libstreaming.rtsp.RtspServer
import com.river.libstreaming.video.VideoQuality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

private const val TAG = "WebServerViewActivity"

class WebServerViewActivity : AppCompatActivity(), Session.Callback {

    private var hideControlJob: Job? = null
    private var session: Session? = null
    private var webServer: WebServer? = null
    private lateinit var surfaceView: SurfaceView
    private lateinit var localIp: String

    companion object {
        const val DEFAULT_WEBSERVER_DELAY = 200L
        const val HIDE_CONTROL_TIMEOUT = 10_000L
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
            delay(DEFAULT_WEBSERVER_DELAY)
            startWebServer()
        }
        hideControlsAfterTimeout()

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
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
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
        val webServerUrl =
            "(webServerIp: http://$localIp:$DEFAULT_WEBSERVER_PORT, rtsp_server: rtsp://${localIp}:$DEFAULT_STREAMING_SERVER_PORT)"
        findViewById<TextView>(R.id.ip_tv).text = webServerUrl
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
            startWebServer()
            hideControlsAfterTimeout()
        }

        findViewById<ImageView>(R.id.stop_webserver_bt).setOnClickListener {
            // Stop the server
            stopAll()
            hideControlsAfterTimeout()
        }

        findViewById<ImageView>(R.id.back_arrow).setOnClickListener {
            finish()
        }

        findViewById<ImageView>(R.id.switch_camera).setOnClickListener {
            session?.switchCamera()
            Log.d("xxx", "clicking $session")

            hideControlsAfterTimeout()
        }
    }

    @SuppressLint("SetTextI18n")

    private fun stopAll() {
        findViewById<TextView>(R.id.webserver_tv).text = "(webserver_status: stopped)"
        session?.stop()
        session = null
        WebServer.getInstance()?.stop()
        WebServer.nullify()
        stopService(Intent(this, RtspServer::class.java))
    }

    @SuppressLint("ApplySharedPref", "SetTextI18n")
    private fun startWebServer() {
        try {

            startSession()

            if (!isRtspServerRunning(this@WebServerViewActivity, RtspServer::class.java)) {
                // Sets the port of the RTSP server to 1234
                val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()
                editor.putString(RtspServer.KEY_PORT, DEFAULT_STREAMING_SERVER_PORT)
                editor.commit()
                startService(Intent(this, RtspServer::class.java))
            }

            val streamingUrl =
                "rtsp://${localIp}:$DEFAULT_STREAMING_SERVER_PORT"

            Log.d("xxx", "initializing server")

            WebServer.getInstance(
                port = DEFAULT_WEBSERVER_PORT,
                streamingUrl = streamingUrl
            ).setListener(object : WebServer.Listener {
                override fun onPauseStreaming() {

                }

                override fun onPlayStreaming() {

                }

                override fun onSwitchCamera() {
                    session?.switchCamera()
                }

                override fun closeActivity() {
                    runOnUiThread {
                        this@WebServerViewActivity.finish() // Finish the activity on the UI thread
                    }

                }

                override fun stopStreaming() {
                    stopAll()
                }

            })

            findViewById<TextView>(R.id.webserver_tv).text = "(webserver_status: started)"



    } catch (e: IOException)
    {
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

@SuppressLint("SetTextI18n")
override fun onBitrateUpdate(bitrate: Long) {
    findViewById<TextView>(R.id.tv_bitrate).text = "(bitrate: $bitrate)"

}

@SuppressLint("SetTextI18n")
override fun onSessionError(reason: Int, streamType: Int, e: Exception?) {
    findViewById<TextView>(R.id.session_tv).text = "(session_status: error($reason))"
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

@SuppressLint("SetTextI18n")
private fun sessionStartedState() {
    findViewById<ImageView>(R.id.start_webserver_bt)
        .setBackgroundResource(R.drawable.gray_state_button_bg)
    findViewById<ImageView>(R.id.stop_webserver_bt)
        .setBackgroundResource(R.drawable.stop_server_button_bg)
    findViewById<TextView>(R.id.session_tv).text = "(session_status: started)"

}

@SuppressLint("SetTextI18n")
private fun sessionStoppedState() {
    findViewById<ImageView>(R.id.start_webserver_bt)
        .setBackgroundResource(R.drawable.run_state_button_bg)
    findViewById<ImageView>(R.id.stop_webserver_bt)
        .setBackgroundResource(R.drawable.gray_state_button_bg)
    findViewById<TextView>(R.id.session_tv).text = "(session_status: stopped)"
}


}