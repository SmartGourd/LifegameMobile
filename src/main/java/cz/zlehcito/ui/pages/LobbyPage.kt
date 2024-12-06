package cz.zlehcito.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.zlehcito.network.WebSocketManager
import cz.zlehcito.model.dtos.Game
import cz.zlehcito.model.modelHandlers.LobbyModelHandler

@Composable
fun LobbyPage(
    webSocketManager: WebSocketManager,
    navigateToPage: (String, Int, Int)  -> Unit
) {
    val lobbyModelHandler = remember { LobbyModelHandler(webSocketManager) }
    val gamesList by lobbyModelHandler.gamesList.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    val filteredGamesList = gamesList.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Text(
                text = "Lobby",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp, bottom = 16.dp)
            )

            SearchBar(
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it }
            )

            if (gamesList.isEmpty()) {
                Text(
                    text = "Loading games ...",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (filteredGamesList.isEmpty()) {
                Text(
                    text = "Loading games ...",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(filteredGamesList) { index, game ->
                        GameItem(
                            game = game,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clickable { navigateToPage("GameSetup", game.idGame, 0)},
                            backgroundColor = if (index % 2 == 0) Color(0xFFBBDEFB) else Color(0xFFE1F5FE)

                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(searchQuery: String, onQueryChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        TextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = { Text(text = "Search games ...") },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(6.dp)),
            singleLine = true,
            colors = TextFieldDefaults.textFieldColors(
                focusedTextColor = Color.Black,
                cursorColor = Color(0xFFE1F5FE),
                focusedIndicatorColor = Color(0xFF2D7EC6), // Bottom border when focused
                unfocusedIndicatorColor = Color(0xFF75BDFF), // Bottom border when not focused
            )
        )
    }
}


@Composable
fun GameItem(game: Game, modifier: Modifier = Modifier, backgroundColor: Color) {
    Row(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(6.dp))
            .padding(16.dp)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = game.name,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        Text(
            text = "Players: ${game.playerCount}/${game.maxPlayers}",
            color = Color(0xFF37474F),
            fontSize = 16.sp,
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(
            text = "Type: ${game.gameType}",
            color = Color(0xFF37474F),
            fontSize = 16.sp
        )
    }
}