package com.pedro.rtpstreamer.webserver

import fi.iki.elonen.NanoHTTPD


class WebServer(port: Int) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {


        var msg = "<html><body><h1>Hello server</h1>\n"
        val parms = session.parms
        msg += if (parms["username"] == null) {
            """<form action='?' method='get'>
  <p>Your name: <input type='text' name='username'></p>
</form>
"""
        } else {
            "<p>Hello, " + parms["username"] + "!</p>"
        }
        return newFixedLengthResponse("$msg</body></html>\n")
    }
}