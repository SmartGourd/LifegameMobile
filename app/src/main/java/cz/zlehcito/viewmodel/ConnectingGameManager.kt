package cz.zlehcito.viewmodel

import cz.zlehcito.model.TermDefinitionPair
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ConnectingGameManager manages the state and logic for the 'Connecting' game mode.
// It handles the current round, visible pairs, user selections, and feedback.
class ConnectingGameManager(private val maxVisiblePairs: Int = 5) {
    // UiState holds all UI-related state for the Connecting game round.
    data class UiState(
        val displayedTerms: List<TermDefinitionPair> = emptyList(), // Terms currently shown to the user
        val displayedDefinitions: List<TermDefinitionPair> = emptyList(), // Definitions currently shown
        val selectedTermIndex: Int? = null, // Index of selected term
        val selectedDefinitionIndex: Int? = null, // Index of selected definition
        val connectedCount: Int = 0, // Number of correct connections
        val mistakesCount: Int = 0, // Number of mistakes made
        val feedback: String? = null, // Feedback for the last action
        val totalPairsInRound: Int = 0, // Total pairs in this round
        val roundFinished: Boolean = false, // Whether the round is finished
        val results: List<Pair<TermDefinitionPair, Boolean>> = emptyList() // List of pairs and correctness
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private var pairQueue: MutableList<TermDefinitionPair> = mutableListOf() // Queue of pairs to be matched
    private val roundResults: MutableList<Pair<TermDefinitionPair, Boolean>> = mutableListOf() // Results for the round

    // Starts a new round with the given pairs
    fun startRound(pairs: List<TermDefinitionPair>) {
        pairQueue = pairs.toMutableList()
        roundResults.clear()
        updateDisplayedPairs()
        _uiState.value = _uiState.value.copy(
            connectedCount = 0,
            mistakesCount = 0,
            feedback = null,
            totalPairsInRound = pairs.size,
            roundFinished = false,
            results = emptyList(),
            selectedTermIndex = null,
            selectedDefinitionIndex = null
        )
    }

    // Updates the visible terms and definitions for the UI
    private fun updateDisplayedPairs() {
        val visible = pairQueue.take(maxVisiblePairs)
        _uiState.value = _uiState.value.copy(
            displayedTerms = visible.shuffled(),
            displayedDefinitions = visible.shuffled()
        )
    }

    // Tries to connect a term and definition, updates state and feedback
    suspend fun tryConnect(term: TermDefinitionPair, definition: TermDefinitionPair) {
        withContext(Dispatchers.Default) {
            if (term.term == definition.term && term.definition == definition.definition) {
                // Correct match: remove only the first matching pair from queue
                val idx = pairQueue.indexOfFirst { it.term == term.term && it.definition == term.definition }
                if (idx != -1) {
                    pairQueue.removeAt(idx)
                }
                roundResults.add(term to true)
                val connected = _uiState.value.connectedCount + 1
                _uiState.value = _uiState.value.copy(
                    connectedCount = connected,
                    feedback = "correct",
                    selectedTermIndex = null,
                    selectedDefinitionIndex = null
                )
            } else {
                // Incorrect: add pair to end (allow duplicates)
                pairQueue.add(term)
                roundResults.add(term to false)
                val mistakes = _uiState.value.mistakesCount + 1
                _uiState.value = _uiState.value.copy(
                    mistakesCount = mistakes,
                    feedback = "incorrect",
                    selectedTermIndex = null,
                    selectedDefinitionIndex = null
                )
            }
            // Update visible pairs
            updateDisplayedPairs()
            // Check if round finished
            if (pairQueue.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    roundFinished = true,
                    results = roundResults.toList()
                )
            }
        }
    }

    // Sets the selected term index in the UI state
    fun setSelectedTermIndex(index: Int?) {
        _uiState.value = _uiState.value.copy(selectedTermIndex = index)
    }

    // Sets the selected definition index in the UI state
    fun setSelectedDefinitionIndex(index: Int?) {
        _uiState.value = _uiState.value.copy(selectedDefinitionIndex = index)
    }

    // Sets feedback message in the UI state
    fun setFeedback(feedback: String?) {
        _uiState.value = _uiState.value.copy(feedback = feedback)
    }

    // Resets the manager to its initial state
    fun reset() {
        pairQueue.clear()
        roundResults.clear()
        _uiState.value = UiState()
    }
}
