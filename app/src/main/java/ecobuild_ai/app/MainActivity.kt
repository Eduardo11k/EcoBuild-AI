package ecobuild_ai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ecobuild_ai.app.constant.Routes
import ecobuild_ai.app.screens.*
import ecobuild_ai.app.ui.theme.EcoBuildAITheme
import ecobuild_ai.app.viewmodel.UploadViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EcoBuildAITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EcoBuild_AI()
                }
            }
        }
    }
}

@Composable
fun EcoBuild_AI() {
    AppNavGraph()
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val uploadViewModel: UploadViewModel = viewModel()

    NavHost(navController = navController, startDestination = Routes.Login) {
        composable(Routes.Home) {
            HomeScreen(navController = navController)
        }
        composable(Routes.Login) {
            LoginScreen(navController = navController)
        }
        composable(Routes.Register) {
            RegisterScreen(navController = navController)
        }
        composable(Routes.Profile) {
            ProfileScreen(navController = navController)
        }
        composable(Routes.Upload) {
            UploadScreen(navController = navController, viewModel = uploadViewModel)
        }
        composable(Routes.Analysis) {
            AnalysisScreen(navController = navController)
        }
        composable(Routes.NewChat) {
            NewChatScreen(navController = navController)
        }
    }
}
