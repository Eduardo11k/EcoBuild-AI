package ecobuild_ai.app.screens

import android.util.Patterns
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import ecobuild_ai.app.R
import ecobuild_ai.app.constant.Routes
import ecobuild_ai.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Configure aqui diretamente o seu Web Client ID do Firebase (OAuth 2.0 Client ID)
 * Exemplo: "422382578180-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.apps.googleusercontent.com"
 * Se preenchido aqui, este valor tem prioridade sobre o strings.xml.
 */
const val GOOGLE_WEB_CLIENT_ID = "422382578180-rh8j758rkn2t4d2qt56ic8rm3l3go8tj.apps.googleusercontent.com"

/**
 * Validação de endereço de email.
 * Retorna uma mensagem de erro ou null se o email for válido.
 */
fun validarEmail(email: String): String? {
    val trimmedEmail = email.trim()
    return when {
        trimmedEmail.isEmpty() -> "O campo de email não pode estar vazio"
        !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() -> "Formato de email inválido"
        else -> null
    }
}

/**
 * Validação de palavra-passe.
 * Retorna uma mensagem de erro ou null se for válida.
 */
fun validarPassword(password: String): String? {
    return when {
        password.isEmpty() -> "O campo de palavra-passe não pode estar vazio"
        password.length < 6 -> "A palavra-passe deve ter pelo menos 6 caracteres"
        else -> null
    }
}

/**
 * Converte exceções do Firebase em mensagens legíveis e amigáveis para o utilizador.
 */
fun getFirebaseErrorMessage(exception: Throwable?): String {
    return when (exception) {
        is FirebaseAuthInvalidUserException -> "Não existe nenhuma conta associada a este email."
        is FirebaseAuthInvalidCredentialsException -> "Email ou palavra-passe incorretos."
        is FirebaseNetworkException -> "Sem ligação à internet. Verifique a sua ligação de rede."
        is FirebaseTooManyRequestsException -> "Muitas tentativas falhadas. Tente novamente mais tarde."
        is FirebaseAuthException -> {
            when (exception.errorCode) {
                "ERROR_INVALID_EMAIL" -> "O formato do email é inválido."
                "ERROR_WRONG_PASSWORD" -> "Email ou palavra-passe incorretos."
                "ERROR_USER_NOT_FOUND" -> "Não existe nenhuma conta associada a este email."
                "ERROR_USER_DISABLED" -> "Esta conta foi desativada."
                "ERROR_TOO_MANY_REQUESTS" -> "Muitas tentativas falhadas. Tente novamente mais tarde."
                "ERROR_OPERATION_NOT_ALLOWED" -> "Início de sessão por email e palavra-passe não está habilitado."
                else -> exception.localizedMessage ?: "Erro na autenticação. Tente novamente."
            }
        }
        else -> {
            val msg = exception?.localizedMessage ?: ""
            if (msg.contains("TOO_MANY_ATTEMPTS_TRY_LATER", ignoreCase = true)) {
                "Muitas tentativas falhadas. Tente novamente mais tarde."
            } else {
                exception?.localizedMessage ?: "Ocorreu um erro inesperado. Tente novamente."
            }
        }
    }
}

@Composable
fun LoginScreen(navController: NavController) {

    val auth = remember { FirebaseAuth.getInstance() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }

    // Check if user is already logged in
    LaunchedEffect(Unit) {
        if (auth.currentUser != null) {
            navController.navigate(Routes.Home) {
                popUpTo(Routes.Login) { inclusive = true }
            }
        }
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Error states for inputs
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // Forgot password state
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetEmailError by remember { mutableStateOf<String?>(null) }
    var isResetLoading by remember { mutableStateOf(false) }

    // Button press animation state
    var signInPressed by remember { mutableStateOf(false) }
    var googlePressed by remember { mutableStateOf(false) }
    val signInScale by animateFloatAsState(
        targetValue = if (signInPressed) 0.96f else 1f,
        animationSpec = tween(100),
        label = "signInScale"
    )
    val googleScale by animateFloatAsState(
        targetValue = if (googlePressed) 0.96f else 1f,
        animationSpec = tween(100),
        label = "googleScale"
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // ── Hero background image (top half) ──────────────────────────────────
        Image(
            painter = painterResource(id = R.drawable.login_img),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.52f),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay at bottom of hero
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.52f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0x55000000)),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // ── EcoBuild Logo ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                painter = painterResource(id = R.drawable.logoapp_img),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(160.dp),
                contentScale = ContentScale.Fit
            )
        }

        // ── White card scrollable container ──────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(top = 200.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(SurfaceWhite)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {

            // ── Welcome header ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Leaf emoji badge
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(EcoGreenIcon, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🌿",
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.login_welcome_title),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.login_welcome_subtitle),
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 19.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 3D building illustration
                Image(
                    painter = painterResource(id = R.drawable.counter_eco_img),
                    contentDescription = null,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(EcoGreenSurface),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Email field ───────────────────────────────────────────────────
            Text(
                text = "✉  " + stringResource(R.string.login_email_label),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (emailError != null) emailError = null
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.login_email_placeholder),
                        color = TextHint,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = emailError != null,
                supportingText = if (emailError != null) {
                    {
                        Text(
                            text = emailError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                } else null,
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EcoGreen,
                    unfocusedBorderColor = BorderLight,
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                    errorContainerColor = SurfaceWhite
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Password field ────────────────────────────────────────────────
            Text(
                text = "🔒  " + stringResource(R.string.login_password_label),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (passwordError != null) passwordError = null
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.login_password_placeholder),
                        color = TextHint,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = passwordError != null,
                supportingText = if (passwordError != null) {
                    {
                        Text(
                            text = passwordError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                } else null,
                enabled = !isLoading,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        enabled = !isLoading
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (passwordVisible)
                                    android.R.drawable.ic_menu_view
                                else
                                    android.R.drawable.ic_secure
                            ),
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EcoGreen,
                    unfocusedBorderColor = BorderLight,
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                    errorContainerColor = SurfaceWhite
                )
            )

            // ── Forgot password ───────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = stringResource(R.string.login_forgot_password),
                    color = EcoGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !isLoading
                        ) {
                            resetEmail = email.trim()
                            resetEmailError = null
                            showForgotPasswordDialog = true
                        }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Sign In button ────────────────────────────────────────────────
            Button(
                onClick = {
                    signInPressed = true

                    val emailErr = validarEmail(email)
                    val passErr = validarPassword(password)

                    emailError = emailErr
                    passwordError = passErr

                    if (emailErr != null || passErr != null) {
                        val message = emailErr ?: passErr ?: "Por favor, preencha todos os campos corretamente"
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        signInPressed = false
                        return@Button
                    }

                    isLoading = true
                    auth.signInWithEmailAndPassword(email.trim(), password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            signInPressed = false
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Sessão iniciada com sucesso!", Toast.LENGTH_SHORT).show()
                                navController.navigate(Routes.Home) {
                                    popUpTo(Routes.Login) { inclusive = true }
                                }
                            } else {
                                val error = getFirebaseErrorMessage(task.exception)
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .scale(signInScale),
                enabled = !isLoading && !isGoogleLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EcoGreen,
                    contentColor = Color.White,
                    disabledContainerColor = EcoGreen.copy(alpha = 0.6f),
                    disabledContentColor = Color.White.copy(alpha = 0.8f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.login_button),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // ── Or continue with divider ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = DividerColor,
                    thickness = 1.dp
                )
                Text(
                    text = "  ${stringResource(R.string.login_or_continue)}  ",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = DividerColor,
                    thickness = 1.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Google button ─────────────────────────────────────────────────
            OutlinedButton(
                onClick = {
                    googlePressed = true

                    val webClientId = if (GOOGLE_WEB_CLIENT_ID.isNotBlank()) {
                        GOOGLE_WEB_CLIENT_ID.trim()
                    } else {
                        try {
                            context.getString(R.string.default_web_client_id).trim()
                        } catch (e: Exception) {
                            ""
                        }
                    }

                    if (webClientId.isBlank() || webClientId.startsWith("YOUR_WEB_CLIENT_ID")) {
                        Toast.makeText(
                            context,
                            "Por favor, configure o Web Client ID em GOOGLE_WEB_CLIENT_ID ou strings.xml",
                            Toast.LENGTH_LONG
                        ).show()
                        googlePressed = false
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
                                        isGoogleLoading = false
                                        googlePressed = false
                                        if (task.isSuccessful) {
                                            Toast.makeText(context, "Sessão iniciada com o Google!", Toast.LENGTH_SHORT).show()
                                            navController.navigate(Routes.Home) {
                                                popUpTo(Routes.Login) { inclusive = true }
                                            }
                                        } else {
                                            val error = getFirebaseErrorMessage(task.exception)
                                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                        }
                                    }
                            } else {
                                isGoogleLoading = false
                                googlePressed = false
                                Toast.makeText(context, "Tipo de credencial inválido.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: GetCredentialCancellationException) {
                            // O utilizador cancelou a seleção de conta Google
                            isGoogleLoading = false
                            googlePressed = false
                        } catch (e: Exception) {
                            isGoogleLoading = false
                            googlePressed = false
                            val errorMsg = e.localizedMessage ?: "Erro ao iniciar sessão com o Google."
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .scale(googleScale),
                enabled = !isLoading && !isGoogleLoading,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, BorderLight),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SurfaceWhite,
                    contentColor = TextPrimary
                )
            ) {
                if (isGoogleLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = EcoGreen,
                        strokeWidth = 2.dp
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.login_google_img),
                        contentDescription = "Google",
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.login_google_button),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Create account ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.login_no_account) + " ",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Text(
                    text = stringResource(R.string.login_create_account),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoGreen,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !isLoading
                    ) {
                        navController.navigate(Routes.Register)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ── Forgot Password Dialog ────────────────────────────────────────────────
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isResetLoading) {
                    showForgotPasswordDialog = false
                    resetEmailError = null
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.login_forgot_password),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Introduza o seu email para receber um link de recuperação da sua palavra-passe.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = {
                            resetEmail = it
                            if (resetEmailError != null) resetEmailError = null
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.login_email_placeholder),
                                color = TextHint,
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        isError = resetEmailError != null,
                        supportingText = if (resetEmailError != null) {
                            {
                                Text(
                                    text = resetEmailError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                            }
                        } else null,
                        enabled = !isResetLoading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EcoGreen,
                            unfocusedBorderColor = BorderLight,
                            focusedContainerColor = SurfaceWhite,
                            unfocusedContainerColor = SurfaceWhite,
                            errorBorderColor = MaterialTheme.colorScheme.error,
                            errorContainerColor = SurfaceWhite
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val validationError = validarEmail(resetEmail)
                        if (validationError != null) {
                            resetEmailError = validationError
                            return@Button
                        }
                        isResetLoading = true
                        auth.sendPasswordResetEmail(resetEmail.trim())
                            .addOnCompleteListener { task ->
                                isResetLoading = false
                                if (task.isSuccessful) {
                                    Toast.makeText(
                                        context,
                                        "Email de recuperação enviado com sucesso!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    showForgotPasswordDialog = false
                                    resetEmailError = null
                                } else {
                                    val errorMsg = getFirebaseErrorMessage(task.exception)
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            }
                    },
                    enabled = !isResetLoading,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EcoGreen,
                        contentColor = Color.White
                    )
                ) {
                    if (isResetLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Enviar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showForgotPasswordDialog = false
                        resetEmailError = null
                    },
                    enabled = !isResetLoading
                ) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

