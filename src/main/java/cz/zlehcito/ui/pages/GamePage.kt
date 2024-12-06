package cz.zlehcito.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.zlehcito.network.WebSocketManager
import org.json.JSONObject

@Composable
fun GamePage(
    webSocketManager: WebSocketManager,
    navigateToPage: (String, Int, Int) -> Unit,
    idGame: Int,
    idUser: Int,
) {
    var gameData by remember { mutableStateOf("Waiting for game data...") }

    LaunchedEffect(Unit) {
        webSocketManager.connect(object : okhttp3.WebSocketListener() {
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                gameData = text
            }
        })

        // Request game data
        val gameRequest = JSONObject()
        gameRequest.put("type", "GET_GAME")
        gameRequest.put("data", 24)
        webSocketManager.sendMessage(gameRequest)
    }

    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Text("Game Page", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(gameData)
        }
    }
}
