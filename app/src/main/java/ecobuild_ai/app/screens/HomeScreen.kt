package ecobuild_ai.app.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import ecobuild_ai.app.R
import ecobuild_ai.app.constant.Routes
import ecobuild_ai.app.model.AnalysisItem
import ecobuild_ai.app.model.sampleRecentAnalyses
import ecobuild_ai.app.navigation.BottomNavBar
import ecobuild_ai.app.ui.theme.*

@Composable
fun HomeScreen(
    navController: NavController,
    analysesList: List<AnalysisItem> = sampleRecentAnalyses
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = remember(auth) { auth.currentUser }
    val context = LocalContext.current

    // If user is not logged in, redirect to login
    if (currentUser == null) {
        LaunchedEffect(Unit) {
            navController.navigate(Routes.Login) {
                popUpTo(Routes.Home) { inclusive = true }
            }
        }
        return
    }

    // Extract first name for personalized greeting
    val rawName = currentUser.displayName?.trim().orEmpty()
    val firstName = remember(rawName) {
        if (rawName.isNotBlank()) {
            rawName.split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "User"
        } else {
            "User"
        }
    }

    var showAllAnalyses by remember { mutableStateOf(false) }

    // Display maximum of 3 items on Home tab unless "See all" is clicked
    val displayedAnalyses = remember(analysesList, showAllAnalyses) {
        if (showAllAnalyses) analysesList else analysesList.take(3)
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(navController = navController, rotaActual = Routes.Home)
        },
        containerColor = SurfaceBgLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // ── 1. Top Header: Logo + Notification Bell ───────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // EcoBuild-AI Logo badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(EcoGreenIconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = EcoGreenPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = EcoGreenPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp
                                )
                            ) {
                                append(stringResource(R.string.app_name_prefix))
                            }
                            withStyle(
                                SpanStyle(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 20.sp
                                )
                            ) {
                                append(stringResource(R.string.app_name_suffix))
                            }
                        }
                    )
                }

                // Notification Bell icon button with circular border
                Surface(
                    shape = CircleShape,
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(
                        onClick = {
                            Toast.makeText(context, context.getString(R.string.home_notifications), Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = stringResource(R.string.home_notifications),
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 2. Greeting Section ───────────────────────────────────────────
            Text(
                text = stringResource(R.string.home_greeting_format, firstName),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.home_subtitle),
                fontSize = 13.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── 3. Featured Card: "Analyse a new plan" ─────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = EcoGreenCardBg,
                border = BorderStroke(1.dp, Color(0xFFD8F3E2))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Top row: Drafting Icon + AI badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Architecture / drafting compass icon badge
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(EcoGreenIconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Architecture,
                                contentDescription = null,
                                tint = EcoGreenPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // AI Chip
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(EcoGreenBadgeBg)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = EcoGreenBadgeText,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = stringResource(R.string.home_cta_ai_badge),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoGreenBadgeText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.home_cta_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.home_cta_description),
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Start Analysis Action Button
                    Button(
                        onClick = {
                            navController.navigate(Routes.Upload)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EcoGreenPrimary,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.home_cta_button),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── 4. Recent Analyses Section ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_recent_analyses_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = if (showAllAnalyses) {
                        stringResource(R.string.home_see_less)
                    } else {
                        stringResource(R.string.home_see_all)
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoGreenPrimary,
                    modifier = Modifier.clickable {
                        showAllAnalyses = !showAllAnalyses
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (displayedAnalyses.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.home_no_analyses),
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    displayedAnalyses.forEach { item ->
                        RecentAnalysisCard(
                            item = item,
                            onClick = {
                                Toast.makeText(context, item.title, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun RecentAnalysisCard(
    item: AnalysisItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, CardBorderColor),
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail Image Container
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(EcoGreenSurface),
                contentAlignment = Alignment.Center
            ) {
                if (item.imageUrl != null) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (item.imageRes != null) {
                    Image(
                        painter = painterResource(id = item.imageRes),
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = null,
                        tint = EcoGreenPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Information Column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Date row with calendar icon
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.date,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Score pill badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(EcoGreenBadgeBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = null,
                        tint = EcoGreenBadgeText,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = stringResource(R.string.home_score_format, item.score),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoGreenBadgeText
                    )
                }
            }

            // Trailing Chevron arrow
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextHint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
