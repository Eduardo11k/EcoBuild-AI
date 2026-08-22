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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import ecobuild_ai.app.R
import ecobuild_ai.app.constant.Routes
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun validarSenha(senha: String): String? {
    return when {
        senha.length < 7 -> "Mínimo de 7 caracteres"
        senha.length > 10 -> "Máximo de 10 caracteres"
        !senha.any { it.isDigit() } -> "Deve conter pelo menos 1 número"
        !senha.any { !it.isLetterOrDigit() } -> "Deve conter 1 caractere especial"
        else -> null
    }
}

@Composable
fun RegisterScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val colorScheme = MaterialTheme.colorScheme

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            painter = painterResource(R.drawable.ic_resgister_image),
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
                contentDescription = "Logo Kudia",
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Inside
            )
            Text(
                text = "KUDIA",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sabores de Angola 🇦🇴",
                color = Color.LightGray,
                fontSize = 16.sp
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
                            append("Criar Conta\n")
                        }
                        withStyle(style = SpanStyle(color = colorScheme.onSurfaceVariant, fontWeight = FontWeight.Normal)) {
                            append("Junte-se à comunidade Kudia")
                        }
                    },
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                var fullName by remember { mutableStateOf("") }
                var email by remember { mutableStateOf("") }
                var emailError by remember { mutableStateOf<String?>(null) }
                var password by remember { mutableStateOf("") }
                var confirmPassword by remember { mutableStateOf("") }

                var passwordError by remember { mutableStateOf<String?>(null) }
                var confirmPasswordError by remember { mutableStateOf<String?>(null) }

                var passwordVisible by remember { mutableStateOf(false) }
                var confirmPasswordVisible by remember { mutableStateOf(false) }

                val context = LocalContext.current

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nome completo") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
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
                        emailError = validarEmail(it)
                    },
                    label = { Text("Email") },
                    isError = emailError != null,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null)
                    },
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
                    Text(text = emailError!!, color = colorScheme.error, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = validarSenha(it)
                    },
                    label = { Text("Palavra-passe") },
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
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                        confirmPasswordError = if (it != password) "As senhas não coincidem" else null
                    },
                    label = { Text("Confirmar Palavra-passe") },
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
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                        if (email.isBlank() || password.isBlank() || fullName.isBlank()) {
                            Toast.makeText(context, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (password != confirmPassword) {
                            Toast.makeText(context, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener { result ->
                                val uid = result.user?.uid ?: return@addOnSuccessListener
                                val newUser = User(
                                    uid = uid,
                                    fullName = fullName,
                                    email = email,
                                    username = email.substringBefore("@")
                                )

                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        UserRepository().saveUser(newUser)
                                        launch(Dispatchers.Main) {
                                            Toast.makeText(context, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()
                                            navController.navigate(Routes.Login) {
                                                popUpTo(Routes.Register) { inclusive = true }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        launch(Dispatchers.Main) {
                                            Toast.makeText(context, "Erro ao salvar dados: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Erro: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                ) {
                    Text(text = "Criar conta", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = colorScheme.outlineVariant)
                    Text(
                        text = " Ou ",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = colorScheme.outlineVariant)
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = { /* Google */ },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colorScheme.outline)
                ) {
                    Image(painter = painterResource(R.drawable.ic_google_image), contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Google", color = colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row {
                    Text(text = "Já possui conta? ", color = colorScheme.onSurfaceVariant)
                    Text(
                        text = "Entrar",
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { navController.navigate(Routes.Login) }
                    )
                }
            }
        }
    }
}

