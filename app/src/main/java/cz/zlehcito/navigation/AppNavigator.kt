package cz.zlehcito.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import cz.zlehcito.ui.pages.GamePage
import cz.zlehcito.ui.pages.GameSetupPage
import cz.zlehcito.ui.pages.LobbyPage

object AppDestinations {
    const val LOBBY = "lobby"
    const val GAME_SETUP = "gameSetup"
    const val GAME = "game"
}

@Composable
fun AppNavigator(navController: NavHostController) {
    NavHost(navController = navController, startDestination = AppDestinations.LOBBY) {
        composable(AppDestinations.LOBBY) {
            LobbyPage(
                navigateToGameSetupPage = { gameId ->
                    navController.navigate("${AppDestinations.GAME_SETUP}/$gameId")
                }
            )
        }
        composable(
            route = "${AppDestinations.GAME_SETUP}/{gameId}",
            arguments = listOf(navArgument("gameId") { type = NavType.IntType })
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getInt("gameId") ?: 0
            GameSetupPage(
                idGame = gameId,
                navigateToLobbyPage = {
                    navController.popBackStack(AppDestinations.LOBBY, inclusive = false)
                },
                navigateToGamePage = { idGame, idUser ->
                    // GameSetupModel.initializeModel is called within GameSetupPage now or will be handled there
                    navController.navigate("${AppDestinations.GAME}/$idGame/$idUser") {
                        // Pop up to lobby to prevent going back to game setup from game
                        popUpTo(AppDestinations.LOBBY)
                    }
                }
            )
        }
        composable(
            route = "${AppDestinations.GAME}/{gameId}/{userId}",
            arguments = listOf(
                navArgument("gameId") { type = NavType.IntType },
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getInt("gameId") ?: 0
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            GamePage(
                idGame = gameId,
                idUser = userId,
                navigateToLobbyPage = {
                    navController.popBackStack(AppDestinations.LOBBY, inclusive = false)
                }
            )
        }
    }
}
