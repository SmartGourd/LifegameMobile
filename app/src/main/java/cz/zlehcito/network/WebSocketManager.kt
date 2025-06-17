package cz.zlehcito.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import android.util.Log
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

//import java.security.cert.X509Certificate
//import javax.net.ssl.*

object WebSocketManager {
    private const val URL = "wss://zlehcito.cz/ws"
    private val client = OkHttpClient()

    //private const val URL = "wss://10.0.2.2:7249/ws"
    //private val client = getUnsafeOkHttpClient()

    private var webSocket: WebSocket? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnectedFlow: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun isConnected(): Boolean = _isConnected.value

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
        if (webSocket == null && !_isConnected.value) { // Connect only if not already connected or trying
            val request = Request.Builder().url(URL).build()
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    _isConnected.value = true
                    Log.i("WebSocketManager", "Connection Opened")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleIncomingMessage(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.i("WebSocketManager", "Connection Closing: $code / $reason")
                    _isConnected.value = false
                    this@WebSocketManager.webSocket = null
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e("WebSocketManager", "Connection Failure: ${'$'}{t.message}", t)
                    _isConnected.value = false
                    this@WebSocketManager.webSocket = null
                    // Optionally, implement retry logic here or notify listeners
                }
            })
        }
    }

    // Register a handler for a specific message type
    fun registerHandler(type: String, handler: (JSONObject) -> Unit) {
        messageHandlers[type] = handler
    }

    // Process incoming messages
    private fun handleIncomingMessage(message: String) {
        val json = JSONObject(message)
        val type = json.optString("${'$'}type", "")
        messageHandlers[type]?.invoke(json)
    }

    fun sendMessage(message: JSONObject) {
        if (!isConnected()) { // Check connection status using the new method
            Log.w("WebSocketManager", "WebSocket is not connected. Attempting to connect...")
            connect() // Attempt to connect
            // Delay sending the message to give time for the connection to establish
            Handler(Looper.getMainLooper()).postDelayed({
                if (isConnected()) {
                    webSocket?.send(message.toString())
                    Log.d("WebSocketManager", "Message sent after reconnecting.")
                } else {
                    Log.e("WebSocketManager", "Failed to send message. WebSocket still not connected after attempting to reconnect.")
                }
            }, 2000) // Increased delay to 2 seconds
        } else {
            webSocket?.send(message.toString())
            Log.d("WebSocketManager", "Message sent successfully.")
        }
    }


}