package cz.zlehcito.model.modelHandlers

import com.google.gson.Gson
import cz.zlehcito.model.appState.AppState
import cz.zlehcito.model.dtos.GameKey
import cz.zlehcito.model.dtos.GameSetupState
import cz.zlehcito.model.dtos.GameSetupResponse
import cz.zlehcito.model.dtos.JoinGameResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

class GameSetupModelHandler(
    private val appState: AppState,
    private val navigateToPage: (String) -> Unit,
)  {
    private val _gameSetupState = MutableStateFlow<GameSetupState?>(null)
    val gameSetupState: StateFlow<GameSetupState?> get() = _gameSetupState
    private val _defaultKeyValue = GameKey(appState.idGame, "", "Invalid")
    private val _gameKey = MutableStateFlow<GameKey>(_defaultKeyValue)
    val gameKey: StateFlow<GameKey> get() = _gameKey

    public fun initializeModel() {
        appState.webSocketManager.registerHandler("GET_GAME") { json ->
            _gameSetupState.value = parseGameSetupStateJson(json.toString())
        }

        appState.webSocketManager.registerHandler("JOIN_GAME") { json ->
            _gameKey.value = parseJoinGameJson(json.toString())
            appState.idUser = _gameKey.value.idUser
        }

        appState.webSocketManager.registerHandler("LEAVE_GAME") {
            _gameKey.value = _defaultKeyValue
            _gameSetupState.value = null
        }

        appState.webSocketManager.registerHandler("RACE_START_GAME") {
            _gameKey.value = _defaultKeyValue
            _gameSetupState.value = null
            navigateToPage("GamePage")
        }


        sendSubscriptionPutGameSetupRequest()
        sendGetGameRequest()
    }

    public fun sendSubscriptionPutGameSetupRequest() {
        val sendSubscriptionPutRequest = JSONObject().apply {
            put("${'$'}type", "SUBSCRIPTION_PUT")
            put("webSocketSubscriptionPut", JSONObject().apply {
                put("idGame", appState.idGame)
                put("subscriptionType", "GameSetup")
            })
        }
        appState.webSocketManager.sendMessage(sendSubscriptionPutRequest)
    }

    public fun sendGetGameRequest() {
        val getGameRequest = JSONObject().apply {
            put("${'$'}type", "GET_GAME")
            put("idGame", appState.idGame)
        }
        appState.webSocketManager.sendMessage(getGameRequest)
    }

    public fun sendJoinGameRequest(playerName: String) {
        val joinRequest = JSONObject().apply {
            put("${'$'}type", "JOIN_GAME")
            put("gameJoinDto", JSONObject().apply {
                put("idGame", appState.idGame)
                put("PlayerName", playerName)
            })
        }
        appState.webSocketManager.sendMessage(joinRequest)
    }

    public fun sendLeaveGameRequest(idUser: String) {
        val leaveRequest = JSONObject().apply {
            put("${'$'}type", "LEAVE_GAME")
            put("gameManipulationKey", JSONObject().apply {
                put("IdGame", appState.idGame)
                put("IdUser", idUser)
            })
        }
        appState.webSocketManager.sendMessage(leaveRequest)
    }

    private fun parseGameSetupStateJson(response: String): GameSetupState? {
        return try {
            val gson = Gson()
            val gameResponse = gson.fromJson(response, GameSetupResponse::class.java)
            gameResponse.game
        } catch (e: Exception) {
            gameSetupState.value
        }
    }

    private fun parseJoinGameJson(response: String): GameKey {
        return try {
            val gson = Gson()
            val joinGameResponse = gson.fromJson(response, JoinGameResponse::class.java)
            joinGameResponse.gameKeyPlayer
        } catch (e: Exception) {
            _defaultKeyValue
        }
    }
}
