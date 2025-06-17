package cz.zlehcito.viewmodel

import cz.zlehcito.model.TermDefinitionPair
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Holds all state and logic for the Connecting game mode
class ConnectingGameManager(private val maxVisiblePairs: Int = 5) {
    data class UiState(
        val displayedTerms: List<TermDefinitionPair> = emptyList(),
        val displayedDefinitions: List<TermDefinitionPair> = emptyList(),
        val selectedTermIndex: Int? = null,
        val selectedDefinitionIndex: Int? = null,
        val connectedCount: Int = 0,
        val mistakesCount: Int = 0,
        val feedback: String? = null,
        val totalPairsInRound: Int = 0,
        val roundFinished: Boolean = false,
        val results: List<Pair<TermDefinitionPair, Boolean>> = emptyList() // Pair and correctness
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private var pairQueue: MutableList<TermDefinitionPair> = mutableListOf()
    private val roundResults: MutableList<Pair<TermDefinitionPair, Boolean>> = mutableListOf()

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

    private fun updateDisplayedPairs() {
        val visible = pairQueue.take(maxVisiblePairs)
        _uiState.value = _uiState.value.copy(
            displayedTerms = visible.shuffled(),
            displayedDefinitions = visible.shuffled()
        )
    }

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

    fun setSelectedTermIndex(index: Int?) {
        _uiState.value = _uiState.value.copy(selectedTermIndex = index)
    }

    fun setSelectedDefinitionIndex(index: Int?) {
        _uiState.value = _uiState.value.copy(selectedDefinitionIndex = index)
    }

    fun setFeedback(feedback: String?) {
        _uiState.value = _uiState.value.copy(feedback = feedback)
    }

    fun reset() {
        pairQueue.clear()
        roundResults.clear()
        _uiState.value = UiState()
    }
}
