package ecobuild_ai.app

import android.Manifest
import android.os.Build
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
import ecobuild_ai.app.viewmodel.AnalysisViewModel
import ecobuild_ai.app.viewmodel.ProfileViewModel
import ecobuild_ai.app.viewmodel.UploadViewModel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.navDeepLink
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel("sistema",this)
        enableEdgeToEdge()
        setContent {
            EcoBuildAITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph()
                }
            }
        }
    }
}

fun createNotificationChannel(channelId: String, context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Notificações do Sistema"
        val descriptionText = "Notificações de análises e uploads"
        val importance = android.app.NotificationManager.IMPORTANCE_HIGH
        val channel = android.app.NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
            enableVibration(true)
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun triggerNotificationUpload(context: Context) {
    val channelId = "sistema"
    createNotificationChannel(channelId, context)

    // 1. URI estática apontando diretamente para a rota da análise
    val deepLinkUri = "ecobuild-ai://ANALYSIS".toUri()

    // 2. Intent que carrega essa URI para a MainActivity
    val intent = Intent(
        Intent.ACTION_VIEW,
        deepLinkUri,
        context,
        MainActivity::class.java
    ).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // 3. Construir e disparar a notificação com alta prioridade para banner visível (Heads-up)
    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_logo_ecobuild_ai)
        .setContentTitle("Analysis Complete")
        .setContentText("The report is ready for review.")
        .setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)

    try {
        // Use a unique ID per call so each notification appears as a separate item
        // (static ID 2001 caused Android to replace/update the same notification slot)
        val notificationId = System.currentTimeMillis().toInt()
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    } catch (e: SecurityException) {
        Log.e("Notification", "No permission to send notification", e)
    }
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val uploadViewModel: UploadViewModel = viewModel()
    val analysisViewModel: AnalysisViewModel = viewModel()

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
            val profileViewModel: ProfileViewModel = viewModel(it)
            ProfileScreen(navController = navController, viewModel = profileViewModel)
        }
        composable(Routes.Upload) {
            UploadScreen(navController = navController, viewModel = uploadViewModel)
        }
        composable(Routes.Analysis, deepLinks  = listOf(navDeepLink { uriPattern = "ecobuild-ai://ANALYSIS"})) {
            AnalysisScreen(navController = navController, viewModel = analysisViewModel, uploadViewModel = uploadViewModel)
        }
        composable(Routes.NewChat) {
            NewChatScreen(navController = navController)
        }
    }
}
