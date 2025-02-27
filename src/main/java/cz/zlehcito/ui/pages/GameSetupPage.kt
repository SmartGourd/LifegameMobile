package cz.zlehcito.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.zlehcito.R
import cz.zlehcito.model.modelHandlers.GameSetupModel

@Composable
fun GameSetupPage(
    navigateToLobbyPage: () -> Unit,
) {
    val gameSetupState by GameSetupModel.gameSetupState.collectAsStateWithLifecycle()
    val gameKey by GameSetupModel.gameKey.collectAsStateWithLifecycle()
    var playerName by rememberSaveable { mutableStateOf("") }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.gamesetup_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            gameSetupState?.let { setup ->
                Text(stringResource(R.string.gamesetup_name, setup.name))
                Text(stringResource(R.string.gamesetup_status, setup.gameStatus))
                Text(stringResource(R.string.gamesetup_players, setup.players.count(), setup.maxPlayers))
                Text(stringResource(R.string.gamesetup_total_rounds, setup.roundCount))
                Text(stringResource(R.string.gamesetup_input_type, setup.inputType))

                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.gamesetup_players_in_lobby))
                setup.players.forEach { player ->
                    Text("- ${player.inGameName}")
                }
            } ?: run {
                Text(stringResource(R.string.gamesetup_loading))
            }

            Spacer(modifier = Modifier.height(16.dp))

            gameKey.let { gameKey ->
                if (gameKey.idUser.isEmpty() || gameKey.keyType == "Invalid") {
                    // Input field for player name using a string resource label
                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { playerName = it },
                        label = { Text(stringResource(R.string.gamesetup_enter_name)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { GameSetupModel.sendJoinGameRequest(playerName) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.gamesetup_join_game))
                    }
                } else {
                    Button(
                        onClick = { GameSetupModel.sendLeaveGameRequest(gameKey.idUser) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.gamesetup_leave_game))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    GameSetupModel.sendLeaveGameRequest(gameKey.idUser)
                    navigateToLobbyPage()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.gamesetup_back_to_lobby))
            }
        }
    }
}
