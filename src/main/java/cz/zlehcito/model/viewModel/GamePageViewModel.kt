package cz.zlehcito.model.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cz.zlehcito.model.appState.AppState
import cz.zlehcito.model.modelHandlers.GamePageModelHandler

class GamePageViewModel(
    private val appState: AppState,
) : ViewModel() {
    val modelHandler = GamePageModelHandler(appState)
}

class GamePageViewModelFactory(
    private val appState: AppState
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GamePageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GamePageViewModel(appState) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
