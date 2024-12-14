package cz.zlehcito.model.appState

import cz.zlehcito.network.WebSocketManager

class AppState(
    val webSocketManager: WebSocketManager,
    var idGame: Int,
    var idUser: String,
)
