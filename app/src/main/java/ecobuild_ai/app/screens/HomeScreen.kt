package ecobuild_ai.app.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import ecobuild_ai.app.R
import ecobuild_ai.app.constant.Routes
import ecobuild_ai.app.navigation.BottomNavBar
import ecobuild_ai.app.ui.theme.MontserratFont
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser

    if (currentUser == null) {
        navController.navigate(Routes.Login) {
            popUpTo(Routes.Home) { inclusive = true }
        }
        return
    }

    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val credentialManager = remember { CredentialManager.create(context) }
    val isGoogleUser = currentUser.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }

    fun performLogout() {
        auth.signOut()
        coroutineScope.launch {
            if(isGoogleUser) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            navController.navigate(Routes.Login) {
                popUpTo(Routes.Home) { inclusive = true }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Opção 1", modifier = Modifier.padding(16.dp))
                Text("Opção 2", modifier = Modifier.padding(16.dp))
            }
        }
    )
    {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            fontFamily = MontserratFont,
                            color = colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {coroutineScope.launch { drawerState.open()}}) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu icon",
                                tint = colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { performLogout() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Log out",
                                tint = colorScheme.error
                            )
                        }
                    }
                )
            },
            bottomBar = { BottomNavBar(navController, Routes.Home) }
            ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}
