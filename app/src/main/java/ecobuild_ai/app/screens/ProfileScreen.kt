package ecobuild_ai.app.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import ecobuild_ai.app.R
import ecobuild_ai.app.constant.Routes
import ecobuild_ai.app.navigation.BottomNavBar
import ecobuild_ai.app.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, viewModel: ProfileViewModel) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val colorScheme = MaterialTheme.colorScheme
    val userData by viewModel.userData.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val success by viewModel.saveSuccess.collectAsStateWithLifecycle()

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

    fun performLogout() {
        auth.signOut()
        coroutineScope.launch {
            try {
                if (isGoogleUser) {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            navController.navigate(Routes.Login) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val isLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf(userData?.fullName?.ifEmpty { null } ?: currentUser.displayName ?: "") }
    var email by remember { mutableStateOf(userData?.email?.ifEmpty { null } ?: currentUser.email ?: "") }
    var photoUrl by remember { mutableStateOf(userData?.profileImageUrl?.ifEmpty { null } ?: currentUser.photoUrl?.toString() ?: "") }

    var userModifiedName by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser.uid) {
        viewModel.refreshUserData()
    }

    val isNameValid = name.trim().length >= 3

    val currentSavedName = userData?.fullName?.ifEmpty { null } ?: currentUser.displayName ?: ""
    val currentSavedEmail = userData?.email?.ifEmpty { null } ?: currentUser.email ?: ""
    val currentSavedPhoto = userData?.profileImageUrl?.ifEmpty { null } ?: currentUser.photoUrl?.toString() ?: ""

    val hasChanges = remember(name, email, photoUrl, userData, currentUser) {
        name.trim() != currentSavedName.trim() ||
        email.trim() != currentSavedEmail.trim() ||
        photoUrl != currentSavedPhoto
    }

    val canSave = hasChanges && isNameValid && !isSaving

    val snackbarHostState = remember { SnackbarHostState() }
    val updatedProfileMsg = stringResource(R.string.profile_update_success)

    LaunchedEffect(userData, isLoaded) {
        if (!isLoaded) return@LaunchedEffect

        val firestoreName = userData?.fullName ?: ""
        if (!userModifiedName) {
            name = if (firestoreName.isNotEmpty()) firestoreName else (currentUser.displayName ?: "")
        }

        val firestoreEmail = userData?.email ?: ""
        if (firestoreEmail.isNotEmpty()) {
            email = firestoreEmail
        }

        val firestorePhoto = userData?.profileImageUrl ?: ""
        if (firestorePhoto.isNotEmpty()) {
            photoUrl = firestorePhoto
        }
    }

    LaunchedEffect(success) {
        if (success) {
            Toast.makeText(context, updatedProfileMsg, Toast.LENGTH_SHORT).show()
            viewModel.resetSaveSuccess()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { performLogout() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.Logout),
                            tint = colorScheme.error
                        )
                    }
                }
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
                        viewModel.updateProfile(name, email, photoUrl)
                    },
                    icon = {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, null)
                        }
                    },
                    text = { Text(if (isSaving) "Saving..." else stringResource(R.string.profile_save_button)) },
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
            Box(contentAlignment = Alignment.BottomEnd) {
                if (photoUrl.isNotEmpty()) {
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
                    val r = Random.nextInt(3)
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                             painter = if (r == 0) painterResource(R.drawable.img_profile_women) else if (r == 1) painterResource(R.drawable.img_profile_blackmen) else painterResource(R.drawable.img_profile_whitemen),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
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

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    userModifiedName = true
                },
                label = { Text(stringResource(R.string.profile_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = isSaving,
                isError = !isNameValid && name.isNotEmpty(),
                supportingText = {
                    if (!isNameValid && name.isNotEmpty()) {
                        Text(stringResource(R.string.profile_invalid_name), color = colorScheme.error)
                    }
                },
                leadingIcon = { Icon(Icons.Default.Person, null) }
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.profile_email_label)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGoogleUser,
                readOnly = isGoogleUser && isSaving,
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
