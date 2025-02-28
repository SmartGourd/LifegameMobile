package cz.zlehcito

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import cz.zlehcito.model.entities.AvailablePages
import cz.zlehcito.model.modelHandlers.GamePageModel
import cz.zlehcito.model.modelHandlers.GameSetupModel
import cz.zlehcito.network.WebSocketManager
import cz.zlehcito.ui.pages.GamePage
import cz.zlehcito.ui.pages.GameSetupPage
import cz.zlehcito.ui.pages.LobbyPage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebSocketManager.connect()

        setContent {
            var currentPage by rememberSaveable { mutableStateOf(AvailablePages.Lobby) }

            val navigateToLobbyPage: () -> Unit = {
                currentPage = AvailablePages.Lobby
            }

            val navigateToGamePage: (Int, String) -> Unit = { idGame, idUser ->
                GamePageModel.initializeModel(idGame, idUser)
                currentPage = AvailablePages.Game
            }

            val navigateToGameSetupPage: (Int) -> Unit = { idGame ->
                GameSetupModel.initializeModel(idGame, navigateToGamePage = navigateToGamePage)
                currentPage = AvailablePages.GameSetup
            }


            // Render pages based on the current page
            when (currentPage) {
                AvailablePages.Lobby -> LobbyPage(navigateToGameSetupPage = navigateToGameSetupPage)
                AvailablePages.GameSetup -> GameSetupPage(navigateToLobbyPage = navigateToLobbyPage)
                AvailablePages.Game -> GamePage(navigateToLobbyPage = navigateToLobbyPage)
            }
        }
    }
}
