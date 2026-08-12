package com.otchetmaster.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.otchetmaster.app.ui.home.HomeScreen
import com.otchetmaster.app.ui.home.HomeViewModelFactory
import com.otchetmaster.app.ui.screens.PlaceholderScreen
import com.otchetmaster.app.ui.screens.ProfileScreen
import com.otchetmaster.app.ui.theme.OtchetMasterTheme
import com.otchetmaster.app.updater.UpdateManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OtchetMasterTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    OtchetMasterApp(updateManager = UpdateManager(applicationContext))
                }
            }
        }
    }
}

@Composable
fun OtchetMasterApp(updateManager: UpdateManager) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "profile") {
        composable("profile") {
            ProfileScreen(
                onContinue = { navController.navigate("home") }
            )
        }
        composable("home") {
            HomeScreen(
                viewModelFactory = HomeViewModelFactory(updateManager),
                onNewJob = { navController.navigate("new-job") }
            )
        }
        composable("new-job") {
            PlaceholderScreen("Экран новой работы — будет дальше")
        }
    }
}
