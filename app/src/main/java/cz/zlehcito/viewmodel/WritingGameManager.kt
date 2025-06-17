package cz.zlehcito.viewmodel

import cz.zlehcito.model.TermDefinitionPair
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Holds all state and logic for the Writing game mode
class WritingGameManager {
    data class UiState(
        val currentTerm: String? = null,
        val userResponse: String = "",
        val isWrongAnswer: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun setCurrentTerm(term: String?) {
        _uiState.value = _uiState.value.copy(currentTerm = term, userResponse = "", isWrongAnswer = false)
    }

    fun setUserResponse(response: String) {
        _uiState.value = _uiState.value.copy(userResponse = response)
    }

    fun setWrongAnswer(isWrong: Boolean, correctDefinition: String? = null) {
        _uiState.value = _uiState.value.copy(
            isWrongAnswer = isWrong,
            userResponse = if (isWrong && correctDefinition != null) correctDefinition else _uiState.value.userResponse
        )
    }

    fun reset() {
        _uiState.value = UiState()
    }
}
