package ecobuild_ai.app.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import ecobuild_ai.app.constant.Routes
import ecobuild_ai.app.navigation.BottomNavBar
import androidx.compose.foundation.clickable
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import ecobuild_ai.app.ui.theme.BorderLight
import java.io.File

/**
 * Modificador para criar bordas tracejadas (dashed border) em componentes Compose.
 */
fun Modifier.dashedBorder(
    color: Color,
    strokeWidth: Dp = 2.dp,
    dashWidth: Dp = 10.dp,
    gapWidth: Dp = 8.dp,
    cornerRadius: Dp = 24.dp
): Modifier = this.drawBehind {
    val stroke = Stroke(
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(
            intervals = floatArrayOf(dashWidth.toPx(), gapWidth.toPx()),
            phase = 0f
        )
    )
    drawRoundRect(
        color = color,
        style = stroke,
        cornerRadius = CornerRadius(cornerRadius.toPx())
    )
}

data class UploadItems( val selectedIcon: ImageVector, val iconName: String, val id: Int)

data class SelectedFile(
    val uri: Uri,
    val name: String,
    val sizeLabel: String
)

fun getFileInfo(context: android.content.Context, uri: Uri): SelectedFile {
    var name = "arquivo"
    var size = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
            if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
        }
    }
    val sizeMb = size / (1024f * 1024f)
    val sizeLabel = "%.1f MB".format(sizeMb)
    return SelectedFile(uri, name, sizeLabel)
}

@Composable
fun UploadScreen(navController: NavController) {
    val firebaseAuth = remember { FirebaseAuth.getInstance() }
    val currentUser = remember(firebaseAuth) { firebaseAuth.currentUser }
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    var selectedFile by remember { mutableStateOf<SelectedFile?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

// Câmera
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let { uri -> selectedFile = getFileInfo(context, uri) }
        }
    }

// Galeria (imagens)
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedFile = getFileInfo(context, it) }
    }

// PDF
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { selectedFile = getFileInfo(context, it) }
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController = navController, Routes.Upload) },
        containerColor = colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 15.dp)
        ) {
            Text(
                text = "New Project",
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Upload your construction plan",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = colorScheme.primary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Card com Borda Tracejada ──────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .dashedBorder(
                        color = colorScheme.primary,
                        strokeWidth = 2.dp,
                        dashWidth = 10.dp,
                        gapWidth = 8.dp,
                        cornerRadius = 24.dp
                    ),
                shape = RoundedCornerShape(24.dp),
                color = if (isDark) colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0xFFF2FBF5)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Architecture,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(26.dp).clip(RoundedCornerShape(18.dp))
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Arraste ou selecione uma pasta",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "PNG, JPG or PDF até 25MB",
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = if (isDark) Color.LightGray else Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(15.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val items = listOf(
                    UploadItems(Icons.Default.PhotoCamera, "Camera",1),
                    UploadItems(Icons.Default.Photo, "Gallery", 2),
                    UploadItems(Icons.Default.Description, "PDF", 3)
                )
                items.forEach { item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                when (item.id) {
                                    1 -> {
                                        val photoFile = File.createTempFile("camera_", ".jpg", context.cacheDir)
                                        val uri = FileProvider.getUriForFile(
                                            context, "${context.packageName}.fileprovider", photoFile
                                        )
                                        cameraUri = uri
                                        cameraLauncher.launch(uri)
                                    }
                                    2 -> galleryLauncher.launch("image/*")
                                    else -> pdfLauncher.launch(arrayOf("application/pdf"))
                                }
                                Toast.makeText(context, item.iconName, Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Icon(
                            imageVector = item.selectedIcon,
                            contentDescription = item.iconName,
                            tint = if (!isDark) Color.Black.copy(alpha = 0.6f) else Color.Gray.copy(0.6f ) ,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.iconName,
                            fontSize = 12.sp,
                            color = if (isDark) Color.LightGray else Color.DarkGray
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            AnimatedVisibility(visible = selectedFile != null) {
                selectedFile?.let { file ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Preview", color = colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                                IconButton(onClick = { selectedFile = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remover", tint = colorScheme.onSurfaceVariant)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            AsyncImage(
                                model = file.uri,
                                contentDescription = file.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(file.name, style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(file.sizeLabel, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = {
                    navController.navigate(Routes.Analysis) {
                        popUpTo(Routes.Upload) { inclusive = true }
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = colorScheme.primaryContainer,
                    contentColor = colorScheme.primary,
                    disabledContainerColor = colorScheme.primaryContainer,
                    disabledContentColor = colorScheme.primary
                ),
                enabled = (selectedFile != null),
                border = BorderStroke(width = 1.dp, color = BorderLight),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Analysis", color = colorScheme.primary)
            }
        }
    }
}