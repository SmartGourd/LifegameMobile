package cz.zlehcito.model.modelHandlers

import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import cz.zlehcito.model.dtos.GameKey
import cz.zlehcito.model.dtos.GameSetupState
import cz.zlehcito.model.dtos.GameSetupResponse
import cz.zlehcito.model.dtos.JoinGameResponse
import cz.zlehcito.network.WebSocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

class GameSetupModelHandler(
    private val webSocketManager: WebSocketManager,
    private val navigateToPage: (String, Int, Int) -> Unit,
    private val idGame: Int
)  {
    private val _gameSetupState = MutableStateFlow<GameSetupState?>(null)
    val gameSetupState: StateFlow<GameSetupState?> get() = _gameSetupState
    private val _defaultKeyValue = GameKey(idGame, "", "Invalid")
    private val _gameKey = MutableStateFlow<GameKey>(_defaultKeyValue)
    val gameKey: StateFlow<GameKey> get() = _gameKey

    init {
        webSocketManager.registerHandler("GET_GAME") { json ->
            _gameSetupState.value = parseGameSetupStateJson(json.toString())
        }

        webSocketManager.registerHandler("JOIN_GAME") { json ->
            _gameKey.value = parseJoinGameJson(json.toString())
        }

        webSocketManager.registerHandler("LEAVE_GAME") {
            _gameKey.value = _defaultKeyValue
        }

        webSocketManager.registerHandler("START_GAME") {
            navigateToPage("GamePage", idGame, 0)
        }


        sendSubscriptionPutGameSetupRequest()
        sendGetGameRequest()
    }

    public fun sendSubscriptionPutGameSetupRequest() {
        val joinRequest = JSONObject().apply {
            put("type", "SUBSCRIPTION_PUT")
            put("data", JSONObject().apply {
                put("idGame", idGame)
                put("subscriptionType", "GameSetup")
            })
        }
        webSocketManager.sendMessage(joinRequest)
    }

    public fun sendGetGameRequest() {
        val getGameRequest = JSONObject().apply {
            put("type", "GET_GAME")
            put("data", idGame)
        }
        webSocketManager.sendMessage(getGameRequest)
    }

    public fun sendJoinGameRequest(playerName: String) {
        val joinRequest = JSONObject().apply {
            put("type", "JOIN_GAME")
            put("data", JSONObject().apply {
                put("idGame", idGame)
                put("PlayerName", playerName)
            })
        }
        webSocketManager.sendMessage(joinRequest)
    }

    public fun sendLeaveGameRequest(idUser: String) {
        val leaveRequest = JSONObject().apply {
            put("type", "LEAVE_GAME")
            put("data", JSONObject().apply {
                put("IdGame", idGame)
                put("IdUser", idUser)
            })
        }
        webSocketManager.sendMessage(leaveRequest)
    }

    private fun parseGameSetupStateJson(response: String): GameSetupState? {
        return try {
            val gson = Gson()
            val gameResponse = gson.fromJson(response, GameSetupResponse::class.java)
            gameResponse.data.game
        } catch (e: Exception) {
            gameSetupState.value
        }
    }

    private fun parseJoinGameJson(response: String): GameKey {
        return try {
            val gson = Gson()
            val joinGameResponse = gson.fromJson(response, JoinGameResponse::class.java)
            joinGameResponse.data.gameKeyPlayer
        } catch (e: Exception) {
            _defaultKeyValue
        }
    }
}
