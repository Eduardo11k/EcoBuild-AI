package ecobuild_ai.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import ecobuild_ai.app.R
import ecobuild_ai.app.constant.Routes

@Composable
fun BottomNavBar(navController: NavController, rotaActual: String) {
    val colorScheme = MaterialTheme.colorScheme

    NavigationBar {
        NavigationBarItem(
            selected = rotaActual == Routes.Home,
            onClick = { navController.navigate(Routes.Home) },
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text(stringResource(R.string.home), color = colorScheme.onSurface) }
        )
        NavigationBarItem(
            selected = rotaActual == Routes.NewChat,
            onClick = { navController.navigate(Routes.NewChat)},
            icon = { Icon(Icons.Default.AddCircleOutline, null) },
            label = { Text(stringResource(R.string.newChat), color = colorScheme.onSurface) }
        )
        NavigationBarItem(
            selected = rotaActual == Routes.Profile,
            onClick = { navController.navigate(Routes.Profile) },
            icon = { Icon(Icons.Default.Person, null) },
            label = { Text(stringResource(R.string.profile), color = colorScheme.onSurface) }
        )
    }
}