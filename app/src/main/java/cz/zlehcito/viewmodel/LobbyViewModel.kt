package cz.zlehcito.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import cz.zlehcito.model.LobbyGameForList
import cz.zlehcito.model.GetLobbyGamesResponse
import cz.zlehcito.network.WebSocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

class LobbyViewModel : ViewModel() {

    private val _gamesList = MutableStateFlow<List<LobbyGameForList>>(emptyList())
    val gamesList: StateFlow<List<LobbyGameForList>> get() = _gamesList

    init {
        WebSocketManager.registerHandler("GET_GAMES") { json ->
            _gamesList.value = parseGamesListJson(json.toString())
        }
    }

    fun sendSubscriptionPutLobbyRequest() {
        val joinRequest = JSONObject().apply {
            put("${'$'}type", "SUBSCRIPTION_PUT")
            put("webSocketSubscriptionPut", JSONObject().apply {
                put("idGameString", "")
                put("subscriptionType", "Lobby")
            })
        }
        WebSocketManager.sendMessage(joinRequest)
    }

    fun sendGetGamesRequest() {
        val json = JSONObject().apply { put("${'$'}type", "GET_GAMES") }
        WebSocketManager.sendMessage(json)
    }

    private fun parseGamesListJson(response: String): List<LobbyGameForList> {
        return try {
            val gson = Gson()
            val gameResponse = gson.fromJson(response, GetLobbyGamesResponse::class.java)
            gameResponse.games
        } catch (e: Exception) {
            Log.e("LobbyViewModel", "Error parsing games list", e)
            emptyList() // Return empty list on parsing error
        }
    }
}
