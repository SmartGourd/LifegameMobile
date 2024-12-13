package cz.zlehcito.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.zlehcito.model.appState.AppState
import cz.zlehcito.model.viewModel.GameSetupViewModel
import cz.zlehcito.model.viewModel.GameSetupViewModelFactory

@Composable
fun GameSetupPage(
    appState: AppState,
    navigateToPage: (String) -> Unit
) {
    val viewModel: GameSetupViewModel = viewModel(
        factory = GameSetupViewModelFactory(appState, navigateToPage)
    )
    val gameSetupModelHandler = viewModel.gameSetupModelHandler
    gameSetupModelHandler.sendSubscriptionPutGameSetupRequest()
    gameSetupModelHandler.sendGetGameRequest()

    val gameSetupState by gameSetupModelHandler.gameSetupState.collectAsStateWithLifecycle()
    val gameKey by gameSetupModelHandler.gameKey.collectAsStateWithLifecycle()
    var playerName by rememberSaveable { mutableStateOf("") }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Game Setup", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // Display game setup information if available
            gameSetupState?.let { setup ->
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



            gameKey.let { gameKey ->
                if (gameKey.idUser.isEmpty() || gameKey.keyType == "Invalid") {
                    // Input field for player name
                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { playerName = it },
                        label = { Text("Enter your name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            gameSetupModelHandler.sendJoinGameRequest(playerName)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Join Game")
                    }
                } else {
                    Button(
                        onClick = {
                            gameSetupModelHandler.sendLeaveGameRequest(gameKey.idUser)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Leave Game")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Back to Lobby button
            Button(onClick = {
                gameSetupModelHandler.sendLeaveGameRequest(gameKey.idUser)
                navigateToPage("Lobby")
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Lobby")
            }
        }
    }
}
