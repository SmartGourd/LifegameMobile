package cz.zlehcito.viewmodel

import cz.zlehcito.model.TermDefinitionPair
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Holds all state and logic for the Connecting game mode
class ConnectingGameManager(private val maxVisiblePairs: Int = 5) {
    data class UiState(
        val displayedTerms: List<TermDefinitionPair> = emptyList(),
        val displayedDefinitions: List<TermDefinitionPair> = emptyList(),
        val selectedTerm: TermDefinitionPair? = null,
        val selectedDefinition: TermDefinitionPair? = null,
        val connectedCount: Int = 0,
        val mistakesCount: Int = 0,
        val feedback: String? = null,
        val totalPairsInRound: Int = 0
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    // Add methods to update state as needed, e.g.:
    fun setDisplayedPairs(terms: List<TermDefinitionPair>, definitions: List<TermDefinitionPair>, total: Int) {
        _uiState.value = _uiState.value.copy(
            displayedTerms = terms,
            displayedDefinitions = definitions,
            totalPairsInRound = total
        )
    }

    fun setSelectedTerm(term: TermDefinitionPair?) {
        _uiState.value = _uiState.value.copy(selectedTerm = term)
    }

    fun setSelectedDefinition(definition: TermDefinitionPair?) {
        _uiState.value = _uiState.value.copy(selectedDefinition = definition)
    }

    fun setFeedback(feedback: String?) {
        _uiState.value = _uiState.value.copy(feedback = feedback)
    }

    fun setConnectedCount(count: Int) {
        _uiState.value = _uiState.value.copy(connectedCount = count)
    }

    fun setMistakesCount(count: Int) {
        _uiState.value = _uiState.value.copy(mistakesCount = count)
    }

    fun reset() {
        _uiState.value = UiState()
    }
}
