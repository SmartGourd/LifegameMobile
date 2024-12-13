package cz.zlehcito.model.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cz.zlehcito.model.appState.AppState
import cz.zlehcito.network.WebSocketManager

class AppStateViewModel(
    private val webSocketManager: WebSocketManager,
    private val idGame: Int,
    private val idUser: String,
    private val mistakesDone: Array<String>
) : ViewModel() {

    // Hold the AppState instance
    val appState = AppState(
        webSocketManager = webSocketManager,
        idGame = idGame,
        idUser = idUser,
        mistakesDone = mistakesDone
    )

    override fun onCleared() {
        super.onCleared()
        appState.webSocketManager.disconnect()
    }
}

class AppStateViewModelFactory(
    private val webSocketManager: WebSocketManager,
    private val idGame: Int,
    private val idUser: String,
    private val mistakesDone: Array<String>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppStateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppStateViewModel(
                webSocketManager = webSocketManager,
                idGame = idGame,
                idUser = idUser,
                mistakesDone = mistakesDone
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}