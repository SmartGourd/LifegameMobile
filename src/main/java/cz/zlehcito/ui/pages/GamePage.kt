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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.zlehcito.model.appState.AppState
import cz.zlehcito.model.dtos.TermDefinitionPair
import cz.zlehcito.model.modelHandlers.GamePageModelHandler
import cz.zlehcito.model.viewModel.GamePageViewModel
import cz.zlehcito.model.viewModel.GamePageViewModelFactory

@Composable
fun GamePage(
    appState: AppState,
    navigateToPage: (String) -> Unit,
) {
    val gamePageViewModel: GamePageViewModel = viewModel(factory = GamePageViewModelFactory(appState))
    val gamePageModelHandler = gamePageViewModel.modelHandler
    LaunchedEffect(Unit) {
        gamePageModelHandler.initializeModel()
    }

    val gameSetupState by gamePageModelHandler.gameSetupState.collectAsStateWithLifecycle()
    val showResults by gamePageModelHandler.showResults.collectAsStateWithLifecycle()
    val playerFinalResults by gamePageModelHandler.playerFinalResults.collectAsStateWithLifecycle()
    val mistakeDictionary by gamePageModelHandler.mistakeDictionary.collectAsStateWithLifecycle()
    val secondsOfCountdown by gamePageModelHandler.secondsOfCountdown.collectAsStateWithLifecycle()
    val termDefinitionPairsQueueThisRound by gamePageModelHandler.termDefinitionPairsQueueThisRound.collectAsStateWithLifecycle()
    val currentTerm by gamePageModelHandler.currentTerm.collectAsStateWithLifecycle()
    val currentDefintion by gamePageModelHandler.currentDefinition.collectAsStateWithLifecycle()
    val answerCorrect by gamePageModelHandler.lastOneWasCorrect.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        println("showResults: $showResults")
        println("playerFinalResults: $playerFinalResults")
        println("secondsOfCountdown: $secondsOfCountdown")
        println("termDefinitionPairsQueueThisRound: $termDefinitionPairsQueueThisRound")
        when {
            showResults -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Results",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (playerFinalResults.isEmpty()) {
                        Text(
                            text = "Loading results",
                        )
                    } else {
                        var place = 1
                        playerFinalResults.sortedByDescending { it.points }.forEach { player ->
                            Text(
                                text = "${place}. ${player.inGameName}: ${player.points} points",
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            place++
                        }
                    }
                    Button(onClick = {
                        navigateToPage("Lobby")
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Back to Lobby")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Mistakes",
                        fontSize = 20.sp,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    mistakeDictionary.forEach { mistake ->
                        Text(
                            text = "${mistake.key} ${mistake.value} ${if (mistake.value > 1) "mistakes" else "mistake"}",
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            // Show countdown if secondsOfCountdown > 0
            secondsOfCountdown > 0 -> {
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

            gameSetupState?.inputType == "Writing" -> {
                // Define a background color based on the correctness state
                val backgroundColor = if (answerCorrect) Color (0xffccecCB) else Color(0xd3ffccCB) // pink
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
                        value = currentDefintion,
                        onValueChange = { gamePageModelHandler.setCurrentDefiniton(it)},
                        placeholder = { Text("Enter definition") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = false,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                gamePageModelHandler.checkDefinitionCorrectness()
                            }
                        )
                    )
                    // Submit button
                    Button (
                        onClick = {
                            gamePageModelHandler.checkDefinitionCorrectness()
                        },
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                    ) {
                        Text("Submit")
                    }
                }
            }

            // Show termDefinitionPairsQueueThisRound otherwise
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    if (termDefinitionPairsQueueThisRound.isEmpty()) {
                        Text(
                            text = "Game",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 30.dp, bottom = 16.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(180.dp))
                        GameScreen(
                            termDefinitionPairsQueueThisRound,
                            gamePageModelHandler
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameScreen(
    termDefinitionPairsQueueThisRound: List<TermDefinitionPair>,
    gamePageModelHandler: GamePageModelHandler
) {
    // Instead of storing just the text, store the index of the selected term and definition.
    var selectedTermIndex by remember { mutableStateOf<Int?>(null) }
    var selectedDefinitionIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Title
        Text(
            text = "Game",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 30.dp, bottom = 16.dp)
        )

        if (termDefinitionPairsQueueThisRound.isEmpty()) {
            Text(
                text = "No terms available!",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left column: Terms
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    itemsIndexed(termDefinitionPairsQueueThisRound.take(5)) { index, termDefinitionPair ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                                .clickable {
                                    selectedTermIndex = index
                                    // If both a term and a definition are selected, check the match
                                    if (selectedTermIndex != null && selectedDefinitionIndex != null) {
                                        val term = termDefinitionPairsQueueThisRound.take(5)[selectedTermIndex!!].term
                                        val definition = termDefinitionPairsQueueThisRound.take(5)[selectedDefinitionIndex!!].definition
                                        val isMatch = gamePageModelHandler.pairConnected(term, definition)
                                        // Reset selections after checking
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

                // Right column: Definitions
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    itemsIndexed(termDefinitionPairsQueueThisRound.take(5)) { index, termDefinitionPair ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                                .clickable {
                                    selectedDefinitionIndex = index
                                    // If both selections are made, check the match
                                    if (selectedTermIndex != null && selectedDefinitionIndex != null) {
                                        val term = termDefinitionPairsQueueThisRound.take(5)[selectedTermIndex!!].term
                                        val definition = termDefinitionPairsQueueThisRound.take(5)[selectedDefinitionIndex!!].definition
                                        val isMatch = gamePageModelHandler.pairConnected(term, definition)
                                        // Reset selections after checking
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
}
