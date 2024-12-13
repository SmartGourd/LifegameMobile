package cz.zlehcito.model.appState

import cz.zlehcito.network.WebSocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppState(
    val webSocketManager: WebSocketManager,
    var idGame: Int,
    var idUser: String,
    val mistakesDone: Array<String>,
)
