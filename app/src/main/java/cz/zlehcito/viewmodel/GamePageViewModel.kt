package cz.zlehcito.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import cz.zlehcito.model.EndGameResponse
import cz.zlehcito.model.GetRaceGameResponse
import cz.zlehcito.model.NewTermResponse
import cz.zlehcito.model.RaceGame
import cz.zlehcito.model.RacePlayerResult
import cz.zlehcito.model.StartRaceRoundResponse
import cz.zlehcito.model.SubmitAnswerResponse
import cz.zlehcito.model.TermDefinitionPair
import cz.zlehcito.network.WebSocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class GamePageViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    // Game IDs from navigation
    private val _idGame: String = savedStateHandle.get<String>("idGame") ?: ""
    private val _idUser: String = savedStateHandle.get<String>("idUser") ?: ""

    private companion object {
        private const val COUNTDOWN_INITIAL_SECONDS = 3
        private const val MAX_VISIBLE_CONNECTING_PAIRS = 5 // Added constant
    }

    // Game Details State (like props.gameDetails in Vue)
    private val _gameDetails = MutableStateFlow<RaceGame?>(null)

    private val _inputType = MutableStateFlow<String?>(null) // "Writing" or "Connecting"
    val inputType: StateFlow<String?> = _inputType.asStateFlow()

    // Countdown State
    private val _showCountdown = MutableStateFlow(false)
    val showCountdown: StateFlow<Boolean> = _showCountdown.asStateFlow()

    private val _countdownSeconds = MutableStateFlow(0)
    val countdownSeconds: StateFlow<Int> = _countdownSeconds.asStateFlow()

    // Game State
    private val _currentRound = MutableStateFlow(0)

    private val _displayResults = MutableStateFlow(false)
    val displayResults: StateFlow<Boolean> = _displayResults.asStateFlow()

    private val _playerRoundResults = MutableStateFlow<List<RacePlayerResult>>(emptyList())
    
    private val _playerFinalResults = MutableStateFlow<List<RacePlayerResult>>(emptyList())
    val playerFinalResults: StateFlow<List<RacePlayerResult>> = _playerFinalResults.asStateFlow()

    private val _mistakePairs = MutableStateFlow<Map<String, Int>>(emptyMap())
    val mistakePairs: StateFlow<Map<String, Int>> = _mistakePairs.asStateFlow()

    // Connecting Game Specific State
    private val _navigateToLobby = MutableStateFlow<Boolean>(false)
    val navigateToLobby: StateFlow<Boolean> = _navigateToLobby.asStateFlow()

    val writingGameManager = WritingGameManager()
    val connectingGameManager = ConnectingGameManager(MAX_VISIBLE_CONNECTING_PAIRS)

    init {
        Log.d("GamePageVM", "Initializing with idGame: $_idGame, idUser: $_idUser")
        registerWebSocketHandlers()
    }


    private fun registerWebSocketHandlers() {
        WebSocketManager.registerHandler("RACE_GET_GAME") { json ->
            viewModelScope.launch(Dispatchers.Default) {
                val response = Gson().fromJson(json.toString(), GetRaceGameResponse::class.java)
                _gameDetails.value = response.game
                _inputType.value = response.game.inputType
                
                // If game is already over (e.g., page refresh)
                if (response.game.currentRound == -1) {
                    _displayResults.value = true
                } else {
                    _currentRound.value = response.game.currentRound
                    if (_currentRound.value > 0 && _inputType.value == "Connecting") {
                        prepareConnectingGameRound(_currentRound.value)
                    }
                }
            }
        }

        WebSocketManager.registerHandler("RACE_ROUND_START") { json ->
            viewModelScope.launch(Dispatchers.Default) {
                val response = Gson().fromJson(json.toString(), StartRaceRoundResponse::class.java)
                _currentRound.value = response.raceGameInterRoundState.currentRound
                _playerRoundResults.value = response.raceGameInterRoundState.playerResults
                _displayResults.value = false // Ensure results screen is hidden
                
                if (_inputType.value == "Connecting") {
                    prepareConnectingGameRound(_currentRound.value)
                }
                startCountdownAndRound()
            }
        }

        WebSocketManager.registerHandler("RACE_NEW_TERM") { json ->
            viewModelScope.launch(Dispatchers.Default) {
                if (_inputType.value == "Writing") {
                    val response = Gson().fromJson(json.toString(), NewTermResponse::class.java)
                    writingGameManager.setCurrentTerm(response.term)
                }
            }
        }

        WebSocketManager.registerHandler("RACE_SUBMIT_ANSWER") { json ->
            viewModelScope.launch(Dispatchers.Default) {
                val response = Gson().fromJson(json.toString(), SubmitAnswerResponse::class.java)
                if (_inputType.value == "Writing") {
                    writingGameManager.setAnswerResult(
                        isCorrect = response.answerCorrect,
                        correctDefinition = response.termDefinitionPair.definition
                    )
                    writingGameManager.setCurrentTerm(response.termDefinitionPair.term)
                    if (response.answerCorrect && !response.endOfRound) {
                        sendRaceNewTermRequest()
                    }
                } else if (_inputType.value == "Connecting") {
                    connectingGameManager.setFeedback(if (response.answerCorrect) "correct" else "incorrect")
                    viewModelScope.launch {
                        delay(1000)
                        connectingGameManager.setFeedback(null)
                    }
                    if (response.answerCorrect) {
                        val correctTerm = response.termDefinitionPair.term
                        val correctDefinition = response.termDefinitionPair.definition
                        // unansweredPairsInCurrentRound.removeAll { it.term == correctTerm && it.definition == correctDefinition }
                        // refreshDisplayedConnectingPairs()
                    } else {
                        updateMistakePairs(response.termDefinitionPair.term)
                    }
                }
            }
        }

        WebSocketManager.registerHandler("RACE_END") { json ->
            viewModelScope.launch(Dispatchers.Default) {
                val response = Gson().fromJson(json.toString(), EndGameResponse::class.java)
                _playerFinalResults.value = response.racePlayerResults
                _displayResults.value = true
                _showCountdown.value = false
            }
        }
    }

    private fun startCountdownAndRound() {
        _showCountdown.value = true
        _countdownSeconds.value = COUNTDOWN_INITIAL_SECONDS
        viewModelScope.launch(Dispatchers.Default) {
            repeat(COUNTDOWN_INITIAL_SECONDS) {
                delay(1000)
                _countdownSeconds.value -= 1
            }
            _showCountdown.value = false
            // After countdown, start the actual round logic
            if (_inputType.value == "Writing") {
                sendRaceNewTermRequest() // Request the first term for writing game
            } else if (_inputType.value == "Connecting") {
                // For connecting, pairs are already prepared by prepareConnectingGameRound
                // UI will become active.
            }
        }
    }
    
    private fun prepareConnectingGameRound(roundNumber: Int) {
        if (roundNumber < 1) return // Prevent negative indices and crash
        val game = _gameDetails.value ?: return
        val roundCount = game.roundCount.takeIf { it > 0 } ?: 1 // Avoid division by zero if roundCount is 0
        val totalPairs = game.termDefinitionPairs.size

        val roundSize = totalPairs / roundCount
        val extraItems = totalPairs % roundCount

        val startIdx = (roundNumber - 1) * roundSize + minOf(roundNumber - 1, extraItems)
        val endIdx = minOf(roundNumber * roundSize + minOf(roundNumber, extraItems), totalPairs)
        
        if (startIdx >= endIdx) {
            connectingGameManager.startRound(emptyList())
            return
        }

        val pairsForRound = game.termDefinitionPairs.slice(startIdx until endIdx)
        connectingGameManager.startRound(pairsForRound)
    }

    fun sendGetGameRequest() {
        val request = JSONObject().apply {
            put("\$type", "RACE_GET_GAME") // Assuming this is the type to get game details
            put("gameManipulationKey", JSONObject().apply {
                put("IdGame", _idGame)
                put("IdUser", _idUser)
            })
        }
        WebSocketManager.sendMessage(request)
        Log.d("GamePageVM", "Sent RACE_GET_GAME request for game: $_idGame")
    }

    private fun sendRaceNewTermRequest() {
        val request = JSONObject().apply {
            put("\$type", "RACE_NEW_TERM")
            put("gameManipulationKey", JSONObject().apply {
                put("idGame", _idGame)
                put("idUser", _idUser)
            })
        }
        WebSocketManager.sendMessage(request)
        Log.d("GamePageVM", "Sent RACE_NEW_TERM request")
    }

    fun submitWritingAnswer() {
        if (_inputType.value == "Writing" && writingGameManager.uiState.value.currentTerm != null) {
            val request = JSONObject().apply {
                put("$" + "type", "RACE_SUBMIT_ANSWER")
                put("gameManipulationKey", JSONObject().apply {
                    put("idGame", _idGame)
                    put("idUser", _idUser)
                })
                put("answer", JSONObject().apply {
                    put("term", writingGameManager.uiState.value.currentTerm)
                    put("definition", writingGameManager.uiState.value.userResponse)
                })
            }
            WebSocketManager.sendMessage(request)
            Log.d("GamePageVM", "Sent RACE_SUBMIT_ANSWER for Writing: Term='${writingGameManager.uiState.value.currentTerm}', Def='${writingGameManager.uiState.value.userResponse}'")
        }
    }
    
    fun setWritingUserResponse(response: String) {
        writingGameManager.setUserResponse(response)
    }

    fun selectConnectingTerm(index: Int) {
        connectingGameManager.setSelectedTermIndex(index)
        checkConnectingMatch()
    }

    fun selectConnectingDefinition(index: Int) {
        connectingGameManager.setSelectedDefinitionIndex(index)
        checkConnectingMatch()
    }

    private fun checkConnectingMatch() {
        val uiState = connectingGameManager.uiState.value
        val termIndex = uiState.selectedTermIndex
        val defIndex = uiState.selectedDefinitionIndex
        if (termIndex != null && defIndex != null) {
            val term = uiState.displayedTerms.getOrNull(termIndex)
            val definition = uiState.displayedDefinitions.getOrNull(defIndex)
            if (term != null && definition != null) {
                // Always send the answer to the server
                sendConnectingAnswer(term.term, definition.definition)
                viewModelScope.launch {
                    connectingGameManager.tryConnect(term, definition)
                }
            }
        }
    }
    
    private fun updateMistakePairs(term: String) {
        val currentMistakes = _mistakePairs.value.toMutableMap()
        currentMistakes[term] = (currentMistakes[term] ?: 0) + 1
        _mistakePairs.value = currentMistakes
    }
    
    fun onNavigateToLobbyClicked() {
        _navigateToLobby.value = true
    }

    fun onNavigationDone() {
        _navigateToLobby.value = false
    }

    override fun onCleared() {
        super.onCleared()
        // Unregister handlers if WebSocketManager allows it, or handle disconnection
        Log.d("GamePageVM", "ViewModel cleared")
    }

    private fun sendConnectingAnswer(term: String, definition: String) {
        val request = JSONObject().apply {
            put("$" + "type", "RACE_SUBMIT_ANSWER")
            put("gameManipulationKey", JSONObject().apply {
                put("idGame", _idGame)
                put("idUser", _idUser)
            })
            put("answer", JSONObject().apply {
                put("term", term)
                put("definition", definition)
            })
        }
        WebSocketManager.sendMessage(request)
        Log.d("GamePageVM", "Sent RACE_SUBMIT_ANSWER for Connecting: Term='$term', Def='$definition'")
    }
}
