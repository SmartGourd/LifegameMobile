package cz.zlehcito.model.modelHandlers

import com.google.gson.Gson
import cz.zlehcito.model.entities.Game
import cz.zlehcito.model.entities.GameResponse
import cz.zlehcito.network.WebSocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

class LobbyModel(
    private val navigateToGameSetupPage: (Int) -> Unit,
) {
    private val _gamesList = MutableStateFlow<List<Game>>(emptyList())
    val gamesList: StateFlow<List<Game>> get() = _gamesList // Expose immutable variant of the gamesList

    init {
        WebSocketManager.registerHandler("GET_GAMES") { json ->
            _gamesList.value = parseGamesListJson(json.toString())
        }

        sendSubscriptionPutLobbyRequest()
        sendGetGamesRequest()
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
            emptyList() // Return empty list on parsing error
        }
    }

    fun navigateToGameSetup(idGame: Int) {
        navigateToGameSetupPage(idGame)
    }
}