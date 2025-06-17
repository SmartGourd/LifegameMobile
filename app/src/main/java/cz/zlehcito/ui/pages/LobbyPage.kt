package cz.zlehcito.ui.pages

import android.util.Log
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.zlehcito.R
import cz.zlehcito.model.LobbyGameForList
import cz.zlehcito.viewmodel.LobbyViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun LobbyPage(
    navigateToGameSetupPage: (idString: String) -> Unit,
    viewModel: LobbyViewModel = koinViewModel() // Injected ViewModel
) {
    val gamesList by viewModel.gamesList.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    val filteredGamesList = gamesList.filter {
        it.name.contains(searchQuery, ignoreCase = true) && it.gameType == "Race"
    }

    // Call subscription method when LobbyPage becomes active
    LaunchedEffect(Unit) {
        viewModel.sendSubscriptionPutLobbyRequest()
        viewModel.sendGetGamesRequest()
    }

    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Text(
                text = stringResource(R.string.lobby_title),
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
                    text = stringResource(R.string.lobby_loading_games),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (filteredGamesList.isEmpty()) {
                Text(
                    text = stringResource(R.string.lobby_no_games_found), // Consider a different string for no results after search
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
                                .clickable {
                                    navigateToGameSetupPage(game.idString)
                                },
                            backgroundColor = if (index % 2 == 0)
                                colorResource(id = R.color.lighter_blue)
                            else
                                colorResource(id = R.color.darker_blue)
                        )
                    }
                }
            }
        }
    }
}

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
            placeholder = { Text(text = stringResource(R.string.searchbar_placeholder)) },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(6.dp)),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = colorResource(id = R.color.lighter_blue),
                unfocusedIndicatorColor = colorResource(id = R.color.lighter_blue)
            )
        )
    }
}


@Composable
fun GameItem(game: LobbyGameForList, modifier: Modifier = Modifier, backgroundColor: Color) {
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
            text = stringResource(R.string.gameitem_players, game.playerCount),
            color = colorResource(id = R.color.text_grey),
            fontSize = 16.sp,
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(
            text = stringResource(R.string.gameitem_type, game.gameType),
            color = colorResource(id = R.color.text_grey),
            fontSize = 16.sp
        )
    }
}
