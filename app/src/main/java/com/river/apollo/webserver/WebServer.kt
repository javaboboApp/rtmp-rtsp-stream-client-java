package com.river.apollo.webserver

import fi.iki.elonen.NanoHTTPD


class WebServer(port: Int, private val streamingUrl: String) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {

        val html = "<html><body>" +
                "<h1>Live Video Streaming</h1>" +
                "<video width=\"640\" height=\"360\" controls>" +
                "<source src=\"rtsp://192.168.1.22:8086\" type=\"application/x-rtsp\">" +
                "Your browser does not support the video tag." +
                "</video>" +
                "</body></html>"
//        body += """<iframe src="$streamingUrl" scrolling="no"></iframe>"""
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }
}