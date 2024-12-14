package cz.zlehcito

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import cz.zlehcito.model.viewModel.AppStateViewModel
import cz.zlehcito.model.viewModel.AppStateViewModelFactory
import cz.zlehcito.network.WebSocketManager
import cz.zlehcito.ui.pages.*

class MainActivity : ComponentActivity() {
    private val webSocketManager = WebSocketManager("wss://zlehcito.cz/ws")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webSocketManager.connect()

        val appStateViewModel: AppStateViewModel by lazy {
            AppStateViewModelFactory(
                webSocketManager = webSocketManager,
                idGame = 0,
                idUser = "",
            ).create(AppStateViewModel::class.java)
        }

        setContent {
            val appState = appStateViewModel.appState
            var currentPage by rememberSaveable { mutableStateOf("Lobby") }

            val navigateToPage: (String) -> Unit = { page ->
                currentPage = page
            }

            // Render pages based on the current page
            when (currentPage) {
                "Lobby" -> LobbyPage(appState, navigateToPage)
                "GameSetup" -> GameSetupPage(appState, navigateToPage)
                "Game" -> GamePage(appState, navigateToPage)
            }
        }
    }
}
