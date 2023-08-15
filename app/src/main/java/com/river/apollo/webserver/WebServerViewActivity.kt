package com.river.apollo.webserver

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.river.apollo.R
import com.river.apollo.utils.NetworkUtils
import com.river.libstreaming.SessionBuilder
import com.river.libstreaming.audio.AudioQuality
import com.river.libstreaming.gl.SurfaceView
import com.river.libstreaming.rtsp.RtspServer
import com.river.libstreaming.video.VideoQuality
import java.io.IOException


class WebServerViewActivity : AppCompatActivity() {

    private var server: WebServer? = null
    private lateinit var surfaceView: SurfaceView
    private lateinit var localIp: String

    companion object {
        const val DEFAULT_WEBSERVER_PORT = 8186
        const val DEFAULT_STREAMING_SERVER_PORT = "1234"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_server_view)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        getLocalIp()
        initListeners()
        initViews()
        startWebServer()
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
            Toast.makeText(this, "Webserver started !!", Toast.LENGTH_LONG).show()
            findViewById<TextView>(R.id.ip_tv).visibility = View.VISIBLE
        }

        findViewById<ImageView>(R.id.stop_webserver_bt).setOnClickListener {
            // Stop the server
            stopWebserver()
            Toast.makeText(this, "Webserver stopped !!", Toast.LENGTH_LONG).show()
            findViewById<TextView>(R.id.ip_tv).visibility = View.GONE
        }
    }

    private fun stopWebserver() {
        server?.stop()
        server = null
        stopService(Intent(this, RtspServer::class.java))
    }

    @SuppressLint("ApplySharedPref")
    private fun startWebServer() {
        try {

           if( server?.isAlive == true ){
               Log.d("startWebServer", "server already started !!")
               return
           }

            // Sets the port of the RTSP server to 1234
            val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()
            editor.putString(RtspServer.KEY_PORT, DEFAULT_STREAMING_SERVER_PORT)
            editor.commit()

            SessionBuilder.getInstance()
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_AAC)
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
                .setSurfaceView(surfaceView)
                .setAudioQuality(AudioQuality(16000, 32000))
                .setVideoQuality(VideoQuality(320, 240, 20, 500000))

            startService(Intent(this, RtspServer::class.java))

            val streamingUrl =  "rtsp://${NetworkUtils.getLocalIpAddress(this)}:$DEFAULT_STREAMING_SERVER_PORT"

            server = WebServer(
                port = DEFAULT_WEBSERVER_PORT,
                streamingUrl =streamingUrl
            )

            server?.start()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


}