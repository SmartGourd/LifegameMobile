package cz.zlehcito.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.zlehcito.network.WebSocketManager
import cz.zlehcito.model.Game
import cz.zlehcito.model.GameResponse
import com.google.gson.Gson

@Composable
fun LobbyPage(webSocketManager: WebSocketManager, navigateToPage: (String) -> Unit) {
    var gamesList by remember { mutableStateOf<List<Game>>(emptyList()) }

    LaunchedEffect(Unit) {
        webSocketManager.connect(object : okhttp3.WebSocketListener() {
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                gamesList = parseJson(text)
            }
        })

        // Send the GET_GAMES request
        val json = org.json.JSONObject().apply { put("type", "GET_GAMES") }
        webSocketManager.sendMessage(json)
    }

    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Text(
                text = "Games Lobby",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )

            if (gamesList.isNotEmpty()) {
                GamesList(games = gamesList, onGameClick = { gameId ->
                    // Navigate to the game page (or setup page)
                    navigateToPage("GameSetup")
                })
            } else {
                Text(
                    text = "No games available.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun GamesList(games: List<Game>, onGameClick: (Int) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        games.forEach { game ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                onClick = { onGameClick(game.idGame) } // Pass game ID when clicked
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Game: ${game.name} (${game.gameType})")
                    Text(text = "Status: ${game.gameStatus}")
                    Text(text = "Players: ${game.playerCount} / ${game.maxPlayers}")
                }
            }
        }
    }
}

// Function to parse JSON response
private fun parseJson(response: String): List<Game> {
    val gson = Gson()
    val gameResponse = gson.fromJson(response, GameResponse::class.java)
    return gameResponse.data.games
}
