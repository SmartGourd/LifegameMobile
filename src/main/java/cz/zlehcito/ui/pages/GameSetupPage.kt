package cz.zlehcito.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.zlehcito.model.GameSetup
import cz.zlehcito.model.GameSettings
import cz.zlehcito.model.Player
import cz.zlehcito.network.WebSocketManager
import org.json.JSONObject

@Composable
fun GameSetupPage(
    webSocketManager: WebSocketManager, navigateToPage: (String) -> Unit
) {
    var gameSetup by remember { mutableStateOf<GameSetup?>(null) }
    var playerName by remember { mutableStateOf("") }
    var joinMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        webSocketManager.connect(object : okhttp3.WebSocketListener() {
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                val response = JSONObject(text)

                when (response.getString("type")) {
                    "GET_GAME" -> {
                        val gameData = response.getJSONObject("data").getJSONObject("game")
                        gameSetup = GameSetup(
                            gameTypeSpecificSettings = GameSettings(
                                roundCount = gameData.getJSONObject("gameTypeSpecificSettings").getInt("roundCount"),
                                currentRound = gameData.getJSONObject("gameTypeSpecificSettings").getInt("currentRound"),
                                inputType = gameData.getJSONObject("gameTypeSpecificSettings").getString("inputType")
                            ),
                            players = gameData.getJSONArray("players").let { playerArray ->
                                List(playerArray.length()) { i ->
                                    Player(playerArray.getJSONObject(i).getString("inGameName"))
                                }
                            },
                            termDefinitionPairs = emptyList(), // Add if applicable
                            idGame = gameData.getInt("idGame"),
                            name = gameData.getString("name"),
                            gameStatus = gameData.getString("gameStatus"),
                            gameType = gameData.getString("gameType"),
                            playerCount = gameData.getInt("playerCount"),
                            maxPlayers = gameData.getInt("maxPlayers")
                        )
                    }

                    "JOIN_GAME" -> {
                        joinMessage = "You joined the game!"
                    }

                    "START_GAME" -> {
                        navigateToPage("GamePage")
                    }
                }
            }
        })

        // Send GET_GAME request to the server
        val setupRequest = JSONObject().apply {
            put("type", "GET_GAME")
            put("data", 267) // Replace 24 with your game ID
        }
        webSocketManager.sendMessage(setupRequest)
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Game Setup", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            gameSetup?.let { setup ->
                Text("Name: ${setup.name}")
                Text("Type: ${setup.gameType}")
                Text("Status: ${setup.gameStatus}")
                Text("Players: ${setup.playerCount} / ${setup.maxPlayers}")
                Text("Current Round: ${setup.gameTypeSpecificSettings.currentRound}")
                Text("Total Rounds: ${setup.gameTypeSpecificSettings.roundCount}")
                Text("Input Type: ${setup.gameTypeSpecificSettings.inputType}")

                Spacer(modifier = Modifier.height(16.dp))

                Text("Players in Lobby:")
                setup.players.forEach { player ->
                    Text("- ${player.inGameName}")
                }
            } ?: run {
                Text("Loading game data...")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input field for player name
            OutlinedTextField(
                value = playerName,
                onValueChange = { playerName = it },
                label = { Text("Enter your name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Join Game button
            Button(
                onClick = {
                    val joinRequest = JSONObject().apply {
                        put("type", "JOIN_GAME")
                        put("data", JSONObject().apply {
                            put("IdGame", gameSetup?.idGame ?: 24) // Replace 24 with your game ID
                            put("PlayerName", playerName)
                        })
                    }
                    webSocketManager.sendMessage(joinRequest)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Join Game")
            }

            if (joinMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(joinMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
