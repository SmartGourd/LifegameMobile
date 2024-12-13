package cz.zlehcito.model.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cz.zlehcito.model.appState.AppState
import cz.zlehcito.model.modelHandlers.GameSetupModelHandler

class GameSetupViewModel(
    private val appState: AppState,
    private val navigateToPage: (String) -> Unit,
) : ViewModel() {
    val gameSetupModelHandler = GameSetupModelHandler(appState, navigateToPage)
}

class GameSetupViewModelFactory(
    private val appState: AppState,
    private val navigateToPage: (String) -> Unit,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameSetupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameSetupViewModel(appState, navigateToPage) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}