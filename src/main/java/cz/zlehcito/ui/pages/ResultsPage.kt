package cz.zlehcito.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.zlehcito.model.appState.AppState
import org.json.JSONObject

@Composable
fun ResultsPage(
    appState: AppState,
    navigateToPage: (String) -> Unit,
) {
    var resultsData by remember { mutableStateOf("Waiting for results...") }

    LaunchedEffect(Unit) {

        // Request results data
        val resultsRequest = JSONObject()
        resultsRequest.put("type", "GET_RESULTS")
        appState.webSocketManager.sendMessage(resultsRequest)
    }

    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Text("Results Page", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(resultsData)
        }
    }
}
