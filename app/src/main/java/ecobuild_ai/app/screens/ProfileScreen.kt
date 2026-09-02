package ecobuild_ai.app.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import ecobuild_ai.app.R
import ecobuild_ai.app.constant.Routes
import ecobuild_ai.app.navigation.BottomNavBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = remember(auth) { auth.currentUser }
    
    if (currentUser == null) {
        LaunchedEffect(Unit) {
            navController.navigate(Routes.Login) {
                popUpTo(Routes.Profile) { inclusive = true }
            }
        }
        return
    }

    val isGoogleUser = remember(currentUser) {
        currentUser.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
    }

    // Estados dos campos
    var name by remember { mutableStateOf(currentUser.displayName ?: "") }
    var email by remember { mutableStateOf(currentUser.email ?: "") }
    val photoUrl = remember(currentUser) { currentUser.photoUrl }

    // Estado original para detectar mudanças
    val originalName = remember { currentUser.displayName ?: "" }

    val hasChanges = remember(name) {
        name != originalName }

    // Validação básica
    val isNameValid = name.length >= 3
    val canSave = hasChanges && isNameValid

    val snackbarHostState = remember { SnackbarHostState() }
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface
                )
            )
        },
        bottomBar = { BottomNavBar(navController, Routes.Profile) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = canSave,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        // Lógica de salvar (simulada ou Firebase updateProfile)
                        // TODO: Implementar updateProfile real
                    },
                    icon = { Icon(Icons.Default.Save, null) },
                    text = { Text(stringResource(R.string.profile_save_button)) },
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Seção da Foto
            Box(contentAlignment = Alignment.BottomEnd) {
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (!isGoogleUser) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primary)
                            .clickable { /* Abrir galeria */ }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, null, tint = colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                    }
                } else {
                    // Badge do Google
                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(32.dp).offset(x = (-4).dp, y = (-4).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Google Verified",
                            tint = Color(0xFF4285F4),
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }

            if (isGoogleUser) {
                Text(
                    text = stringResource(R.string.profile_google_account_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(colorScheme.primaryContainer, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Campo Nome
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.profile_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = !isNameValid && name.isNotEmpty(),
                supportingText = {
                    if (!isNameValid && name.isNotEmpty()) {
                        Text(stringResource(R.string.profile_invalid_name))
                    }
                },
                leadingIcon = { Icon(Icons.Default.Person, null) }
            )

            // Campo Email (Bloqueado se Google)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.profile_email_label)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGoogleUser,
                readOnly = isGoogleUser,
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Email, null) },
                colors = if (isGoogleUser) {
                    OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = colorScheme.outlineVariant,
                        disabledLabelColor = colorScheme.onSurfaceVariant,
                        disabledTextColor = colorScheme.onSurfaceVariant
                    )
                } else OutlinedTextFieldDefaults.colors()
            )
        }
    }
}
