package com.pedro.rtpstreamer.webserver

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.pedro.rtpstreamer.R
import java.io.IOException


class WebServerViewActivity : AppCompatActivity() {
    private var server: WebServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_server_view)
        initListeners()

    }

    private fun initListeners() {
        findViewById<Button>(R.id.start_webserver_bt).setOnClickListener {
            // Start the server
            startWebServer()
        }

        findViewById<Button>(R.id.stop_webserver_bt).setOnClickListener {
            // Stop the server
            stopWebserver()
        }
    }

    private fun stopWebserver() {
        server?.stop()
    }

    private fun startWebServer() {
        try {
            server = WebServer(8080) // Use your desired port
            server?.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


}