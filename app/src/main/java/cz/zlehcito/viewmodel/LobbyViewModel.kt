package cz.zlehcito.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import cz.zlehcito.model.LobbyGameForList
import cz.zlehcito.model.GetLobbyGamesResponse
import cz.zlehcito.network.WebSocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

// LobbyViewModel manages the state and logic for the lobby screen.
// It handles fetching the list of available games, filtering, and WebSocket events.
class LobbyViewModel : ViewModel() {
    // Holds the list of games in the lobby
    private val _gamesList = MutableStateFlow<List<LobbyGameForList>>(emptyList())
    val gamesList: StateFlow<List<LobbyGameForList>> get() = _gamesList

    // Registers WebSocket event handler for receiving the games list
    init {
        WebSocketManager.registerHandler("GET_GAMES") { json ->
            viewModelScope.launch(Dispatchers.Default) {
                val games = parseGamesListJson(json.toString())
                _gamesList.value = games
            }
        }
    }

    // Sends a subscription request to receive lobby updates
    fun sendSubscriptionPutLobbyRequest() {
        val joinRequest = JSONObject().apply {
            put("$" + "type", "SUBSCRIPTION_PUT")
            put("webSocketSubscriptionPut", JSONObject().apply {
                put("idGameString", "")
                put("subscriptionType", "Lobby")
            })
        }
        WebSocketManager.sendMessage(joinRequest)
    }

    // Requests the list of games from the server
    fun sendGetGamesRequest() {
        val json = JSONObject().apply { put("$" + "type", "GET_GAMES") }
        WebSocketManager.sendMessage(json)
    }

    // Parses the games list from a JSON response
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

    // Returns a filtered list of games based on the query and game type
    fun getFilteredGames(query: String): StateFlow<List<LobbyGameForList>> {
        return gamesList
            .map { list ->
                list.filter {
                    it.name.contains(query, ignoreCase = true) && it.gameType == "Race"
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )
    }
}
