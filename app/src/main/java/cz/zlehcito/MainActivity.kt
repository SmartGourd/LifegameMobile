package cz.zlehcito

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import cz.zlehcito.navigation.AppNavigator
import cz.zlehcito.network.WebSocketManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebSocketManager.connect()

        setContent {
            val navController = rememberNavController()
            AppNavigator(navController = navController)
        }
    }
}
