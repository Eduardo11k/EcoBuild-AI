package ecobuild_ai.app.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.navigation.NavController
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import ecobuild_ai.app.R
import ecobuild_ai.app.constant.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import ecobuild_ai.app.model.User
import ecobuild_ai.app.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun validarPassword(senha: String): String? {
    return when {
        senha.length < 7 -> "Minimum 7 characters"
        !senha.any { it.isDigit() } -> "Must contain at least 1 number"
        !senha.any { !it.isLetterOrDigit() } -> "Must contain 1 special character"
        else -> null
    }
}

@Composable
fun RegisterScreen(navController: NavController) {
    val auth = remember { FirebaseAuth.getInstance() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val colorScheme = MaterialTheme.colorScheme

    val defaultWebClientId = stringResource(R.string.default_web_client_id).trim()
    val webClientId = if (GOOGLE_WEB_CLIENT_ID.isNotBlank()) {
        GOOGLE_WEB_CLIENT_ID.trim()
    } else {
        defaultWebClientId
    }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            painter = painterResource(R.drawable.register_img),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopStart
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.logoapp_img),
                contentDescription = "Logo EcoBuild-AI",
                modifier = Modifier.size(200.dp)
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            shape = RoundedCornerShape(32.dp),
            color = colorScheme.surface.copy(alpha = 0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = colorScheme.onSurface, fontWeight = FontWeight.Bold)) {
                            append("Create Account\n")
                        }
                        withStyle(style = SpanStyle(color = colorScheme.onSurfaceVariant, fontWeight = FontWeight.Normal)) {
                            append("Together for sustainable projects")
                        }
                    },
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full name") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null)
                    },
                    supportingText = {
                        if (fullName.length < 3 && fullName.isNotEmpty()) {
                            Text(stringResource(R.string.profile_invalid_name), color = colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading && !isGoogleLoading,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (emailError != null) emailError = null
                    },
                    label = { Text("Email") },
                    isError = emailError != null,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null)
                    },
                    enabled = !isLoading && !isGoogleLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline,
                        errorBorderColor = colorScheme.error
                    ),
                    singleLine = true
                )
                if (emailError != null) {
                    Text(
                        text = emailError!!,
                        color = colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (passwordError != null) passwordError = null
                    },
                    label = { Text("Password") },
                    isError = passwordError != null,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null)
                        }
                    },
                    enabled = !isLoading && !isGoogleLoading,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline,
                        errorBorderColor = colorScheme.error
                    ),
                    singleLine = true
                )
                passwordError?.let {
                    Text(text = it, color = colorScheme.error, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        if (confirmPasswordError != null) confirmPasswordError = null
                    },
                    label = { Text("Confirm password") },
                    isError = confirmPasswordError != null,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        val image = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(imageVector = image, contentDescription = null)
                        }
                    },
                    enabled = !isLoading && !isGoogleLoading,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline,
                        errorBorderColor = colorScheme.error
                    ),
                    singleLine = true
                )
                confirmPasswordError?.let {
                    Text(text = it, color = colorScheme.error, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (fullName.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val emailErr = validarEmail(email)
                        val passErr = validarPassword(password)

                        emailError = emailErr
                        passwordError = passErr

                        if (emailErr != null || passErr != null) {
                            val msg = emailErr ?: passErr ?: "Invalid data"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (password != confirmPassword) {
                            confirmPasswordError = "Passwords do not match"
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isLoading = true
                        auth.createUserWithEmailAndPassword(email.trim(), password)
                            .addOnSuccessListener { result ->
                                val firebaseUser = result.user ?: run {
                                    isLoading = false
                                    return@addOnSuccessListener
                                }
                                val cleanName = fullName.trim()
                                val cleanEmail = email.trim()

                                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                                    displayName = cleanName
                                }

                                firebaseUser.updateProfile(profileUpdates).addOnCompleteListener {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val newUser = User(
                                            uid = firebaseUser.uid,
                                            fullName = cleanName,
                                            email = cleanEmail
                                        )
                                        try {
                                            UserRepository().saveUser(newUser)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        launch(Dispatchers.Main) {
                                            isLoading = false
                                            Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                            navController.navigate(Routes.Home) {
                                                popUpTo(Routes.Register) { inclusive = true }
                                            }
                                        }
                                    }
                                }
                            }
                            .addOnFailureListener { error ->
                                isLoading = false
                                val errorMsg = getFirebaseErrorMessage(error)
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isLoading && !isGoogleLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = "Create account", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = colorScheme.outlineVariant)
                    Text(
                        text = " Or ",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = colorScheme.outlineVariant)
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = {
                        if (webClientId.isBlank() || webClientId.startsWith("YOUR_WEB_CLIENT_ID")) {
                            Toast.makeText(
                                context,
                                "Please configure Web Client ID in GOOGLE_WEB_CLIENT_ID or strings.xml",
                                Toast.LENGTH_LONG
                            ).show()
                            return@OutlinedButton
                        }

                        isGoogleLoading = true

                        coroutineScope.launch {
                            try {
                                val googleIdOption = GetGoogleIdOption.Builder()
                                    .setFilterByAuthorizedAccounts(false)
                                    .setServerClientId(webClientId)
                                    .setAutoSelectEnabled(false)
                                    .build()

                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()

                                val result = credentialManager.getCredential(
                                    request = request,
                                    context = context
                                )

                                val credential = result.credential
                                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                    val idToken = googleIdTokenCredential.idToken
                                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

                                    auth.signInWithCredential(firebaseCredential)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                val firebaseUser = auth.currentUser
                                                if (firebaseUser != null) {
                                                    val u = User(
                                                        uid = firebaseUser.uid,
                                                        fullName = firebaseUser.displayName ?: "",
                                                        email = firebaseUser.email ?: "",
                                                        profileImageUrl = firebaseUser.photoUrl?.toString() ?: ""
                                                    )
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        try {
                                                            UserRepository().saveUserIfNotExists(u)
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                        launch(Dispatchers.Main) {
                                                            isGoogleLoading = false
                                                            Toast.makeText(context, "Signed in with Google!", Toast.LENGTH_SHORT).show()
                                                            navController.navigate(Routes.Home) {
                                                                popUpTo(Routes.Register) { inclusive = true }
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    isGoogleLoading = false
                                                    navController.navigate(Routes.Home) {
                                                        popUpTo(Routes.Register) { inclusive = true }
                                                    }
                                                }
                                            } else {
                                                isGoogleLoading = false
                                                val error = getFirebaseErrorMessage(task.exception)
                                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                } else {
                                    isGoogleLoading = false
                                    Toast.makeText(context, "Invalid credential type.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: GetCredentialCancellationException) {
                                isGoogleLoading = false
                            } catch (e: NoCredentialException) {
                                isGoogleLoading = false
                                Toast.makeText(
                                    context,
                                    "No Google accounts available on device or SHA-1 not registered in Firebase Console.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } catch (e: Exception) {
                                isGoogleLoading = false
                                val errorMsg = when {
                                    e.message?.contains("No credentials available", ignoreCase = true) == true ->
                                        "No Google accounts available"
                                    else -> e.localizedMessage ?: "Error signing in with Google."
                                }
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isLoading && !isGoogleLoading,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colorScheme.outline)
                ) {
                    if (isGoogleLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Image(painter = painterResource(R.drawable.login_google_img), modifier = Modifier.size(36.dp), contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Google", color = colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row {
                    Text(text = "Already a user? ", color = colorScheme.onSurfaceVariant)
                    Text(
                        text = "Sign in",
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            navController.navigate(Routes.Login) {
                                popUpTo(Routes.Register) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
