package cz.zlehcito.model.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cz.zlehcito.model.modelHandlers.GameSetupModelHandler
import cz.zlehcito.network.WebSocketManager

class GameSetupViewModel(
    webSocketManager: WebSocketManager,
    navigateToPage: (String, Int, Int) -> Unit,
    idGame: Int
) : ViewModel() {
    val gameSetupModelHandler = GameSetupModelHandler(webSocketManager, navigateToPage, idGame)
}

class GameSetupViewModelFactory(
    private val webSocketManager: WebSocketManager,
    private val navigateToPage: (String, Int, Int) -> Unit,
    private val idGame: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameSetupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameSetupViewModel(webSocketManager, navigateToPage, idGame) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}