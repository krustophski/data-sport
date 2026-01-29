package com.kvl.cyclotrack.vmix

import org.nanohttpd.protocols.http.NanoHTTPD

class VmixHttpServer(
    port: Int,
    private val getSnapshotJson: () -> String,
    private val getHistoryJson: (Int) -> String
) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {
        val path = session.uri ?: "/"
        val response = when (path) {
            "/vmix" -> newFixedLengthResponse(
                Response.Status.OK,
                "application/json",
                getSnapshotJson()
            )

            "/history" -> {
                val seconds = session.parameters["seconds"]
                    ?.firstOrNull()
                    ?.toIntOrNull()
                    ?: 10
                newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    getHistoryJson(seconds)
                )
            }

            else -> newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                "text/plain",
                "Not found"
            )
        }
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Cache-Control", "no-store")
        return response
    }
}
