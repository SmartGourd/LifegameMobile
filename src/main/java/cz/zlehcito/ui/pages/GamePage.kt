package cz.zlehcito.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.zlehcito.model.appState.AppState
import cz.zlehcito.model.viewModel.GamePageViewModel
import cz.zlehcito.model.viewModel.GamePageViewModelFactory

@Composable
fun GamePage(
    appState: AppState,
    navigateToPage: (String) -> Unit,
) {
    val gamePageViewModel: GamePageViewModel = viewModel(factory = GamePageViewModelFactory(appState))
    val gamePageModel = gamePageViewModel.modelHandler

    val showResults by gamePageModel.showResults.collectAsStateWithLifecycle()
    val playerFinalResults by gamePageModel.playerFinalResults.collectAsStateWithLifecycle()
    val secondsOfCountdown by gamePageModel.secondsOfCountdown.collectAsStateWithLifecycle()
    val termDefinitionPairsQueueThisRound by gamePageModel.termDefinitionPairsQueueThisRound.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Text(
                text = "Game",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp, bottom = 16.dp)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        println("showResults: $showResults")
        println("playerFinalResults: $playerFinalResults")
        println("secondsOfCountdown: $secondsOfCountdown")
        println("termDefinitionPairsQueueThisRound: $termDefinitionPairsQueueThisRound")
        when {
            // Show results if showResults is true
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
                    playerFinalResults.sortedByDescending { it.points }.forEach { player ->
                        Text(
                            text = "${player.inGameName}: ${player.points}",
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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
                        termDefinitionPairsQueueThisRound.forEach { termDefinitionPair ->
                            Text(
                                text = "${termDefinitionPair.term}: ${termDefinitionPair.definition}",
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}
