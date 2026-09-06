package ecobuild_ai.app.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import ecobuild_ai.app.constant.Routes
import ecobuild_ai.app.navigation.BottomNavBar
import ecobuild_ai.app.ui.theme.BorderLight
import ecobuild_ai.app.viewmodel.UploadViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

// Render First PDF page
fun getPdfFirstPageThumbnail(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        val pdfRenderer = PdfRenderer(fileDescriptor)
        if (pdfRenderer.pageCount == 0) {
            pdfRenderer.close()
            fileDescriptor.close()
            return null
        }
        val page = pdfRenderer.openPage(0)

        // Mantém a proporção da página, com largura alvo fixa
        val targetWidth = 600
        val scale = targetWidth.toFloat() / page.width
        val targetHeight = (page.height * scale).toInt()

        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        // Fundo branco (PDFs têm fundo transparente por padrão)
        bitmap.eraseColor(android.graphics.Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

        page.close()
        pdfRenderer.close()
        fileDescriptor.close()
        bitmap
    } catch (e: Exception) {
        Log.e("KUDIA_DEBUG", "getPdfFirstPageThumbnail: erro ao renderizar PDF: ${e.message}", e)
        null
    }
}

data class UploadItems(val selectedIcon: ImageVector, val iconName: String, val id: Int)

data class SelectedFile(
    val uri: Uri,
    val name: String,
    val sizeLabel: String,
    val isPDF: Boolean = false
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
    val isPDF = context.contentResolver.getType(uri) == "application/pdf" || name.lowercase().endsWith(".pdf")
    return SelectedFile(uri, name, sizeLabel, isPDF)
}

@Composable
fun UploadScreen(navController: NavController, viewModel: UploadViewModel) {
    val firebaseAuth = FirebaseAuth.getInstance()
    val currentUser = firebaseAuth.currentUser
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val coroutine = rememberCoroutineScope()

    var selectedFile by remember { mutableStateOf<SelectedFile?>(viewModel.selectedFile) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var pdfThumbnails by remember { mutableStateOf<Bitmap?>(viewModel.pdfThumbnail) }

    // Sync back to ViewModel when state changes
    LaunchedEffect(selectedFile) {
        viewModel.selectedFile = selectedFile
    }
    LaunchedEffect(pdfThumbnails) {
        viewModel.pdfThumbnail = pdfThumbnails
    }

    // Launcher para capturar foto
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let { uri -> selectedFile = getFileInfo(context, uri) }
        }
    }

    // Launcher para solicitar permissão de câmera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val photoFile = File.createTempFile("camera_", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", photoFile
            )
            cameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Permissão de câmera negada", Toast.LENGTH_SHORT).show()
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
        uri?.let {
            selectedFile = getFileInfo(context, it)
            coroutine.launch(Dispatchers.IO) {
                pdfThumbnails = getPdfFirstPageThumbnail(context, it)
            }
        }
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

            // ── Card com Borda Tracejada
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

            Spacer(modifier = Modifier.height(22.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val items = listOf(
                    UploadItems(Icons.Default.PhotoCamera, "Camera", 1),
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
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                            val photoFile = File.createTempFile("camera_", ".jpg", context.cacheDir)
                                            val uri = FileProvider.getUriForFile(
                                                context, "${context.packageName}.fileprovider", photoFile
                                            )
                                            cameraUri = uri
                                            cameraLauncher.launch(uri)
                                        } else {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }
                                    2 -> galleryLauncher.launch("image/*")
                                    else -> pdfLauncher.launch(arrayOf("application/pdf"))
                                }
                            }
                    ) {
                        Icon(
                            imageVector = item.selectedIcon,
                            contentDescription = item.iconName,
                            tint = if (!isDark) Color.Black.copy(alpha = 0.6f) else Color.Gray.copy(0.6f),
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
                                IconButton(onClick = { 
                                    selectedFile = null
                                    pdfThumbnails = null
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remover", tint = colorScheme.onSurfaceVariant)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (file.isPDF) {
                                if (pdfThumbnails != null) {
                                    Image(
                                        bitmap = pdfThumbnails!!.asImageBitmap(),
                                        contentDescription = file.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colorScheme.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                            } else {
                                AsyncImage(
                                    model = file.uri,
                                    contentDescription = file.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }

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

            Spacer(modifier = Modifier.height(20.dp))
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
