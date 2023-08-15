package com.river.apollo.webserver

import fi.iki.elonen.NanoHTTPD


class WebServer(port: Int, private val streamingUrl: String) :
    NanoHTTPD(port) {

    companion object {
        @Volatile
        private var INSTANCE: WebServer? = null

        fun getInstance(port: Int, streamingUrl: String): WebServer {
            return INSTANCE ?: synchronized(this) {
                val instance = WebServer(port, streamingUrl)
                instance.start()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(): WebServer?
        {
            return INSTANCE
        }

    }

   private var listener: Listener? = null

    fun setListener( listener: Listener): WebServer {
        this.listener = listener
        return this
    }
    interface Listener {
        fun onPauseStreaming()

        fun onPlayStreaming()

        fun onSwitchCamera()
        fun closeActivity()
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri

        // Check if the request is for the command endpoint
        if ("/command" == uri) {
            val command = session.parms["cmd"] ?: ""
            val responseText = handleCommand(command)
            return newFixedLengthResponse(Response.Status.OK, "text/plain", responseText)
        }

        val html = """
            <html>
            <head>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                    }
                    .navbar {
                        background-color: #333;
                        overflow: hidden;
                    }
                    .navbar a {
                        float: left;
                        display: block;
                        color: white;
                        text-align: center;
                        padding: 14px 16px;
                        text-decoration: none;
                    }
                    .content {
                        padding: 20px;
                    }
                    .button-container {
                        margin-top: 20px;
                    }
                    .button-container button {
                        margin-right: 10px;
                    }
                </style>
                <script>
                    function sendCommand(command) {
                        var xhr = new XMLHttpRequest();
                        xhr.open("GET", "/command?cmd=" + command, true);
                        xhr.send();
                    }
                </script>
            </head>
            <body>
                <div class="navbar">
                    <a href="#">Apollo Web Server</a>
                </div>
                <div class="content">
                    <h1>Live Video Streaming</h1>
                    <p>In order to watch the content, you need to use VLC Media Player.</p>
                    <a href="$streamingUrl">Click here to start streaming</a>
                     <div class="button-container">
                        <button onclick="sendCommand('switchCamera')">Switch Camera</button>
                        <button onclick="sendCommand('closeWindows')">Close Window</button>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        return newFixedLengthResponse(Response.Status.OK, "text/html", html)
    }

    private fun handleCommand(command: String): String {
        when (command) {
            "play" -> {
                listener?.onPlayStreaming()
                return "Play command received"
            }

            "pause" -> {
                // Perform pause action, e.g., pause video playback
                listener?.onPauseStreaming()
                return "Pause command received"
            }

            "switchCamera" -> {
                // Perform switch camera action, e.g., switch camera feed
                listener?.onSwitchCamera()
                return "Switch Camera command received"
            }

            "closeWindows" -> {
                listener?.closeActivity()
                return "Close Windows command received"
            }

            else -> {
                return "Unknown command: $command"
            }
        }

    }


}
