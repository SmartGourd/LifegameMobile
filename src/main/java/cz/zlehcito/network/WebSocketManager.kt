package cz.zlehcito.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class WebSocketManager(private val url: String) {

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    fun connect(listener: WebSocketListener) {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, listener)
    }

    fun sendMessage(message: JSONObject) {
        webSocket?.send(message.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        client.dispatcher.executorService.shutdown()
    }
}