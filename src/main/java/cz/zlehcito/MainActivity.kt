package cz.zlehcito

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import cz.zlehcito.network.WebSocketManager
import cz.zlehcito.ui.pages.*

/*
Questions
How can I put optional parameters to navigateToPage
 */


class MainActivity : ComponentActivity() {
    private val webSocketManager = WebSocketManager("wss://zlehcito.cz/ws")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webSocketManager.connect()

        setContent {
            // Manage current page and optional parameters with state
            var currentPage by rememberSaveable { mutableStateOf("Lobby") }
            var idGame by rememberSaveable { mutableIntStateOf(0) }
            var idUser by rememberSaveable { mutableIntStateOf(0) }

            // Navigation function with optional parameters
            val navigateToPage: (String, Int, Int) -> Unit = { page, gameId, userId ->
                currentPage = page
                idGame = gameId
                idUser = userId
            }

            // Render pages based on the current state
            when (currentPage) {
                "Lobby" -> LobbyPage(webSocketManager, navigateToPage)
                "GameSetup" -> GameSetupPage(webSocketManager, navigateToPage, idGame)
                "Game" -> GamePage(webSocketManager, navigateToPage, idGame, idUser)
                "Results" -> ResultsPage(webSocketManager, navigateToPage, idGame, idUser)
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        //webSocketManager.disconnect()
    }
}

