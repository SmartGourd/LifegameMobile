package cz.zlehcito.model.modelHandlers

import com.google.gson.Gson
import cz.zlehcito.model.entities.GameKey
import cz.zlehcito.model.entities.GameSetupState
import cz.zlehcito.model.entities.GameSetupResponse
import cz.zlehcito.model.entities.JoinGameResponse
import cz.zlehcito.network.WebSocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

object GameSetupModel {
    private var _idGame: Int = 0

    private val _gameSetupState = MutableStateFlow<GameSetupState?>(null)
    val gameSetupState: StateFlow<GameSetupState?> get() = _gameSetupState
    private val _defaultKeyValue = GameKey(_idGame, "", "Invalid")
    private val _gameKey = MutableStateFlow(_defaultKeyValue)
    val gameKey: StateFlow<GameKey> get() = _gameKey
    private val _idUser = MutableStateFlow("")


    fun initializeModel(idGame: Int, navigateToGamePage: (Int, String) -> Unit) {
        _idGame = idGame

        WebSocketManager.registerHandler("GET_GAME") { json ->
            _gameSetupState.value = parseGameSetupStateJson(json.toString())
        }

        WebSocketManager.registerHandler("JOIN_GAME") { json ->
            _gameKey.value = parseJoinGameJson(json.toString())
            _idUser.value = _gameKey.value.idUser
        }

        WebSocketManager.registerHandler("LEAVE_GAME") {
            _gameKey.value = _defaultKeyValue
            _gameSetupState.value = null
        }

        WebSocketManager.registerHandler("RACE_START_GAME") {
            _gameKey.value = _defaultKeyValue
            _gameSetupState.value = null
            navigateToGamePage(idGame, _idUser.value)
        }


        sendSubscriptionPutGameSetupRequest()
        sendGetGameRequest()
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

    fun sendLeaveGameRequest(idUser: String) {
        val leaveRequest = JSONObject().apply {
            put("${'$'}type", "LEAVE_GAME")
            put("gameManipulationKey", JSONObject().apply {
                put("IdGame", _idGame)
                put("IdUser", idUser)
            })
        }
        WebSocketManager.sendMessage(leaveRequest)
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
