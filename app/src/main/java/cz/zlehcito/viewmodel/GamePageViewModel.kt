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
import org.json.JSONObject
import java.util.Collections

class GamePageViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    // Game IDs from navigation
    private val _idGame: String = savedStateHandle.get<String>("idGame") ?: "" // Changed to String
    private val _idUser: String = savedStateHandle.get<String>("idUser") ?: ""

    private companion object {
        private const val COUNTDOWN_INITIAL_SECONDS = 3
        private const val MAX_VISIBLE_CONNECTING_PAIRS = 5 // Added constant
    }

    // Game Details State (like props.gameDetails in Vue)
    private val _gameDetails = MutableStateFlow<RaceGame?>(null)
    val gameDetails: StateFlow<RaceGame?> = _gameDetails.asStateFlow()

    private val _inputType = MutableStateFlow<String?>(null) // "Writing" or "Connecting"
    val inputType: StateFlow<String?> = _inputType.asStateFlow()

    // Countdown State
    private val _showCountdown = MutableStateFlow(false)
    val showCountdown: StateFlow<Boolean> = _showCountdown.asStateFlow()

    private val _countdownSeconds = MutableStateFlow(0)
    val countdownSeconds: StateFlow<Int> = _countdownSeconds.asStateFlow()

    // Game State
    private val _currentRound = MutableStateFlow(0)
    val currentRound: StateFlow<Int> = _currentRound.asStateFlow()

    private val _displayResults = MutableStateFlow(false)
    val displayResults: StateFlow<Boolean> = _displayResults.asStateFlow()

    private val _playerRoundResults = MutableStateFlow<List<RacePlayerResult>>(emptyList()) // For inter-round results
    val playerRoundResults: StateFlow<List<RacePlayerResult>> = _playerRoundResults.asStateFlow()
    
    private val _playerFinalResults = MutableStateFlow<List<RacePlayerResult>>(emptyList()) // For final game results
    val playerFinalResults: StateFlow<List<RacePlayerResult>> = _playerFinalResults.asStateFlow()

    private val _mistakePairs = MutableStateFlow<Map<String, Int>>(emptyMap())
    val mistakePairs: StateFlow<Map<String, Int>> = _mistakePairs.asStateFlow()

    // Writing Game Specific State
    private val _writing_currentTerm = MutableStateFlow<String?>(null)
    val writing_currentTerm: StateFlow<String?> = _writing_currentTerm.asStateFlow()

    private val _writing_userResponse = MutableStateFlow("")
    val writing_userResponse: StateFlow<String> = _writing_userResponse.asStateFlow()

    private val _writing_isWrongAnswer = MutableStateFlow(false)
    val writing_isWrongAnswer: StateFlow<Boolean> = _writing_isWrongAnswer.asStateFlow()

    // Connecting Game Specific State
    private var fullTermDefinitionQueue: List<TermDefinitionPair> = emptyList() // Full list for the game
    private val _connecting_termDefinitionQueueThisRound = MutableStateFlow<List<TermDefinitionPair>>(emptyList())
    val connecting_termDefinitionQueueThisRound: StateFlow<List<TermDefinitionPair>> = _connecting_termDefinitionQueueThisRound.asStateFlow()

    // Stores all pairs for the current round that are not yet correctly answered.
    private var unansweredPairsInCurrentRound: MutableList<TermDefinitionPair> = mutableListOf()

    private val _connecting_displayedTerms = MutableStateFlow<List<TermDefinitionPair>>(emptyList())
    val connecting_displayedTerms: StateFlow<List<TermDefinitionPair>> = _connecting_displayedTerms.asStateFlow()

    private val _connecting_displayedDefinitions = MutableStateFlow<List<TermDefinitionPair>>(emptyList())
    val connecting_displayedDefinitions: StateFlow<List<TermDefinitionPair>> = _connecting_displayedDefinitions.asStateFlow()
    
    private val _connecting_selectedTerm = MutableStateFlow<TermDefinitionPair?>(null)
    val connecting_selectedTerm: StateFlow<TermDefinitionPair?> = _connecting_selectedTerm.asStateFlow()

    private val _connecting_selectedDefinition = MutableStateFlow<TermDefinitionPair?>(null)
    val connecting_selectedDefinition: StateFlow<TermDefinitionPair?> = _connecting_selectedDefinition.asStateFlow()

    private val _connecting_connectedCount = MutableStateFlow(0)
    val connecting_connectedCount: StateFlow<Int> = _connecting_connectedCount.asStateFlow()

    private val _connecting_mistakesCount = MutableStateFlow(0) // Mistakes within the connecting component
    val connecting_mistakesCount: StateFlow<Int> = _connecting_mistakesCount.asStateFlow()

    private val _connecting_feedback = MutableStateFlow<String?>(null) // "correct" or "incorrect"
    val connecting_feedback: StateFlow<String?> = _connecting_feedback.asStateFlow()


    private val _navigateToLobby = MutableStateFlow<Boolean>(false)
    val navigateToLobby: StateFlow<Boolean> = _navigateToLobby.asStateFlow()

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
                fullTermDefinitionQueue = response.game.termDefinitionPairs ?: emptyList()
                
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
                    _writing_currentTerm.value = response.term
                    _writing_userResponse.value = ""
                    _writing_isWrongAnswer.value = false
                }
            }
        }

        WebSocketManager.registerHandler("RACE_SUBMIT_ANSWER") { json ->
            viewModelScope.launch(Dispatchers.Default) {
                val response = Gson().fromJson(json.toString(), SubmitAnswerResponse::class.java)
                if (_inputType.value == "Writing") {
                    _writing_isWrongAnswer.value = !response.answerCorrect
                    _writing_currentTerm.value = response.termDefinitionPair.term
                    if (!response.answerCorrect) {
                        _writing_userResponse.value = response.termDefinitionPair.definition
                        updateMistakePairs(response.termDefinitionPair.term)
                    } else {
                        _writing_userResponse.value = ""
                    }
                    if (response.answerCorrect && !response.endOfRound) {
                        sendRaceNewTermRequest()
                    }
                } else if (_inputType.value == "Connecting") {
                    _connecting_feedback.value = if (response.answerCorrect) "correct" else "incorrect"
                    viewModelScope.launch { // Clear feedback after a short delay
                        delay(1000)
                        _connecting_feedback.value = null
                    }
                    if (response.answerCorrect) {
                        _connecting_connectedCount.value += 1
                        val correctTerm = response.termDefinitionPair.term
                        val correctDefinition = response.termDefinitionPair.definition
                        unansweredPairsInCurrentRound.removeAll { it.term == correctTerm && it.definition == correctDefinition }
                        refreshDisplayedConnectingPairs()
                    } else {
                        _connecting_mistakesCount.value += 1
                        updateMistakePairs(response.termDefinitionPair.term)
                    }
                    _connecting_selectedTerm.value = null
                    _connecting_selectedDefinition.value = null
                    if (_connecting_displayedTerms.value.isEmpty() && !response.endOfRound) {
                        Log.d("GamePageVM", "Connecting: All pairs for this segment seem connected by client.")
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
        val game = _gameDetails.value ?: return
        val roundCount = game.roundCount.takeIf { it > 0 } ?: 1 // Avoid division by zero if roundCount is 0
        val totalPairs = fullTermDefinitionQueue.size

        val roundSize = totalPairs / roundCount
        val extraItems = totalPairs % roundCount

        val startIdx = (roundNumber - 1) * roundSize + minOf(roundNumber - 1, extraItems)
        val endIdx = minOf(roundNumber * roundSize + minOf(roundNumber, extraItems), totalPairs)
        
        if (startIdx >= endIdx) {
            _connecting_termDefinitionQueueThisRound.value = emptyList()
            unansweredPairsInCurrentRound.clear()
            refreshDisplayedConnectingPairs() // Will set displayed lists to empty
            Log.w("GamePageVM", "Connecting: No pairs for round $roundNumber. Start: $startIdx, End: $endIdx, Total: $totalPairs")
            return
        }

        val pairsForRound = fullTermDefinitionQueue.slice(startIdx until endIdx).toMutableList()
        
        _connecting_termDefinitionQueueThisRound.value = pairsForRound
        unansweredPairsInCurrentRound = ArrayList(pairsForRound) // Initialize with a copy

        refreshDisplayedConnectingPairs() // Setup initial display
        
        _connecting_connectedCount.value = 0
        _connecting_mistakesCount.value = 0
        _connecting_selectedTerm.value = null
        _connecting_selectedDefinition.value = null
        Log.d("GamePageVM", "Connecting: Prepared round $roundNumber with ${pairsForRound.size} pairs.")
    }

    private fun refreshDisplayedConnectingPairs() {
        // Take up to MAX_VISIBLE_CONNECTING_PAIRS unanswered pairs
        val visiblePairsSource = unansweredPairsInCurrentRound.take(MAX_VISIBLE_CONNECTING_PAIRS).toMutableList()

        // Create terms list for display (shuffled)
        // These TermDefinitionPair objects are temporary for display; their 'term' or 'definition' is key.
        val termsForDisplay = visiblePairsSource.map { TermDefinitionPair(term = it.term, definition = "") }.toMutableList()
        Collections.shuffle(termsForDisplay)
        _connecting_displayedTerms.value = termsForDisplay

        // Create definitions list for display (shuffled) from the same source pairs
        val definitionsForDisplay = visiblePairsSource.map { TermDefinitionPair(term = "", definition = it.definition) }.toMutableList()
        Collections.shuffle(definitionsForDisplay)
        _connecting_displayedDefinitions.value = definitionsForDisplay

        Log.d("GamePageVM", "Refreshed displayed pairs. Terms: ${termsForDisplay.size}, Defs: ${definitionsForDisplay.size}. Unanswered left: ${unansweredPairsInCurrentRound.size}")
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
        if (_inputType.value == "Writing" && _writing_currentTerm.value != null) {
            val request = JSONObject().apply {
                put("\$type", "RACE_SUBMIT_ANSWER")
                put("gameManipulationKey", JSONObject().apply {
                    put("idGame", _idGame)
                    put("idUser", _idUser)
                })
                put("answer", JSONObject().apply {
                    put("term", _writing_currentTerm.value)
                    put("definition", _writing_userResponse.value)
                })
            }
            WebSocketManager.sendMessage(request)
            Log.d("GamePageVM", "Sent RACE_SUBMIT_ANSWER for Writing: Term='${_writing_currentTerm.value}', Def='${_writing_userResponse.value}'")
        }
    }
    
    fun setWritingUserResponse(response: String) {
        _writing_userResponse.value = response
    }

    fun selectConnectingTerm(termPair: TermDefinitionPair) {
        _connecting_selectedTerm.value = termPair
        checkConnectingMatch()
    }

    fun selectConnectingDefinition(defPair: TermDefinitionPair) {
        _connecting_selectedDefinition.value = defPair
        checkConnectingMatch()
    }

    private fun checkConnectingMatch() {
        val term = _connecting_selectedTerm.value
        val definition = _connecting_selectedDefinition.value

        if (term != null && definition != null) {
            // Find the original full pair for the selected term
            val originalPairForTerm = _connecting_termDefinitionQueueThisRound.value.find { it.term == term.term }

            if (originalPairForTerm != null && originalPairForTerm.definition == definition.definition) {
                // This is a client-side pre-check. Server will confirm.
                // Send to server
                sendConnectingAnswer(originalPairForTerm.term, originalPairForTerm.definition)
            } else if (originalPairForTerm != null) { // Mismatch
                sendConnectingAnswer(originalPairForTerm.term, definition.definition) // Send the incorrect attempt
            } else {
                // Should not happen if termPair.term is from displayedTerms
                 _connecting_selectedTerm.value = null
                 _connecting_selectedDefinition.value = null
            }
        }
    }
    
    private fun sendConnectingAnswer(term: String, definition: String) {
        val request = JSONObject().apply {
            put("\$type", "RACE_SUBMIT_ANSWER")
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
}
