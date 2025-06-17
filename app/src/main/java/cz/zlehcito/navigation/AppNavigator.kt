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
                navigateToGameSetupPage = { idGame -> // Changed gameId to idGame for consistency
                    navController.navigate("${AppDestinations.GAME_SETUP}/$idGame")
                }
            )
        }
        composable(
            route = "${AppDestinations.GAME_SETUP}/{idGame}", // Changed gameId to idGame
            arguments = listOf(navArgument("idGame") { type = NavType.StringType }) // Changed gameId to idGame and type to StringType
        ) { backStackEntry ->
            val idGame = backStackEntry.arguments?.getString("idGame") ?: "" // Changed gameId to idGame and getInt to getString
            GameSetupPage(
                idGame = idGame,
                navigateToLobbyPage = {
                    navController.popBackStack(AppDestinations.LOBBY, inclusive = false)
                },
                navigateToGamePage = { gameId, userId -> // Parameters from GameSetupPage are idGame, idUser
                    // GameSetupModel.initializeModel is called within GameSetupPage now or will be handled there
                    navController.navigate("${AppDestinations.GAME}/$gameId/$userId") { // gameId here is the string idGame from GameSetupPage
                        // Pop up to lobby to prevent going back to game setup from game
                        popUpTo(AppDestinations.LOBBY)
                    }
                }
            )
        }
        composable(
            route = "${AppDestinations.GAME}/{idGame}/{idUser}", // Changed gameId to idGame and userId to idUser
            arguments = listOf(
                navArgument("idGame") { type = NavType.StringType }, // Changed gameId to idGame and type to StringType
                navArgument("idUser") { type = NavType.StringType } // Changed userId to idUser
            )
        ) { backStackEntry ->
            val idGame = backStackEntry.arguments?.getString("idGame") ?: "" // Changed gameId to idGame and getInt to getString
            val idUser = backStackEntry.arguments?.getString("idUser") ?: "" // Changed userId to idUser
            GamePage(
                idGame = idGame,
                idUser = idUser,
                navigateToLobbyPage = {
                    navController.popBackStack(AppDestinations.LOBBY, inclusive = false)
                }
            )
        }
    }
}
