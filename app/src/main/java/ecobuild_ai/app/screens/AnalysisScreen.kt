package ecobuild_ai.app.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import ecobuild_ai.app.R
import ecobuild_ai.app.constant.Routes
import ecobuild_ai.app.data.remote.AnalysisResponse
import ecobuild_ai.app.triggerNotificationUpload
import ecobuild_ai.app.viewmodel.AnalysisUiState
import ecobuild_ai.app.viewmodel.AnalysisViewModel
import ecobuild_ai.app.viewmodel.UploadViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Colour helpers
// ─────────────────────────────────────────────────────────────────────────────
private val GreenPrimary = Color(0xFF34C759)
private val GreenLight   = Color(0xFF4CD964)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    navController: NavController,
    viewModel: AnalysisViewModel,
    uploadViewModel: UploadViewModel
) {
    val planId    = uploadViewModel.lastUploadedPlanId
    val isChecked by viewModel.isCheckedNotify.collectAsStateWithLifecycle()
    val uiState   = viewModel.uiState
    val context   = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setNotifyEnabled(true)
            triggerNotificationUpload(context)
        } else {
            viewModel.setNotifyEnabled(false)
            Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(planId) {
        if (planId != null) viewModel.pollAnalysisStatus(planId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analysis Result", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Analysis) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Share — visible but no action yet (placeholder for future ShareCompat)
                    IconButton(onClick = { /* TODO: implement share */ }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopStart
        ) {
            // ── Smooth animated transition between states ──────────────────
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 4 })
                        .togetherWith(fadeOut() + slideOutVertically { -it / 4 })
                },
                label = "AnalysisStateTransition"
            ) { state ->
                when (state) {

                    // ── Loading / Idle ─────────────────────────────────────
                    is AnalysisUiState.Idle, is AnalysisUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color       = GreenPrimary,
                                    modifier    = Modifier.size(56.dp),
                                    strokeWidth = 4.dp
                                )
                                Spacer(Modifier.height(24.dp))
                                Text(
                                    "Analyzing your plan...",
                                    style      = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = GreenPrimary
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "This may take a few seconds",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // ── Ready ──────────────────────────────────────────────
                    is AnalysisUiState.Ready -> {
                        AnalysisResultContent(
                            analysis       = state.analysis,
                            isChecked      = isChecked,
                            onToggleNotify = { checked ->
                                viewModel.setNotifyEnabled(checked)
                                if (checked) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val hasPerm = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED

                                        if (hasPerm) triggerNotificationUpload(context)
                                        else permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        triggerNotificationUpload(context)
                                    }
                                }
                            }
                        )
                    }

                    // ── Error ──────────────────────────────────────────────
                    is AnalysisUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint     = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Error: ${state.message}",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(20.dp))
                                Button(onClick = {
                                    if (planId != null) viewModel.pollAnalysisStatus(planId)
                                }) { Text("Retry") }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Result Content — tabbed layout
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalysisResultContent(
    analysis: AnalysisResponse,
    isChecked: Boolean,
    onToggleNotify: (Boolean) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Materials", "Cost", "Sustain")
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ProjectHeaderCard(analysis)

        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor   = colorScheme.surface,
            contentColor     = GreenPrimary,
            edgePadding      = 16.dp
        ) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick  = { selectedTab = index },
                    text = {
                        Text(
                            label,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize   = 14.sp
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            when (selectedTab) {
                0 -> OverviewTab(analysis, isChecked, onToggleNotify)
                1 -> MaterialsTab(analysis)
                2 -> CostTab(analysis)
                3 -> SustainTab(analysis)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Project Header Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ProjectHeaderCard(analysis: AnalysisResponse) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GreenPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_logo_ecobuild_ai),
                    contentDescription = null,
                    tint     = GreenPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = analysis.project_name ?: "Project Plan #${analysis.plan_id}",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                if (analysis.plan_type != null) {
                    Text(
                        text  = analysis.plan_type,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint     = GreenPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Analysis complete",
                        fontSize   = 12.sp,
                        color      = GreenPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Overview Tab
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OverviewTab(
    analysis: AnalysisResponse,
    isChecked: Boolean,
    onToggleNotify: (Boolean) -> Unit
) {
    val score = analysis.sustainability_score
    if (score != null) {
        SustainabilityScoreCard(score)
        Spacer(Modifier.height(16.dp))
    }

    val matCount = analysis.material_list?.materials?.size

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCard(
            modifier = Modifier.weight(1f),
            value    = if (analysis.total_area != null) "${analysis.total_area.toInt()} m²" else "—",
            label    = "Total Area"
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            value    = if (analysis.rooms != null) "${analysis.rooms} Rooms" else "—",
            label    = "Divisions"
        )
    }
    Spacer(Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCard(
            modifier = Modifier.weight(1f),
            value    = if (matCount != null) "$matCount Types" else "—",
            label    = "Materials"
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            value    = if (analysis.estimated_cost != null) "€ ${"%,.0f".format(analysis.estimated_cost)}" else "—",
            label    = "Est. Cost"
        )
    }

    val co2 = analysis.co2_estimated
    if (co2 != null) {
        Spacer(Modifier.height(12.dp))
        Co2Card(co2 = co2, rating = analysis.co2_rating)
    }

    if (analysis.material_list?.waste_percentage != null) {
        Spacer(Modifier.height(12.dp))
        MetricCard(
            modifier = Modifier.fillMaxWidth(),
            value = "${analysis.material_list.waste_percentage}%",
            label = "Estimated Waste Percentage"
        )
    }

    Spacer(Modifier.height(20.dp))
    NotifyCard(isChecked = isChecked, onToggle = onToggleNotify)
}

// ─────────────────────────────────────────────────────────────────────────────
// Materials Tab
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MaterialsTab(analysis: AnalysisResponse) {
    val materials = analysis.material_list?.materials

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Materials Report", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "${materials?.size ?: 0} items identified",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))

            if (materials.isNullOrEmpty()) {
                Text("No specific materials listed.", color = Color.Gray)
            } else {
                materials.forEachIndexed { index, mat ->
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mat.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            if (mat.eco_rating != null) {
                                Text("Eco: ${mat.eco_rating}", style = MaterialTheme.typography.labelSmall, color = GreenPrimary)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${mat.quantity} ${mat.unit}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (mat.cost_per_unit != null) {
                                Text(
                                    "€${mat.cost_per_unit}/unit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (index < materials.lastIndex) {
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }

            analysis.material_list?.notes?.let { notes ->
                Spacer(Modifier.height(12.dp))
                Surface(
                    color    = GreenPrimary.copy(alpha = 0.08f),
                    shape    = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        notes,
                        modifier = Modifier.padding(10.dp),
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cost Tab
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CostTab(analysis: AnalysisResponse) {
    val cost      = analysis.estimated_cost
    val materials = analysis.material_list?.materials

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Cost Report", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(16.dp))

            if (cost != null) {
                Surface(
                    color    = GreenPrimary.copy(alpha = 0.1f),
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier            = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Estimated Total Cost", style = MaterialTheme.typography.labelMedium, color = GreenPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("€ ${"%,.2f".format(cost)}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    }
                }
                Spacer(Modifier.height(20.dp))
            } else {
                Text("Cost data not yet available.", color = Color.Gray)
                Spacer(Modifier.height(16.dp))
            }

            val breakdownMats = materials?.filter { it.cost_per_unit != null }
            if (!breakdownMats.isNullOrEmpty()) {
                Text("Breakdown by Material", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                breakdownMats.forEach { mat ->
                    val line = mat.cost_per_unit!! * mat.quantity
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(mat.name, style = MaterialTheme.typography.bodySmall)
                        Text("€ ${"%,.2f".format(line)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sustain Tab
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SustainTab(analysis: AnalysisResponse) {
    val ecoMats = analysis.material_list?.materials?.filter { it.eco_rating != null }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Sustainability Report", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(16.dp))

            if (analysis.sustainability_score != null) {
                SustainabilityScoreCard(analysis.sustainability_score)
                Spacer(Modifier.height(16.dp))
            }

            val co2 = analysis.co2_estimated
            if (co2 != null) {
                Co2Card(co2 = co2, rating = analysis.co2_rating)
                Spacer(Modifier.height(16.dp))
            }

            if (!ecoMats.isNullOrEmpty()) {
                Text("Materials Eco Ratings", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                ecoMats.forEach { mat ->
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(mat.name, style = MaterialTheme.typography.bodySmall)
                        val badgeColor = when (mat.eco_rating) {
                            "A"  -> GreenPrimary
                            "B"  -> Color(0xFFB8860B)
                            else -> Color(0xFFCC0000)
                        }
                        Surface(
                            color = badgeColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                mat.eco_rating ?: "",
                                modifier   = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize   = 12.sp,
                                color      = badgeColor
                            )
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
                Spacer(Modifier.height(12.dp))
            }

            analysis.sustain_notes?.let { notes ->
                Surface(
                    color    = GreenPrimary.copy(alpha = 0.08f),
                    shape    = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(notes, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (analysis.sustainability_score == null && analysis.co2_estimated == null && analysis.sustain_notes == null && ecoMats.isNullOrEmpty()) {
                Text("Sustainability data not yet available.", color = Color.Gray)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable Composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SustainabilityScoreCard(score: Int) {
    val rating = when {
        score >= 90 -> "Outstanding"
        score >= 75 -> "Excellent rating"
        score >= 60 -> "Good rating"
        score >= 45 -> "Fair rating"
        else        -> "Needs improvement"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(GreenPrimary, GreenLight)),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("Sustainability Score", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                    Spacer(Modifier.height(4.dp))
                    Text("$score%", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(rating, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                }
                Box(
                    modifier         = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(modifier: Modifier = Modifier, value: String, label: String) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Co2Card(co2: Double, rating: String?) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("$co2 ton", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("CO₂ Estimated", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (rating != null) {
                val badgeColor = when (rating.lowercase()) {
                    "low"    -> GreenPrimary
                    "medium" -> Color(0xFFFF9500)
                    else     -> Color(0xFFFF3B30)
                }
                Surface(color = badgeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        rating,
                        modifier   = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color      = badgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun NotifyCard(isChecked: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Notify me when ready", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Get a push notification when analysis completes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
            Spacer(Modifier.width(16.dp))
            Switch(checked = isChecked, onCheckedChange = onToggle)
        }
    }
}
