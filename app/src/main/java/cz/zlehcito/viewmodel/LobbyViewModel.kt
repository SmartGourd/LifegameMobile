package cz.zlehcito.viewmodel

import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import cz.zlehcito.model.entities.Game
import cz.zlehcito.model.entities.GameResponse
import cz.zlehcito.network.WebSocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

class LobbyViewModel : ViewModel() {

    private val _gamesList = MutableStateFlow<List<Game>>(emptyList())
    val gamesList: StateFlow<List<Game>> get() = _gamesList

    init {
        WebSocketManager.registerHandler("GET_GAMES") { json ->
            _gamesList.value = parseGamesListJson(json.toString())
        }
        // Ensure WebSocketManager is connected before sending messages
        // This might be better handled by ensuring connect() is called reliably at app start
        // or by having WebSocketManager manage its connection state internally and queue messages.
        if (WebSocketManager.isConnected()) { // Assuming WebSocketManager has an isConnected method
            sendSubscriptionPutLobbyRequest()
            sendGetGamesRequest()
        } else {
            // Handle case where WebSocket is not connected, perhaps log or retry
            // For now, we assume connect() in MainActivity or Application class handles initial connection.
        }
    }

    private fun sendSubscriptionPutLobbyRequest() {
        val joinRequest = JSONObject().apply {
            put("${'$'}type", "SUBSCRIPTION_PUT")
            put("webSocketSubscriptionPut", JSONObject().apply {
                put("idGame", 0)
                put("subscriptionType", "Lobby")
            })
        }
        WebSocketManager.sendMessage(joinRequest)
    }

    private fun sendGetGamesRequest() {
        val json = JSONObject().apply { put("${'$'}type", "GET_GAMES") }
        WebSocketManager.sendMessage(json)
    }

    private fun parseGamesListJson(response: String): List<Game> {
        return try {
            val gson = Gson()
            val gameResponse = gson.fromJson(response, GameResponse::class.java)
            gameResponse.games
        } catch (e: Exception) {
            // Log error e.g. Log.e("LobbyViewModel", "Error parsing games list", e)
            emptyList() // Return empty list on parsing error
        }
    }

    // The navigateToGameSetup function is removed.
    // Navigation will be handled by the Composable using NavController.
}
