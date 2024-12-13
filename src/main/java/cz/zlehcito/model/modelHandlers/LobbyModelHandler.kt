package cz.zlehcito.model.modelHandlers

import com.google.gson.Gson
import cz.zlehcito.model.appState.AppState
import cz.zlehcito.model.dtos.Game
import cz.zlehcito.model.dtos.GameResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

class LobbyModelHandler(
    private val appState: AppState,
) {
    private val _gamesList = MutableStateFlow<List<Game>>(emptyList())
    val gamesList: StateFlow<List<Game>> get() = _gamesList // Expose immutable variant of the gamesList

    init {
        appState.webSocketManager.registerHandler("GET_GAMES") { json ->
            _gamesList.value = parseGamesListJson(json.toString())
        }

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
        appState.webSocketManager.sendMessage(joinRequest)
    }

    private fun sendGetGamesRequest() {
        val json = JSONObject().apply { put("type", "GET_GAMES") }
        appState.webSocketManager.sendMessage(json)
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