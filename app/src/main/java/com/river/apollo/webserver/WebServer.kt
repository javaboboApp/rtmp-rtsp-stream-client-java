package com.river.apollo.webserver

import fi.iki.elonen.NanoHTTPD


class WebServer(port: Int, private val streamingUrl: String) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {

        var body = "<html><body><h1>Local server streaming</h1>\n"

        body += """<video id="my-video" controls="controls" preload="none" width="500" height="350"
		   poster="http://bqworks.com/products/assets/videos/bbb/bbb-poster.jpg">
		<source src="http://bqworks.com/products/assets/videos/bbb/bbb-trailer.mp4" type='video/mp4'/>
		<source src="http://bqworks.com/products/assets/videos/bbb/bbb-trailer.ogg" type='video/ogg'/>
	</video>
        """.trimIndent()
//        body += """<iframe src="$streamingUrl" scrolling="no"></iframe>"""
        return newFixedLengthResponse("$body</body></html>\n")
    }
}