package cz.zlehcito.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.zlehcito.network.WebSocketManager
import org.json.JSONObject

@Composable
fun ResultsPage(webSocketManager: WebSocketManager) {
    var resultsData by remember { mutableStateOf("Waiting for results...") }

    LaunchedEffect(Unit) {
        webSocketManager.connect(object : okhttp3.WebSocketListener() {
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                resultsData = text
            }
        })

        // Request results data
        val resultsRequest = JSONObject()
        resultsRequest.put("type", "GET_RESULTS")
        webSocketManager.sendMessage(resultsRequest)
    }

    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Text("Results Page", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(resultsData)
        }
    }
}
