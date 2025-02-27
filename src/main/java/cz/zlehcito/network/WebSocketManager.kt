package cz.zlehcito.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import android.util.Log
import android.os.Handler
import android.os.Looper

//import okhttp3.Response
//import java.security.cert.X509Certificate
//import javax.net.ssl.*

object WebSocketManager {
    private const val URL = "wss://zlehcito.cz/ws"
    private val client = OkHttpClient()

    //private val url = "wss://10.0.2.2:7249/ws"
    //private val client = getUnsafeOkHttpClient()

    private var webSocket: WebSocket? = null

    private val messageHandlers: MutableMap<String, (JSONObject) -> Unit> = mutableMapOf()
    /* Used for development with connection to localhost websocket server
    fun getUnsafeOkHttpClient(): OkHttpClient {
        try {
            // Create a trust manager that does not validate certificate chains
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            // Install the all-trusting trust manager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            // Build an OkHttpClient that trusts all certificates
            return OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
    */

    fun connect() {
        if (webSocket == null) {
            val request = Request.Builder().url(URL).build()
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
        val type = json.optString("${'$'}type", "")
        messageHandlers[type]?.invoke(json)
    }

    fun sendMessage(message: JSONObject) {
        // If the webSocket is null, try connecting and then send after a delay
        if (webSocket == null) {
            Log.w("WebSocketManager", "WebSocket is null. Reconnecting...")
            connect()
            Handler(Looper.getMainLooper()).postDelayed({
                webSocket?.let { ws ->
                    if (!ws.send(message.toString())) {
                        Log.e("WebSocketManager", "Failed to send message after reconnecting")
                    } else {
                        Log.d("WebSocketManager", "Message sent successfully after reconnecting")
                    }
                } ?: Log.e("WebSocketManager", "WebSocket still null after reconnecting")
            }, 1000) // Delay of 1 second (adjust if needed)
        } else {
            // Attempt to send the message
            if (!webSocket!!.send(message.toString())) {
                Log.w("WebSocketManager", "Send returned false. Reconnecting...")
                webSocket = null
                connect()
                Handler(Looper.getMainLooper()).postDelayed({
                    webSocket?.let { ws ->
                        if (!ws.send(message.toString())) {
                            Log.e("WebSocketManager", "Failed to send message after reconnecting")
                        } else {
                            Log.d("WebSocketManager", "Message sent successfully after reconnecting")
                        }
                    } ?: Log.e("WebSocketManager", "WebSocket still null after reconnecting")
                }, 1000)
            } else {
                Log.d("WebSocketManager", "Message sent successfully")
            }
        }
    }


    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        client.dispatcher.executorService.shutdown()
    }
}