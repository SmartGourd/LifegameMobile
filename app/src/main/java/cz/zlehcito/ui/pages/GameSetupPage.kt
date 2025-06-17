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
import cz.zlehcito.viewmodel.GameSetupViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GameSetupPage(
    idGame: String, // Received from Navigation
    navigateToLobbyPage: () -> Unit,
    navigateToGamePage: (idGame: String, userId: String) -> Unit, // For navigation triggered by ViewModel event
    viewModel: GameSetupViewModel = koinViewModel(parameters = { parametersOf(idGame) }) // Pass idGame via parameters for SavedStateHandle
) {
    val gameSetupState by viewModel.gameSetupState.collectAsStateWithLifecycle()
    val gameKey by viewModel.gameKey.collectAsStateWithLifecycle()
    var playerName by rememberSaveable { mutableStateOf("") }

    // Observe navigation event from ViewModel
    val navigateToGameEvent by viewModel.navigateToGameAction.collectAsStateWithLifecycle()
    LaunchedEffect(navigateToGameEvent) {
        navigateToGameEvent?.data?.let { (idGame, userId) ->
            navigateToGamePage(idGame, userId)
            viewModel.consumeNavigationEvent() // Reset the event after navigation
        }
    }

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
                Text(stringResource(R.string.gamesetup_players, setup.players.count()))

                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.gamesetup_players_in_lobby))
                setup.players.forEach { player ->
                    Text("- ${player.inGameName}")
                }
            } ?: run {
                Text(stringResource(R.string.gamesetup_loading))
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (gameKey.idUser.isEmpty() || gameKey.keyType == "Invalid") {
                OutlinedTextField(
                    value = playerName,
                    onValueChange = { playerName = it },
                    label = { Text(stringResource(R.string.gamesetup_enter_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.sendJoinGameRequest(playerName) }, // Use ViewModel
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.gamesetup_join_game))
                }
            } else {
                Button(
                    onClick = { viewModel.sendLeaveGameRequest() }, // Use ViewModel
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.gamesetup_leave_game))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // If user is in game, send leave request.
                    if (gameKey.idUser.isNotEmpty() && gameKey.keyType != "Invalid") {
                        viewModel.sendLeaveGameRequest()
                    }
                    navigateToLobbyPage()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.gamesetup_back_to_lobby))
            }
        }
    }
}
