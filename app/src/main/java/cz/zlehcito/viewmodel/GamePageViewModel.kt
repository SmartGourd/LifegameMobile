package cz.zlehcito.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import cz.zlehcito.model.entities.*
import cz.zlehcito.network.WebSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.min

class GamePageViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _idGame: Int = savedStateHandle.get<Int>("gameId") ?: 0
    private val _idUser: String = savedStateHandle.get<String>("userId") ?: ""

    private companion object {
        private const val COUNTDOWN_SECONDS = 3
    }

    // Use viewModelScope for coroutines tied to the ViewModel's lifecycle
    private val countdownScope = CoroutineScope(SupervisorJob() + Dispatchers.Main + viewModelScope.coroutineContext)

    private var currentRoundNumber: Int = 1 // Renamed to avoid conflict with StateFlow

    private val _gameSetupState = MutableStateFlow<GameSetupState?>(null)
    val gameSetupState: StateFlow<GameSetupState?> = _gameSetupState.asStateFlow()

    private val _playerFinalResults = MutableStateFlow<List<RacePlayerResult>>(emptyList())
    val playerFinalResults: StateFlow<List<RacePlayerResult>> = _playerFinalResults.asStateFlow()

    private val _showResults = MutableStateFlow(false)
    val showResults: StateFlow<Boolean> = _showResults.asStateFlow()

    private val _secondsOfCountdown = MutableStateFlow(0)
    val secondsOfCountdown: StateFlow<Int> = _secondsOfCountdown.asStateFlow()

    private val _termDefinitionPairsQueueThisRound =
        MutableStateFlow<List<TermDefinitionPair>>(emptyList())
    val termDefinitionPairsQueueThisRound: StateFlow<List<TermDefinitionPair>> = _termDefinitionPairsQueueThisRound.asStateFlow()

    private val _mistakeDictionary = MutableStateFlow<Map<String, Int>>(emptyMap())
    val mistakeDictionary: StateFlow<Map<String, Int>> = _mistakeDictionary.asStateFlow()

    private val _currentTerm = MutableStateFlow("")
    val currentTerm: StateFlow<String> = _currentTerm.asStateFlow()

    private val _currentDefinition = MutableStateFlow("")
    val currentDefinition: StateFlow<String> = _currentDefinition.asStateFlow()

    private val _lastOneWasCorrect = MutableStateFlow(true)
    val lastOneWasCorrect: StateFlow<Boolean> = _lastOneWasCorrect.asStateFlow()

    private val _navigateToLobby = MutableStateFlow<Boolean>(false)
    val navigateToLobby: StateFlow<Boolean> = _navigateToLobby.asStateFlow()

    init {
        registerWebSocketHandlers()
        resetGameStateAndStart()
    }

    fun onNavigateToLobbyClicked() {
        _navigateToLobby.value = true
    }

    fun onNavigationDone() {
        _navigateToLobby.value = false // Reset after navigation
    }

    private fun registerWebSocketHandlers() {
        WebSocketManager.registerHandler("GET_GAME") { json ->
            _gameSetupState.value = parseGameSetupStateJson(json.toString())
        }

        WebSocketManager.registerHandler("RACE_ROUND_START") { json ->
            currentRoundNumber = parseStartRaceRoundJson(json.toString()).toInt()
            setupTermDefinitionQueueForThisRound(currentRoundNumber)
            startCountdown()
            // sendRaceNewTermRequest() // This might be called after countdown or based on game logic
        }

        WebSocketManager.registerHandler("RACE_SUBMIT_ANSWER") { json ->
            val submitAnswerResponse = parseSubmitAnswerJson(json.toString())
            _currentTerm.value = submitAnswerResponse?.termDefinitionPair?.term ?: ""
            _lastOneWasCorrect.value = submitAnswerResponse?.answerCorrect ?: false
            if (!(_lastOneWasCorrect.value)) {
                _currentDefinition.value =
                    submitAnswerResponse?.termDefinitionPair?.definition ?: ""
                addMistake(_currentTerm.value, _currentDefinition.value) // Pass current definition if it was wrong
            }
            // If end of round, RACE_ROUND_START or RACE_END should be triggered by server.
            // If not end of round and answer was correct, server might send RACE_NEW_TERM or client requests it.
            if (submitAnswerResponse?.answerCorrect == true && submitAnswerResponse.endOfRound == false) {
                sendRaceNewTermRequest() // Request new term if correct and not end of round
            }
        }

        WebSocketManager.registerHandler("RACE_NEW_TERM") { json ->
            _currentTerm.value = parseNewTermJson(json.toString())
            _currentDefinition.value = "" // Clear previous definition
            _lastOneWasCorrect.value = true // Assume correct until next submission
        }

        WebSocketManager.registerHandler("RACE_END") { json ->
            _playerFinalResults.value = parseEndGameResultsJson(json.toString())
            _showResults.value = true
        }
    }

    private fun resetGameStateAndStart() {
        _showResults.value = false
        _mistakeDictionary.value = emptyMap()
        _currentTerm.value = ""
        _currentDefinition.value = ""
        _lastOneWasCorrect.value = true
        currentRoundNumber = 1 // Reset round number

        if (WebSocketManager.isConnected()) {
            sendSubscriptionPutGameRunningRequest()
            sendGetGameRequest() // To get initial game setup like terms
            // Initial RACE_ROUND_START should be sent by server after subscription or game start signal
            // Or, if client needs to initiate the first round start signal:
            // sendRaceRoundStartRequest() // Or similar, if applicable
        } else {
            // Handle WebSocket not connected
        }
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
        viewModelScope.launch { // Use viewModelScope for lifecycle-aware coroutines
            repeat(COUNTDOWN_SECONDS) { i ->
                delay(1000) // Wait 1 second
                _secondsOfCountdown.value = COUNTDOWN_SECONDS - i - 1
            }
            if (_secondsOfCountdown.value == 0) {
                 sendRaceNewTermRequest() // Request first term after countdown finishes
            }
        }
    }

    private fun setupTermDefinitionQueueForThisRound(currentRound: Int) {
        if (currentRound <= 0) return
        val gameSetup = _gameSetupState.value ?: return
        val totalRounds = gameSetup.roundCount
        val termsAndDefinitions = gameSetup.termDefinitionPairs

        if (termsAndDefinitions.isEmpty() || totalRounds <= 0) return

        val roundSize = termsAndDefinitions.size / totalRounds
        val extraItems = termsAndDefinitions.size % totalRounds

        val startIdx = (currentRound - 1) * roundSize + min(currentRound - 1, extraItems)
        val endIdx = min(
            currentRound * roundSize + min(currentRound, extraItems),
            termsAndDefinitions.size
        )
        if (startIdx >= endIdx || startIdx < 0 || endIdx > termsAndDefinitions.size) return

        val newList = termsAndDefinitions.subList(startIdx, endIdx).toMutableList()
        newList.shuffle()
        _termDefinitionPairsQueueThisRound.value = newList
    }

    private fun addMistake(term: String, definitionProvided: String) {
        // Find the correct definition for the term from the game setup state if available
        val correctDefinition = _gameSetupState.value?.termDefinitionPairs?.find { it.term == term }?.definition ?: "[Correct definition not found]"
        val key = "Term: '$term' - Your answer: '$definitionProvided' (Correct: '$correctDefinition')"
        _mistakeDictionary.value = _mistakeDictionary.value.toMutableMap().apply {
            put(key, getOrDefault(key, 0) + 1)
        }
    }

    fun checkDefinitionCorrectness() {
        sendRaceSubmitAnswer(currentTerm.value, _currentDefinition.value)
        // _currentDefinition.value = "" // Server response will dictate new term/definition
    }

    fun setCurrentDefinition(definition: String) {
        _currentDefinition.value = definition
    }

    fun pairConnected(term: String, definition: String): Boolean {
        // This logic might need adjustment based on how RACE_SUBMIT_ANSWER response works for connecting games
        // For connecting games, the server response should ideally confirm the match and update state.
        // The client-side queue manipulation might become redundant or lead to inconsistencies.
        // For now, we send the answer and let server response guide the state.
        sendRaceSubmitAnswer(term, definition)

        // Placeholder: The actual result of connection should come from server via RACE_SUBMIT_ANSWER handler
        // For immediate UI feedback, you might optimistically update, but server is source of truth.
        val currentQueue = _termDefinitionPairsQueueThisRound.value.toMutableList()
        val pairToCheck = TermDefinitionPair(term, definition)
        val isMatch = currentQueue.any { it.term == term && it.definition == definition } 

        // Optimistic update (consider if this is desired or if server should be sole source of truth)
        if (isMatch) {
            _termDefinitionPairsQueueThisRound.value = currentQueue.filterNot { it.term == term && it.definition == definition }
        } else {
            // Handle incorrect match display or feedback if needed, server response will also indicate this
        }
        return isMatch // This return might be for immediate UI, but server response is key
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
        WebSocketManager.sendMessage(personalGameDataRequest)
    }

    private fun sendGetGameRequest() {
        val getGameRequest = JSONObject().apply {
            put("${'$'}type", "GET_GAME")
            put("idGame", _idGame)
        }
        WebSocketManager.sendMessage(getGameRequest)
    }

    //region Parsers
    private fun parseGameSetupStateJson(response: String): GameSetupState? {
        return try {
            Gson().fromJson(response, GameSetupResponse::class.java)?.game
        } catch (e: Exception) {
            // Log.e("GamePageVM", "Error parsing GameSetupState: $e")
            _gameSetupState.value // Keep existing on error
        }
    }

    private fun parseStartRaceRoundJson(response: String): Number {
        return try {
            Gson().fromJson(response, StartRaceRoundResponse::class.java)?.raceGameInterRoundState?.currentRound ?: 0
        } catch (e: Exception) {
            // Log.e("GamePageVM", "Error parsing StartRaceRound: $e")
            0
        }
    }

    private fun parseEndGameResultsJson(response: String): List<RacePlayerResult> {
        return try {
            Gson().fromJson(response, EndGameResponse::class.java)?.racePlayerResults ?: emptyList()
        } catch (e: Exception) {
            // Log.e("GamePageVM", "Error parsing EndGameResults: $e")
            _playerFinalResults.value // Keep existing on error
        }
    }

    private fun parseSubmitAnswerJson(response: String): SubmitAnswerResponse? {
        return try {
            Gson().fromJson(response, SubmitAnswerResponse::class.java)
        } catch (e: Exception) {
            // Log.e("GamePageVM", "Error parsing SubmitAnswer: $e")
            null
        }
    }

    private fun parseNewTermJson(response: String): String {
        return try {
            Gson().fromJson(response, NewTermResponse::class.java)?.term ?: ""
        } catch (e: Exception) {
            // Log.e("GamePageVM", "Error parsing NewTerm: $e")
            ""
        }
    }
    //endregion
}
