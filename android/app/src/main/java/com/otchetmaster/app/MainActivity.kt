package com.otchetmaster.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.otchetmaster.app.ui.home.HomeScreen
import com.otchetmaster.app.ui.home.HomeViewModelFactory
import com.otchetmaster.app.ui.job.JobDetailsScreen
import com.otchetmaster.app.ui.job.NewJobScreen
import com.otchetmaster.app.ui.screens.PlaceholderScreen
import com.otchetmaster.app.ui.screens.ProfileScreen
import com.otchetmaster.app.ui.theme.OtchetMasterTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as OtchetMasterApplication
        setContent {
            OtchetMasterTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    OtchetMasterApp(app = app)
                }
            }
        }
    }
}

@Composable
fun OtchetMasterApp(app: OtchetMasterApplication) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val profile by app.profileRepository.profile.collectAsState(initial = null)
    var profileLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        if (profile != null) profileLoaded = true
    }

    val startDestination = if (profileLoaded && profile != null) "home" else "profile"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("profile") {
            ProfileScreen(
                onSave = { name, phone, city ->
                    scope.launch {
                        app.profileRepository.save(name, phone, city)
                    }
                    navController.navigate("home") {
                        popUpTo("profile") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                viewModelFactory = HomeViewModelFactory(app.updateManager, app.jobRepository),
                onNewJob = { navController.navigate("new-job") },
                onJobClick = { jobId ->
                    navController.navigate("job/$jobId") {
                        popUpTo("home")
                    }
                }
            )
        }
        composable("new-job") {
            NewJobScreen(
                jobRepository = app.jobRepository,
                onJobCreated = { jobId ->
                    navController.navigate("job/$jobId") {
                        popUpTo("home")
                    }
                }
            )
        }
        composable("job/{jobId}") { entry ->
            val jobId = entry.arguments?.getString("jobId").orEmpty()
            JobDetailsScreen(
                jobId = jobId,
                jobRepository = app.jobRepository,
                photoRepository = app.photoRepository,
                reportRepository = app.reportRepository,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
