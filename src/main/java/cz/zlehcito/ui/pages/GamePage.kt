package cz.zlehcito.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.zlehcito.model.entities.RacePlayerResult
import cz.zlehcito.model.entities.TermDefinitionPair
import cz.zlehcito.model.modelHandlers.GamePageModel

import cz.zlehcito.R
import androidx.compose.ui.res.stringResource

@Composable
fun GamePage(
    navigateToLobbyPage: () -> Unit,
) {
    val gameSetupState by GamePageModel.gameSetupState.collectAsStateWithLifecycle()
    val showResults by GamePageModel.showResults.collectAsStateWithLifecycle()
    val playerFinalResults by GamePageModel.playerFinalResults.collectAsStateWithLifecycle()
    val mistakeDictionary by GamePageModel.mistakeDictionary.collectAsStateWithLifecycle()
    val secondsOfCountdown by GamePageModel.secondsOfCountdown.collectAsStateWithLifecycle()
    val termDefinitionPairsQueueThisRound by GamePageModel.termDefinitionPairsQueueThisRound.collectAsStateWithLifecycle()
    val currentTerm by GamePageModel.currentTerm.collectAsStateWithLifecycle()
    val currentDefinition by GamePageModel.currentDefinition.collectAsStateWithLifecycle()
    val answerCorrect by GamePageModel.lastOneWasCorrect.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        val backgroundColor = if (answerCorrect) {
            colorResource(id = R.color.correct_background)
        } else {
            colorResource(id = R.color.incorrect_background)
        }
        when {
            showResults -> {
                ResultsScreen(
                    navigateToLobbyPage = navigateToLobbyPage,
                    playerFinalResults = playerFinalResults,
                    mistakeDictionary = mistakeDictionary
                )
            }

            secondsOfCountdown > 0 -> {
                CountdownScreen(secondsOfCountdown = secondsOfCountdown)
            }

            gameSetupState?.inputType == "Writing" -> {
                WritingScreen(
                    currentTerm = currentTerm,
                    currentDefinition = currentDefinition,
                    onDefinitionChange = { GamePageModel.setCurrentDefinition(it) },
                    onSubmit = { GamePageModel.checkDefinitionCorrectness() },
                    backgroundColor = backgroundColor
                )
            }

            else -> {
                ConnectingScreen(
                    termDefinitionPairsQueueThisRound = termDefinitionPairsQueueThisRound,
                    gamePageModel = GamePageModel,
                    backgroundColor = backgroundColor
                )
            }
        }
    }
}

@Composable
fun ResultsScreen(
    navigateToLobbyPage: () -> Unit,
    playerFinalResults: List<RacePlayerResult>,
    mistakeDictionary: Map<String, Int>
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
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (playerFinalResults.isEmpty()) {
            Text(text = stringResource(id = R.string.gamepage_result_loading))
        } else {
            var place = 1
            playerFinalResults.sortedByDescending { it.points }.forEach { player ->
                Text(
                    text = "$place. ${player.inGameName}: ${player.points} ${stringResource(id = R.string.gamepage_result_points)}",
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                place++
            }
        }
        Button(
            onClick = { navigateToLobbyPage() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.gamepage_result_back_to_lobby))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(id = R.string.gamepage_result_mistakes_title), fontSize = 20.sp)
        Spacer(modifier = Modifier.height(10.dp))
        mistakeDictionary.forEach { mistake ->
            Text(
                text = "${mistake.key} ${mistake.value} ${
                    if (mistake.value > 1) stringResource(id = R.string.gamepage_result_mistakes) 
                    else stringResource(id = R.string.gamepage_result_mistake)
                }",
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
fun CountdownScreen(secondsOfCountdown: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = secondsOfCountdown.toString(),
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

@Composable
fun WritingScreen(
    currentTerm: String,
    currentDefinition: String,
    onDefinitionChange: (String) -> Unit,
    onSubmit: () -> Unit,
    backgroundColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(180.dp))
        Text(
            text = currentTerm,
            fontSize = 28.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 30.dp, bottom = 16.dp)
        )
        TextField(
            value = currentDefinition,
            onValueChange = onDefinitionChange,
            placeholder = { Text(text = stringResource(id = R.string.gamepage_writing_enter_definition)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSubmit() }
            )
        )
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.gamepage_writing_submit))
        }
    }
}


@Composable
fun ConnectingScreen(
    termDefinitionPairsQueueThisRound: List<TermDefinitionPair>,
    gamePageModel: GamePageModel,
    backgroundColor: Color
) {
    var selectedTermIndex by remember { mutableStateOf<Int?>(null) }
    var selectedDefinitionIndex by remember { mutableStateOf<Int?>(null) }

    // Get the first five pairs.
    val termsToDisplay = termDefinitionPairsQueueThisRound.take(5)
    // Shuffle the definitions for random order.
    val definitionsShuffled = remember(termsToDisplay) { termsToDisplay.shuffled() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(300.dp))
        Text(
            text = stringResource(id = R.string.gamepage_connecting_game_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 30.dp, bottom = 16.dp)
        )
        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                itemsIndexed(termsToDisplay) { index, termDefinitionPair ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .clickable {
                                selectedTermIndex = index
                                if (selectedDefinitionIndex != null) {
                                    val term = termsToDisplay[selectedTermIndex!!].term
                                    val definition = definitionsShuffled[selectedDefinitionIndex!!].definition
                                    gamePageModel.pairConnected(term, definition)
                                    selectedTermIndex = null
                                    selectedDefinitionIndex = null
                                }
                            }
                            .background(
                                if (selectedTermIndex == index) Color.LightGray else Color.White
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = termDefinitionPair.term,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                itemsIndexed(definitionsShuffled) { index, termDefinitionPair ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .clickable {
                                selectedDefinitionIndex = index
                                if (selectedTermIndex != null) {
                                    val term = termsToDisplay[selectedTermIndex!!].term
                                    val definition = definitionsShuffled[selectedDefinitionIndex!!].definition
                                    gamePageModel.pairConnected(term, definition)
                                    selectedTermIndex = null
                                    selectedDefinitionIndex = null
                                }
                            }
                            .background(
                                if (selectedDefinitionIndex == index) Color.LightGray else Color.White
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = termDefinitionPair.definition,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
