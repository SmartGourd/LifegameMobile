package cz.zlehcito.viewmodel

import cz.zlehcito.model.TermDefinitionPair
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// WritingGameManager manages the state and logic for the 'Writing' game mode.
// It tracks the current term, user responses, mistakes, and feedback for the UI.
class WritingGameManager {
    // UiState holds all UI-related state for the Writing game round.
    data class UiState(
        val currentTerm: String? = null, // The term currently being answered
        val userResponse: String = "", // The user's current input
        val isWrongAnswer: Boolean = false, // Whether the last answer was wrong
        val isCorrectAnswer: Boolean = false, // Whether the last answer was correct
        val correctDefinition: String? = null, // The correct definition if the answer was wrong
        val mistakesCount: Int = 0, // Total number of mistakes
        val mistakePairs: Map<String, Int> = emptyMap() // Mistake count per term
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    // Sets the current term and resets answer state
    fun setCurrentTerm(term: String?) {
        _uiState.value = _uiState.value.copy(currentTerm = term, userResponse = "", isWrongAnswer = false, isCorrectAnswer = false, correctDefinition = null)
    }

    // Updates the user's response
    fun setUserResponse(response: String) {
        _uiState.value = _uiState.value.copy(userResponse = response)
    }

    // Updates the state based on whether the answer was correct or not
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

    // Resets the manager to its initial state
    fun reset() {
        _uiState.value = UiState()
    }
}
