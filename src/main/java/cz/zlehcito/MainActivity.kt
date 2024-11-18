package cz.zlehcito

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import cz.zlehcito.network.WebSocketManager
import cz.zlehcito.ui.pages.*

class MainActivity : ComponentActivity() {
    private val webSocketManager = WebSocketManager("wss://zlehcito.cz/ws")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentPage by remember { mutableStateOf("Lobby") }

            val navigateToPage: (String) -> Unit = { page ->
                currentPage = page
            }

            when (currentPage) {
                "Lobby" -> LobbyPage(webSocketManager, navigateToPage)
                "GameSetup" -> GameSetupPage(webSocketManager, navigateToPage)
                "Game" -> GamePage(webSocketManager)
                "Results" -> ResultsPage(webSocketManager)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketManager.disconnect()
    }
}
