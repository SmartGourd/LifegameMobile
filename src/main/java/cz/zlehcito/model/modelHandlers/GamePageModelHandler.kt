package cz.zlehcito.model.modelHandlers

import com.google.gson.Gson
import cz.zlehcito.model.appState.AppState
import cz.zlehcito.model.dtos.EndGameResponse
import cz.zlehcito.model.dtos.GameSetupResponse
import cz.zlehcito.model.dtos.GameSetupState
import cz.zlehcito.model.dtos.PersonalGameData
import cz.zlehcito.model.dtos.RacePlayerResult
import cz.zlehcito.model.dtos.StartRaceRoundResponse
import cz.zlehcito.model.dtos.TermDefinitionPair
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject

class GamePageModelHandler (
    private val appState: AppState,
) {
    private val countdownSeconds = 3
    private val countdownScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var currentRound = 0 as Number

    private val _gameSetupState = MutableStateFlow<GameSetupState?>(null)
    val gameSetupState: StateFlow<GameSetupState?> get() = _gameSetupState
    private val _personalGameData = MutableStateFlow<PersonalGameData?>(null)
    val personalGameData: StateFlow<PersonalGameData?> get() = _personalGameData
    private val _playerFinalResults = MutableStateFlow<List<RacePlayerResult>>(emptyList())
    val playerFinalResults: StateFlow<List<RacePlayerResult>> get() = _playerFinalResults
    private val _showResults = MutableStateFlow<Boolean>(false)
    val showResults: StateFlow<Boolean> get() = _showResults
    private val _secondsOfCountdown = MutableStateFlow<Int>(0)
    val secondsOfCountdown: StateFlow<Int> get() = _secondsOfCountdown
    private val _termDefinitionPairsQueueThisRound = MutableStateFlow<List<TermDefinitionPair>>(emptyList())
    val termDefinitionPairsQueueThisRound: StateFlow<List<TermDefinitionPair>> get() = _termDefinitionPairsQueueThisRound
    private val _mistakeDictionary = MutableStateFlow<Map<String, Int>>(emptyMap())
    val mistakeDictionary: StateFlow<Map<String, Int>> = _mistakeDictionary


    init {
        appState.webSocketManager.registerHandler("GET_GAME") { json ->
            _gameSetupState.value = parseGameSetupStateJson(json.toString())
        }

        appState.webSocketManager.registerHandler("START_RACE_ROUND") { json ->
            currentRound = parseStartRaceRoundJson(json.toString())
            startCountdown()
        }

        appState.webSocketManager.registerHandler("END_RACE") { json ->
            _playerFinalResults.value = parseEndGameResultsJson(json.toString())
            _showResults.value = true
        }

        sendSubscriptionPutGameRunningRequest()
        sendGetGameRequest()
        startCountdown()
    }

    private fun sendSubscriptionPutGameRunningRequest() {
        val sendSubscriptionPutRequest = JSONObject().apply {
            put("type", "SUBSCRIPTION_PUT")
            put("data", JSONObject().apply {
                put("idGame", appState.idGame)
                put("subscriptionType", "GameRunning")
            })
        }
        appState.webSocketManager.sendMessage(sendSubscriptionPutRequest)
    }

    private fun startCountdown() {
        _secondsOfCountdown.value = countdownSeconds
        countdownScope.launch {
            repeat(countdownSeconds) { i ->
                delay(1000) // Wait 1 second
                _secondsOfCountdown.value = countdownSeconds - i - 1
            }
        }
    }

    fun cleanup() {
        countdownScope.cancel()
    }

    fun addMistake(key: String) {
        _mistakeDictionary.value = _mistakeDictionary.value.toMutableMap().apply {
            put(key, getOrDefault(key, 0) + 1)
        }
    }

    public fun sendRacePutRequest() {
        val personalGameDataRequest = JSONObject().apply {
            put("type", "RACE_PUT")
            put("data", personalGameData)
        }
        appState.webSocketManager.sendMessage(personalGameDataRequest)
    }

    public fun sendGetGameRequest() {
        val getGameRequest = JSONObject().apply {
            put("type", "GET_GAME")
            put("data", appState.idGame)
        }
        appState.webSocketManager.sendMessage(getGameRequest)
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

    private fun parseStartRaceRoundJson(response: String): Number {
        return try {
            val gson = Gson()
            val startRaceRoundResponse = gson.fromJson(response, StartRaceRoundResponse::class.java)
            startRaceRoundResponse.data.raceGameInterRoundState.currentRound
        } catch (e: Exception) {
            0
        }
    }

    private fun parseEndGameResultsJson(response: String): List<RacePlayerResult>{
        return try {
            val gson = Gson()
            val endGameResponse = gson.fromJson(response, EndGameResponse::class.java)
            endGameResponse.data.playerResult
        } catch (e: Exception) {
            playerFinalResults.value
        }
    }
}