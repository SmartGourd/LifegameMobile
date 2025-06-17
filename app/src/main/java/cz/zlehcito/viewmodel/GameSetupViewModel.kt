package cz.zlehcito.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import cz.zlehcito.model.GameKey
import cz.zlehcito.model.GetLobbyGameResponse
import cz.zlehcito.model.LobbyGameDetail
import cz.zlehcito.model.JoinGameResponse
import cz.zlehcito.network.WebSocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

// NavigationEvent is a wrapper for navigation actions, allowing data to be passed with navigation events.
data class NavigationEvent<T>(val data: T)

// GameSetupViewModel manages the state and logic for the game setup screen.
// It handles joining/leaving games, fetching game details, and navigation events.
class GameSetupViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _idGame: String = savedStateHandle.get<String>("idGame") ?: ""

    // Holds the current game setup state (null if not loaded)
    private val _gameSetupState = MutableStateFlow<LobbyGameDetail?>(null)
    val gameSetupState: StateFlow<LobbyGameDetail?> = _gameSetupState.asStateFlow()

    // Holds the current game key (used for joining/leaving)
    private val _defaultKeyValue = GameKey(_idGame, "", "Invalid")
    private val _gameKey = MutableStateFlow(_defaultKeyValue)
    val gameKey: StateFlow<GameKey> = _gameKey.asStateFlow()

    // Holds the user ID (updated upon joining)
    private val _idUser = MutableStateFlow("")

    // Navigation event for moving to the game page
    private val _navigateToGameAction = MutableStateFlow<NavigationEvent<Pair<String, String>>?>(null)
    val navigateToGameAction: StateFlow<NavigationEvent<Pair<String, String>>?> = _navigateToGameAction.asStateFlow()

    // Registers WebSocket event handlers for game setup events
    init {
        WebSocketManager.registerHandler("GET_GAME") { json ->
            viewModelScope.launch(Dispatchers.Default) {
                val game = parseGameSetupStateJson(json.toString())
                _gameSetupState.value = game
            }
        }

        WebSocketManager.registerHandler("JOIN_GAME") { json ->
            viewModelScope.launch(Dispatchers.Default) {
                val joinedGameKey = parseJoinGameJson(json.toString())
                _gameKey.value = joinedGameKey
                _idUser.value = joinedGameKey.idUser
            }
        }

        WebSocketManager.registerHandler("LEAVE_GAME") {
            _gameKey.value = _defaultKeyValue.copy(idGame = _idGame) // Ensure idGame is current
            _gameSetupState.value = null // Or re-fetch, depending on desired behavior
        }

        WebSocketManager.registerHandler("RACE_START_GAME") {
            // Signal navigation instead of calling directly
            _navigateToGameAction.value = NavigationEvent(Pair(_idGame, _idUser.value))
            // Reset state after signaling navigation if needed
            // _gameKey.value = _defaultKeyValue.copy(idGame = _idGame)
            // _gameSetupState.value = null
        }
    }

    // Sends a subscription request to receive game setup updates
    fun sendSubscriptionPutGameSetupRequest() {
        val sendSubscriptionPutRequest = JSONObject().apply {
            put("$" + "type", "SUBSCRIPTION_PUT")
            put("webSocketSubscriptionPut", JSONObject().apply {
                put("idGameString", _idGame)
                put("subscriptionType", "Game")
            })
        }
        WebSocketManager.sendMessage(sendSubscriptionPutRequest)
    }

    // Requests the current game setup state from the server
    fun sendGetGameRequest() {
        val getGameRequest = JSONObject().apply {
            put("$" + "type", "GET_GAME")
            put("idGame", _idGame)
        }
        WebSocketManager.sendMessage(getGameRequest)
    }

    // Sends a request to join the game with the given player name
    fun sendJoinGameRequest(playerName: String) {
        val joinRequest = JSONObject().apply {
            put("$" + "type", "JOIN_GAME")
            put("gameJoinDto", JSONObject().apply {
                put("idGame", _idGame)
                put("PlayerName", playerName)
            })
        }
        WebSocketManager.sendMessage(joinRequest)
    }

    // Sends a request to leave the game (if user is joined)
    fun sendLeaveGameRequest() {
        if (_gameKey.value.idUser.isNotEmpty() && _gameKey.value.keyType != "Invalid") {
            val leaveRequest = JSONObject().apply {
                put("$" + "type", "LEAVE_GAME")
                put("gameManipulationKey", JSONObject().apply {
                    put("IdGame", _idGame)
                    put("IdUser", _gameKey.value.idUser)
                })
            }
            WebSocketManager.sendMessage(leaveRequest)
        }
    }

    // Consumes the navigation event after it has been handled
    fun consumeNavigationEvent() {
        _navigateToGameAction.value = null
    }

    // Parses the game setup state from a JSON response
    private fun parseGameSetupStateJson(response: String): LobbyGameDetail? {
        return try {
            val gson = Gson()
            val gameResponse = gson.fromJson(response, GetLobbyGameResponse::class.java)
            gameResponse.game
        } catch (e: Exception) {
            // Log error
            _gameSetupState.value // Keep existing state on error
        }
    }

    // Parses the join game response and returns the GameKey
    private fun parseJoinGameJson(response: String): GameKey {
        return try {
            val gson = Gson()
            val joinGameResponse = gson.fromJson(response, JoinGameResponse::class.java)
            joinGameResponse.gameKeyPlayer
        } catch (e: Exception) {
            // Log error
            _defaultKeyValue.copy(idGame = _idGame) // Ensure idGame is current
        }
    }
}
