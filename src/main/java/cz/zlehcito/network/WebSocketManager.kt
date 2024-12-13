package cz.zlehcito.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class WebSocketManager(private val url: String) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    private val messageHandlers: MutableMap<String, (JSONObject) -> Unit> = mutableMapOf()

    fun connect() {
        if (webSocket == null) {
            val request = Request.Builder().url(url).build()
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleIncomingMessage(text)
                }
            })
        }
    }

    // Register a handler for a specific message type
    fun registerHandler(type: String, handler: (JSONObject) -> Unit) {
        messageHandlers[type] = handler
    }

    // Remove a handler for a specific message type
    fun unregisterHandler(type: String) {
        messageHandlers.remove(type)
    }

    // Process incoming messages
    private fun handleIncomingMessage(message: String) {
        val json = JSONObject(message)
        val type = json.optString("type", "")
        messageHandlers[type]?.invoke(json)
    }

    fun sendMessage(message: JSONObject) {
        webSocket?.send(message.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        client.dispatcher.executorService.shutdown()
    }
}