package cz.zlehcito.model.modelHandlers

import com.google.gson.Gson
import cz.zlehcito.model.dtos.Game
import cz.zlehcito.model.dtos.GameResponse
import cz.zlehcito.network.WebSocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

class LobbyModelHandler(
    private val webSocketManager: WebSocketManager
) {
    private val _gamesList = MutableStateFlow<List<Game>>(emptyList())
    val gamesList: StateFlow<List<Game>> get() = _gamesList // Expose immutable variant of the gamesList

    init {
        // Connect to WebSocket and set up listener
        webSocketManager.connect(object : okhttp3.WebSocketListener() {
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                val response = JSONObject(text)

                when (response.getString("type")) {
                    "GET_GAMES" -> {
                        _gamesList.value = parseGamesListJson(text)
                    }
                }

            }
        })

        sendSubscriptionPutGameSetupRequest()
        sendGetGamesRequest()
    }

    private fun sendSubscriptionPutGameSetupRequest() {
        val joinRequest = JSONObject().apply {
            put("type", "SUBSCRIPTION_PUT")
            put("data", JSONObject().apply {
                put("idGame", 0)
                put("subscriptionType", "Lobby")
            })
        }
        webSocketManager.sendMessage(joinRequest)
    }

    private fun sendGetGamesRequest() {
        val json = JSONObject().apply { put("type", "GET_GAMES") }
        webSocketManager.sendMessage(json)
    }

    private fun parseGamesListJson(response: String): List<Game> {
        return try {
            val gson = Gson()
            val gameResponse = gson.fromJson(response, GameResponse::class.java)
            gameResponse.data.games
        } catch (e: Exception) {
            emptyList() // Return empty list on parsing error
        }
    }
}