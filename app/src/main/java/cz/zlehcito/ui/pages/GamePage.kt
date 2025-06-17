package cz.zlehcito.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.zlehcito.model.RacePlayerResult
import cz.zlehcito.model.TermDefinitionPair
import cz.zlehcito.viewmodel.GamePageViewModel

import cz.zlehcito.R
import androidx.compose.ui.res.stringResource
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GamePage(
    idGame: String,
    idUser: String,
    navigateToLobbyPage: () -> Unit,
    viewModel: GamePageViewModel = koinViewModel(parameters = { parametersOf(idGame, idUser) })
) {
    val inputType by viewModel.inputType.collectAsStateWithLifecycle()
    val showCountdown by viewModel.showCountdown.collectAsStateWithLifecycle()
    val countdownSeconds by viewModel.countdownSeconds.collectAsStateWithLifecycle()
    val displayResults by viewModel.displayResults.collectAsStateWithLifecycle()
    val playerFinalResults by viewModel.playerFinalResults.collectAsStateWithLifecycle()
    val mistakePairs by viewModel.mistakePairs.collectAsStateWithLifecycle()

    // Navigate to Lobby when triggered by ViewModel
    LaunchedEffect(viewModel.navigateToLobby) {
        viewModel.navigateToLobby.collect { shouldNavigate ->
            if (shouldNavigate) {
                navigateToLobbyPage()
                viewModel.onNavigationDone()
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.sendGetGameRequest()
    }


    Box(modifier = Modifier.fillMaxSize()) {
        when {
            displayResults -> {
                ResultsScreen(
                    onNavigateToLobby = { viewModel.onNavigateToLobbyClicked() },
                    playerFinalResults = playerFinalResults,
                    mistakePairs = mistakePairs
                )
            }
            showCountdown -> {
                CountdownScreen(seconds = countdownSeconds)
            }
            inputType == "Writing" -> {
                val writingUiState by viewModel.writingGameManager.uiState.collectAsStateWithLifecycle()
                WritingScreen(
                    currentTerm = writingUiState.currentTerm ?: stringResource(R.string.gamepage_waiting_for_term),
                    userResponse = writingUiState.userResponse,
                    onUserResponseChange = { viewModel.setWritingUserResponse(it) },
                    onSubmit = { viewModel.submitWritingAnswer() },
                    isWrong = writingUiState.isWrongAnswer,
                    isCorrect = writingUiState.isCorrectAnswer
                )
            }
            inputType == "Connecting" -> {
                val connectingUiState by viewModel.connectingGameManager.uiState.collectAsStateWithLifecycle()
                ConnectingScreen(
                    displayedTerms = connectingUiState.displayedTerms,
                    displayedDefinitions = connectingUiState.displayedDefinitions,
                    onTermSelected = { viewModel.selectConnectingTerm(it) },
                    onDefinitionSelected = { viewModel.selectConnectingDefinition(it) },
                    selectedTerm = connectingUiState.selectedTerm,
                    selectedDefinition = connectingUiState.selectedDefinition,
                    connectedCount = connectingUiState.connectedCount,
                    mistakesCount = connectingUiState.mistakesCount,
                    totalPairsInRound = connectingUiState.totalPairsInRound,
                    feedback = connectingUiState.feedback
                )
            }
            else -> {
                // Loading state or placeholder if gameDetails or inputType is not yet available
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.gamepage_loading_game_details))
                }
            }
        }
    }
}

@Composable
fun ResultsScreen(
    onNavigateToLobby: () -> Unit,
    playerFinalResults: List<RacePlayerResult>,
    mistakePairs: Map<String, Int>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.gamepage_result_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(
            onClick = onNavigateToLobby,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text(text = stringResource(id = R.string.gamepage_result_back_to_lobby))
        }

        if (playerFinalResults.isEmpty()) {
            Text(text = stringResource(id = R.string.gamepage_result_loading))
        } else {
            Text(stringResource(R.string.gamepage_result_player_scores), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            playerFinalResults.sortedByDescending { it.points }.forEachIndexed { index, player ->
                Text(
                    text = "${index + 1}. ${player.inGameName}: ${player.points} ${stringResource(id = R.string.gamepage_result_points)}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        if (mistakePairs.isNotEmpty()){
            Text(stringResource(R.string.gamepage_result_mistakes_summary), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            mistakePairs.forEach { (term, count) ->
                Text(
                    text = "'$term': $count ${stringResource(if (count > 1) R.string.gamepage_result_mistakes else R.string.gamepage_result_mistake)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun CountdownScreen(seconds: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.9f)), // Semi-transparent white background
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = seconds.toString(),
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.Black
        )
    }
}

@Composable
fun WritingScreen(
    currentTerm: String,
    userResponse: String,
    onUserResponseChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isWrong: Boolean,
    isCorrect: Boolean
) {
    val backgroundColor = when {
        isCorrect -> colorResource(id = R.color.correct_background)
        isWrong -> colorResource(id = R.color.incorrect_background)
        else -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = currentTerm,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        TextField(
            value = userResponse,
            onValueChange = onUserResponseChange,
            label = { Text(stringResource(R.string.gamepage_writing_enter_definition)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.Sentences
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier.fillMaxWidth(0.8f),
            isError = isWrong
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSubmit) {
            Text(stringResource(R.string.gamepage_submit))
        }
    }
}

@Composable
fun ConnectingScreen(
    displayedTerms: List<TermDefinitionPair>,
    displayedDefinitions: List<TermDefinitionPair>,
    onTermSelected: (TermDefinitionPair) -> Unit,
    onDefinitionSelected: (TermDefinitionPair) -> Unit,
    selectedTerm: TermDefinitionPair?,
    selectedDefinition: TermDefinitionPair?,
    connectedCount: Int,
    mistakesCount: Int,
    totalPairsInRound: Int,
    feedback: String? // "correct" or "incorrect"
) {
    val baseBackgroundColor = MaterialTheme.colorScheme.background
    val feedbackColor = when (feedback) {
        "correct" -> colorResource(id = R.color.correct_background)
        "incorrect" -> colorResource(id = R.color.incorrect_background)
        else -> baseBackgroundColor
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(feedbackColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.gamepage_connecting_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Terms Column
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(stringResource(R.string.gamepage_connecting_terms), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                if (displayedTerms.isEmpty() && totalPairsInRound > 0 && connectedCount == totalPairsInRound) {
                     Text(stringResource(R.string.gamepage_connecting_round_cleared), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
                } else if (displayedTerms.isEmpty()) {
                    Text(stringResource(R.string.gamepage_connecting_no_terms), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
                } else {
                    LazyColumn {
                        items(displayedTerms, key = { it.term }) { termPair ->
                            SelectableItem(
                                text = termPair.term,
                                isSelected = termPair.term == selectedTerm?.term,
                                onClick = { onTermSelected(termPair) }
                            )
                        }
                    }
                }
            }

            // Definitions Column
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(stringResource(R.string.gamepage_connecting_definitions), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                 if (displayedDefinitions.isEmpty() && totalPairsInRound > 0 && connectedCount == totalPairsInRound) {
                    // Text("Round Cleared!", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) // Already shown in terms
                } else if (displayedDefinitions.isEmpty()) {
                     Text(stringResource(R.string.gamepage_connecting_no_definitions), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
                } else {
                    LazyColumn {
                        items(displayedDefinitions, key = { it.definition }) { defPair ->
                            SelectableItem(
                                text = defPair.definition,
                                isSelected = defPair.definition == selectedDefinition?.definition,
                                onClick = { onDefinitionSelected(defPair) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("${stringResource(R.string.gamepage_connecting_connected)}: $connectedCount / $totalPairsInRound", style = MaterialTheme.typography.bodyLarge)
        Text("${stringResource(R.string.gamepage_connecting_mistakes)}: $mistakesCount", style = MaterialTheme.typography.bodyLarge)
        val donePercentage = if (totalPairsInRound > 0) (connectedCount * 100 / totalPairsInRound) else 0
        Text("${stringResource(R.string.gamepage_connecting_done)}: $donePercentage%", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun SelectableItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.medium
            )
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}
