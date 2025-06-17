package cz.zlehcito.viewmodel

import cz.zlehcito.model.TermDefinitionPair
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Holds all state and logic for the Writing game mode
class WritingGameManager {
    data class UiState(
        val currentTerm: String? = null,
        val userResponse: String = "",
        val isWrongAnswer: Boolean = false,
        val isCorrectAnswer: Boolean = false,
        val correctDefinition: String? = null,
        val mistakesCount: Int = 0,
        val mistakePairs: Map<String, Int> = emptyMap()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun setCurrentTerm(term: String?) {
        _uiState.value = _uiState.value.copy(currentTerm = term, userResponse = "", isWrongAnswer = false, isCorrectAnswer = false, correctDefinition = null)
    }

    fun setUserResponse(response: String) {
        _uiState.value = _uiState.value.copy(userResponse = response)
    }

    fun setAnswerResult(isCorrect: Boolean, correctDefinition: String? = null) {
        val currentTerm = _uiState.value.currentTerm
        val currentMistakes = _uiState.value.mistakePairs.toMutableMap()
        if (!isCorrect && currentTerm != null) {
            currentMistakes[currentTerm] = (currentMistakes[currentTerm] ?: 0) + 1
        }
        _uiState.value = _uiState.value.copy(
            isWrongAnswer = !isCorrect,
            isCorrectAnswer = isCorrect,
            correctDefinition = if (!isCorrect) correctDefinition else null,
            userResponse = if (!isCorrect && correctDefinition != null) correctDefinition else _uiState.value.userResponse,
            mistakesCount = if (!isCorrect) _uiState.value.mistakesCount + 1 else _uiState.value.mistakesCount,
            mistakePairs = currentMistakes
        )
    }

    fun reset() {
        _uiState.value = UiState()
    }
}
