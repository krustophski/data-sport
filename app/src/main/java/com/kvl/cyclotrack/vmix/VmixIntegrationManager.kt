package com.kvl.cyclotrack.vmix

import android.content.Context
import org.nanohttpd.protocols.http.NanoHTTPD
import java.io.IOException

object VmixIntegrationManager {
    private const val frameIntervalMs = 500L
    private const val port = 8080
    private val lock = Any()
    private var running = false

    val hub: LiveDataHub = LiveDataHub()

    private var frameEngine: FrameEngine? = null
    private var server: VmixHttpServer? = null

    fun isRunning(): Boolean = running

    fun startIfNeeded(context: Context) {
        synchronized(lock) {
            if (running) {
                return
            }
            val engine = FrameEngine(hub, frameIntervalMs)
            val httpServer = VmixHttpServer(
                port = port,
                getSnapshotJson = { engine.getLastVmixSnapshotJson() },
                getHistoryJson = { seconds -> engine.getHistoryJson(seconds) }
            )
            try {
                engine.start()
                httpServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
                frameEngine = engine
                server = httpServer
                running = true
            } catch (e: IOException) {
                engine.stop()
                frameEngine = null
                server = null
                running = false
            }
        }
    }

    fun stopIfRunning() {
        synchronized(lock) {
            if (!running) {
                return
            }
            try {
                server?.stop()
            } finally {
                server = null
                frameEngine?.stop()
                frameEngine = null
                running = false
                clearHub()
            }
        }
    }

    private fun clearHub() {
        hub.lastGpsSpeed = null
        hub.lastBleSpeed = null
        hub.lastLocation = null
        hub.lastAltitudeM = null
        hub.lastDistanceM = null
        hub.lastHeartRate = null
        hub.lastCadence = null
        hub.lastPower = null
    }
}
