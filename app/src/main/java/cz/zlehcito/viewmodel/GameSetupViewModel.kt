package cz.zlehcito.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import cz.zlehcito.model.entities.GameKey
import cz.zlehcito.model.entities.GameSetupResponse
import cz.zlehcito.model.entities.GameSetupState
import cz.zlehcito.model.entities.JoinGameResponse
import cz.zlehcito.network.WebSocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

// Event wrapper for navigation
data class NavigationEvent<T>(val data: T)

class GameSetupViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _idGame: Int = savedStateHandle.get<Int>("gameId") ?: 0

    private val _gameSetupState = MutableStateFlow<GameSetupState?>(null)
    val gameSetupState: StateFlow<GameSetupState?> = _gameSetupState.asStateFlow()

    private val _defaultKeyValue = GameKey(_idGame, "", "Invalid")
    private val _gameKey = MutableStateFlow(_defaultKeyValue)
    val gameKey: StateFlow<GameKey> = _gameKey.asStateFlow()

    private val _idUser = MutableStateFlow("") // This will be updated upon joining

    // For navigation to game page
    private val _navigateToGameAction = MutableStateFlow<NavigationEvent<Pair<Int, String>>?>(null)
    val navigateToGameAction: StateFlow<NavigationEvent<Pair<Int, String>>?> = _navigateToGameAction.asStateFlow()

    init {
        WebSocketManager.registerHandler("GET_GAME") { json ->
            _gameSetupState.value = parseGameSetupStateJson(json.toString())
        }

        WebSocketManager.registerHandler("JOIN_GAME") { json ->
            val joinedGameKey = parseJoinGameJson(json.toString())
            _gameKey.value = joinedGameKey
            _idUser.value = joinedGameKey.idUser
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

        if (WebSocketManager.isConnected()) {
            sendSubscriptionPutGameSetupRequest()
            sendGetGameRequest()
        }
    }

    private fun sendSubscriptionPutGameSetupRequest() {
        val sendSubscriptionPutRequest = JSONObject().apply {
            put("${'$'}type", "SUBSCRIPTION_PUT")
            put("webSocketSubscriptionPut", JSONObject().apply {
                put("idGame", _idGame)
                put("subscriptionType", "GameSetup")
            })
        }
        WebSocketManager.sendMessage(sendSubscriptionPutRequest)
    }

    private fun sendGetGameRequest() {
        val getGameRequest = JSONObject().apply {
            put("${'$'}type", "GET_GAME")
            put("idGame", _idGame)
        }
        WebSocketManager.sendMessage(getGameRequest)
    }

    fun sendJoinGameRequest(playerName: String) {
        val joinRequest = JSONObject().apply {
            put("${'$'}type", "JOIN_GAME")
            put("gameJoinDto", JSONObject().apply {
                put("idGame", _idGame)
                put("PlayerName", playerName)
            })
        }
        WebSocketManager.sendMessage(joinRequest)
    }

    fun sendLeaveGameRequest() { // idUser is now internal to ViewModel via _gameKey
        if (_gameKey.value.idUser.isNotEmpty() && _gameKey.value.keyType != "Invalid") {
            val leaveRequest = JSONObject().apply {
                put("${'$'}type", "LEAVE_GAME")
                put("gameManipulationKey", JSONObject().apply {
                    put("IdGame", _idGame)
                    put("IdUser", _gameKey.value.idUser)
                })
            }
            WebSocketManager.sendMessage(leaveRequest)
        }
    }
    
    fun consumeNavigationEvent() {
        _navigateToGameAction.value = null
    }

    private fun parseGameSetupStateJson(response: String): GameSetupState? {
        return try {
            val gson = Gson()
            val gameResponse = gson.fromJson(response, GameSetupResponse::class.java)
            gameResponse.game
        } catch (e: Exception) {
            // Log error
            _gameSetupState.value // Keep existing state on error
        }
    }

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
