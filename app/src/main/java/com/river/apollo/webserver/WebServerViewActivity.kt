package com.river.apollo.webserver

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.river.apollo.R
import com.river.apollo.utils.NetworkUtils
import com.river.libstreaming.Session
import com.river.libstreaming.SessionBuilder
import com.river.libstreaming.gl.SurfaceView
import com.river.libstreaming.rtsp.RtspServer
import java.io.IOException
import java.lang.Exception



class WebServerViewActivity : AppCompatActivity() {

    private var server: WebServer? = null
    private lateinit var surfaceView: SurfaceView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_server_view)
        initListeners()
        initViews()
    }

    private fun initViews() {
        surfaceView =  findViewById<com.river.libstreaming.gl.SurfaceView>(R.id.surfaceView)
        findViewById<TextView>(R.id.ip_tv).text = "http://${NetworkUtils.getLocalIpAddress(this)}:8080"
    }


    private fun initListeners() {
        findViewById<Button>(R.id.start_webserver_bt).setOnClickListener {
            // Start the server
            startWebServer()
            Toast.makeText(this, "Webserver started !!", Toast.LENGTH_LONG).show()
            findViewById<TextView>(R.id.ip_tv).visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.stop_webserver_bt).setOnClickListener {
            // Stop the server
            stopWebserver()
            Toast.makeText(this, "Webserver stopped !!", Toast.LENGTH_LONG).show()
            findViewById<TextView>(R.id.ip_tv).visibility = View.GONE
        }
    }

    private fun stopWebserver() {
        server?.stop()
    }

    private fun startWebServer() {
        try {
            server = WebServer(port = 8080, streamingUrl = "") // Use your desired port

            val mRtspServer = RtspServer()

            // Configure the streaming session
            SessionBuilder.getInstance()
                .setContext(applicationContext)
                .setAudioEncoder(SessionBuilder.AUDIO_AAC)
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
                .setSurfaceView(surfaceView)
                .setCallback(object : Session.Callback {
                    override fun onBitrateUpdate(bitrate: Long) {
                       Log.d("onBitrateUpdate","bitatrate updated $bitrate")
                    }

                    override fun onSessionError(reason: Int, streamType: Int, e: Exception?) {
                        Log.d("onSessionError","reason  $reason")

                    }

                    override fun onPreviewStarted() {
                        Log.d("onSessionError","onPreviewStarted")
                    }

                    override fun onSessionConfigured() {
                    }

                    override fun onSessionStarted() {
                    }

                    override fun onSessionStopped() {
                    }

                })

            mRtspServer.start()

            SessionBuilder.getInstance().build().start()

            server?.start()


        } catch (e: IOException) {
            e.printStackTrace()
        }
    }











}