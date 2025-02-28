package cz.zlehcito.model.modelHandlers

import com.google.gson.Gson
import cz.zlehcito.model.entities.EndGameResponse
import cz.zlehcito.model.entities.GameSetupResponse
import cz.zlehcito.model.entities.GameSetupState
import cz.zlehcito.model.entities.NewTermResponse
import cz.zlehcito.model.entities.RacePlayerResult
import cz.zlehcito.model.entities.StartRaceRoundResponse
import cz.zlehcito.model.entities.SubmitAnswerResponse
import cz.zlehcito.model.entities.TermDefinitionPair
import cz.zlehcito.network.WebSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

object GamePageModel {
    private var _idGame = 0
    private var _idUser = ""

    private const val COUNTDOWN_SECONDS = 3
    private val countdownScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var currentRound = 1 as Number

    private val _gameSetupState = MutableStateFlow<GameSetupState?>(null)
    val gameSetupState: StateFlow<GameSetupState?> get() = _gameSetupState
    private val _playerFinalResults = MutableStateFlow<List<RacePlayerResult>>(emptyList())
    val playerFinalResults: StateFlow<List<RacePlayerResult>> get() = _playerFinalResults
    private val _showResults = MutableStateFlow(false)
    val showResults: StateFlow<Boolean> get() = _showResults
    private val _secondsOfCountdown = MutableStateFlow(0)
    val secondsOfCountdown: StateFlow<Int> get() = _secondsOfCountdown
    private val _termDefinitionPairsQueueThisRound =
        MutableStateFlow<List<TermDefinitionPair>>(emptyList())
    val termDefinitionPairsQueueThisRound: StateFlow<List<TermDefinitionPair>> get() = _termDefinitionPairsQueueThisRound
    private val _mistakeDictionary = MutableStateFlow<Map<String, Int>>(emptyMap())
    val mistakeDictionary: StateFlow<Map<String, Int>> = _mistakeDictionary
    private val _currentTerm = MutableStateFlow("")
    val currentTerm: StateFlow<String> = _currentTerm
    private val _currentDefinition = MutableStateFlow("")
    val currentDefinition: StateFlow<String> = _currentDefinition
    private val _lastOneWasCorrect = MutableStateFlow(true)
    val lastOneWasCorrect: StateFlow<Boolean> = _lastOneWasCorrect

    fun initializeModel(idGame: Int, idUser: String) {
        _idGame = idGame
        _idUser = idUser

        WebSocketManager.registerHandler("GET_GAME") { json ->
            _gameSetupState.value = parseGameSetupStateJson(json.toString())
        }

        WebSocketManager.registerHandler("RACE_ROUND_START") { json ->
            currentRound = parseStartRaceRoundJson(json.toString())
            setupTermDefinitionQueueForThisRound(currentRound.toInt())
            startCountdown()
            sendRaceNewTermRequest()
        }

        WebSocketManager.registerHandler("RACE_SUBMIT_ANSWER") { json ->
            val submitAnswerResponse = parseSubmitAnswerJson(json.toString())
            _currentTerm.value = submitAnswerResponse?.termDefinitionPair?.term ?: ""
            _lastOneWasCorrect.value = submitAnswerResponse?.answerCorrect ?: false
            if (!_lastOneWasCorrect.value) {
                _currentDefinition.value =
                    submitAnswerResponse?.termDefinitionPair?.definition ?: ""
                addMistake(_currentTerm.value, _currentDefinition.value)
            }
        }

        WebSocketManager.registerHandler("RACE_NEW_TERM") { json ->
            _currentTerm.value = parseNewTermJson(json.toString())
        }

        WebSocketManager.registerHandler("RACE_END") { json ->
            _playerFinalResults.value = parseEndGameResultsJson(json.toString())
            _showResults.value = true
        }

        _showResults.value = false
        _mistakeDictionary.value = emptyMap()
        _currentTerm.value = ""
        _currentDefinition.value = ""
        _lastOneWasCorrect.value = true
        sendSubscriptionPutGameRunningRequest()
        sendGetGameRequest()
        startCountdown()
        sendRaceNewTermRequest()
    }

    private fun sendSubscriptionPutGameRunningRequest() {
        val sendSubscriptionPutRequest = JSONObject().apply {
            put("${'$'}type", "SUBSCRIPTION_PUT")
            put("webSocketSubscriptionPut", JSONObject().apply {
                put("idGame", _idGame)
                put("subscriptionType", "GameRunning")
            })
        }
        WebSocketManager.sendMessage(sendSubscriptionPutRequest)
    }

    private fun startCountdown() {
        _secondsOfCountdown.value = COUNTDOWN_SECONDS
        countdownScope.launch {
            repeat(COUNTDOWN_SECONDS) { i ->
                delay(1000) // Wait 1 second
                _secondsOfCountdown.value = COUNTDOWN_SECONDS - i - 1
            }
        }
    }

    private fun setupTermDefinitionQueueForThisRound(currentRound: Int) {
        // Get the total number of rounds from the game setup state
        if (currentRound < 0) return
        val totalRounds = _gameSetupState.value?.roundCount ?: return
        val termsAndDefinitions = _gameSetupState.value?.termDefinitionPairs ?: return

        // Calculate the round size and the extra items
        val roundSize = termsAndDefinitions.size / totalRounds
        val extraItems = termsAndDefinitions.size % totalRounds

        // Calculate the start and end indices for the current round
        val startIdx = (currentRound - 1) * roundSize + minOf(currentRound - 1, extraItems)
        val endIdx = minOf(
            currentRound * roundSize + minOf(currentRound, extraItems),
            termsAndDefinitions.size
        )

        // Extract the term-definition pairs for this round
        val newList = termsAndDefinitions.subList(startIdx, endIdx).toMutableList()

        // Shuffle the list
        newList.shuffle()

        // Update the MutableStateFlow with the new list
        _termDefinitionPairsQueueThisRound.value = newList
    }

    private fun addMistake(term: String, definition: String) {
        val key = "$term $ $definition |"
        _mistakeDictionary.value = _mistakeDictionary.value.toMutableMap().apply {
            put(key, getOrDefault(key, 0) + 1)
        }
    }

    fun checkDefinitionCorrectness() {
        sendRaceSubmitAnswer(currentTerm.value, currentDefinition.value)
        _currentDefinition.value = ""
    }

    fun setCurrentDefinition(definition: String) {
        _currentDefinition.value = definition
    }

    fun pairConnected(term: String, definition: String): Boolean {
        val currentQueue = _termDefinitionPairsQueueThisRound.value.toMutableList()
        val pairToCheck = TermDefinitionPair(term, definition)

        val isMatch = currentQueue.contains(pairToCheck)

        if (isMatch) {
            // Remove the correctly matched pair from the queue
            currentQueue.remove(pairToCheck)
            _termDefinitionPairsQueueThisRound.value = currentQueue
        } else {
            // Add the incorrectly connected pair to the end of the queue
            val correctPair = currentQueue.find { t -> t.term == pairToCheck.term }
            pairToCheck.definition = correctPair!!.definition

            currentQueue.add(pairToCheck)
            _termDefinitionPairsQueueThisRound.value = currentQueue
        }

        // Send updated game data to the server
        sendRaceSubmitAnswer(term, definition)

        return isMatch
    }

    private fun sendRaceSubmitAnswer(term: String, definition: String) {
        val personalGameDataRequest = JSONObject().apply {
            put("${'$'}type", "RACE_SUBMIT_ANSWER")
            put("gameManipulationKey", JSONObject().apply {
                put("IdGame", _idGame)
                put("IdUser", _idUser)
            })
            put("answer", JSONObject().apply {
                put("term", term)
                put("definition", definition)
            })
        }

        // Send the WebSocket message
        WebSocketManager.sendMessage(personalGameDataRequest)
    }

    private fun sendRaceNewTermRequest() {
        val personalGameDataRequest = JSONObject().apply {
            put("${'$'}type", "RACE_NEW_TERM")
            put("gameManipulationKey", JSONObject().apply {
                put("IdGame", _idGame)
                put("IdUser", _idUser)
            })
        }

        // Send the WebSocket message
        WebSocketManager.sendMessage(personalGameDataRequest)
    }

    private fun sendGetGameRequest() {
        val getGameRequest = JSONObject().apply {
            put("${'$'}type", "GET_GAME")
            put("idGame", _idGame)
        }
        WebSocketManager.sendMessage(getGameRequest)
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

    private fun parseStartRaceRoundJson(response: String): Number {
        return try {
            val gson = Gson()
            val startRaceRoundResponse = gson.fromJson(response, StartRaceRoundResponse::class.java)
            startRaceRoundResponse.raceGameInterRoundState.currentRound
        } catch (e: Exception) {
            0
        }
    }

    private fun parseEndGameResultsJson(response: String): List<RacePlayerResult> {
        return try {
            val gson = Gson()
            val endGameResponse = gson.fromJson(response, EndGameResponse::class.java)
            endGameResponse.racePlayerResults
        } catch (e: Exception) {
            playerFinalResults.value
        }
    }

    private fun parseSubmitAnswerJson(response: String): SubmitAnswerResponse? {
        return try {
            val gson = Gson()
            val submitAnswerResponse = gson.fromJson(response, SubmitAnswerResponse::class.java)
            submitAnswerResponse
        } catch (e: Exception) {
            null
        }
    }

    private fun parseNewTermJson(response: String): String {
        return try {
            val gson = Gson()
            val newTermResponse = gson.fromJson(response, NewTermResponse::class.java)
            newTermResponse.term
        } catch (e: Exception) {
            ""
        }
    }
}