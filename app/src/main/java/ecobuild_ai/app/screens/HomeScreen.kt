package ecobuild_ai.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import ecobuild_ai.app.R
import ecobuild_ai.app.constant.Routes
import ecobuild_ai.app.model.AnalysisItem
import ecobuild_ai.app.model.sampleRecentAnalyses
import ecobuild_ai.app.navigation.BottomNavBar
import ecobuild_ai.app.ui.theme.EcoGreen
import ecobuild_ai.app.ui.theme.EcoGreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    // Unified user data via ProfileViewModel
    val profileViewModel: ecobuild_ai.app.viewmodel.ProfileViewModel = viewModel()
    val userData by profileViewModel.userData.collectAsStateWithLifecycle()

    // Fallback auth instance for navigation guard
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    // Redirect to login if no user
    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            navController.navigate(Routes.Login) {
                popUpTo(Routes.Home) { inclusive = true }
            }
        }
    }

    // Garante que os dados do perfil estejam sempre atualizados ao entrar na Home
    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            profileViewModel.refreshUserData()
        }
    }

    if (currentUser == null) return

    // Display name: Firestore > Auth displayName > email prefix > "User"
    val firstName = (userData?.fullName?.ifEmpty { null }
        ?: currentUser.displayName?.ifEmpty { null }
        ?: currentUser.email?.substringBefore("@"))
        ?.split(" ")?.firstOrNull()
        ?: "User"

    // Dynamic colour scheme — adapts automatically to light / dark
    val cs = MaterialTheme.colorScheme

    Scaffold(
        containerColor = cs.background,
        bottomBar = { BottomNavBar(navController, Routes.Home) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // ── HEADER ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // App logo badge
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(cs.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = cs.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = EcoGreen, fontWeight = FontWeight.Bold)) {
                                append("EcoBuild")
                            }
                            withStyle(SpanStyle(color = cs.onSurfaceVariant, fontWeight = FontWeight.Bold)) {
                                append("-AI")
                            }
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                // Notification button
                Surface(
                    shape = CircleShape,
                    color = cs.surfaceVariant,
                    border = BorderStroke(1.dp, cs.outline),
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(onClick = { /* Notifications */ }) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = cs.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── GREETING ─────────────────────────────────────────────────────
            Text(
                text = "Hello, $firstName 👋",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = cs.onBackground
            )
            Text(
                text = "Ready to build sustainably today?",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── FEATURED CARD ─────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = cs.surfaceVariant,
                border = BorderStroke(1.dp, cs.outline)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Icon badge
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(cs.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Architecture,
                                null,
                                tint = cs.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // AI Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(cs.secondaryContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                null,
                                tint = cs.onSecondaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Analyse a new plan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface
                    )
                    Text(
                        text = "Upload your plan and get a sustainability score in seconds.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { navController.navigate(Routes.Upload) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EcoGreenPrimary)
                    ) {
                        Icon(Icons.Default.FlashOn, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Analysis", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── RECENT ANALYSES ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Analyses",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = cs.onBackground
                )
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.bodySmall,
                    color = EcoGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { /* See All */ }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            sampleRecentAnalyses.take(3).forEach { item ->
                RecentAnalysisCard(item)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun RecentAnalysisCard(item: AnalysisItem) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, cs.outlineVariant),
        color = cs.surface                       // ← adapta-se ao tema automaticamente
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cs.surfaceVariant),  // ← era SurfaceBgLight (sempre claro)
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Image,
                    null,
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,           // ← era TextPrimary (fixo escuro)
                    fontSize = 14.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        null,
                        tint = cs.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        item.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant  // ← era TextSecondary (fixo cinza)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Eco score badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(cs.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Default.Eco,
                        null,
                        tint = cs.onPrimaryContainer,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Score: ${item.score}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
