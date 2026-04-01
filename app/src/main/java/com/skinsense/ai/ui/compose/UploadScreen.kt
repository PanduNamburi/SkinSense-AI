package com.skinsense.ai.ui.compose

import android.content.ContentValues
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.graphics.ImageDecoder
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import com.skinsense.ai.R
import com.skinsense.ai.ui.theme.*
import com.skinsense.ai.utils.SkinImageValidator
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UploadScreen(
    onImageSelected: (Uri) -> Unit,
    onAnalyze: (Uri) -> Unit,
    onBack: () -> Unit
) {
    // State to hold multiple images (UI supports 3, logic consumes first for now)
    val selectedImages = remember { mutableStateListOf<Uri>() }
    val maxImages = 3

    // Validation UI States
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<String?>(null) }
    var isSkinValid by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val validator = remember { SkinImageValidator(context) }
    
    DisposableEffect(Unit) {
        onDispose {
            validator.close()
        }
    }

    // ── Camera setup with FileProvider (crash-safe, recommended approach) ───
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    /** Creates a temp file URI via FileProvider — works on all Android versions */
    fun createCameraUri(): Uri? = try {
        val cacheDir = java.io.File(context.cacheDir, "camera").also { it.mkdirs() }
        val file = java.io.File(cacheDir, "skin_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    } catch (e: Exception) {
        null
    }

    /** Shared validation logic for both camera and gallery images */
    fun validateAndAddUri(uri: Uri) {
        if (selectedImages.size >= maxImages) return
        isAnalyzing = true
        analysisResult = null
        isSkinValid = false
        coroutineScope.launch {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true) ?: bitmap
                delay(3000)
                validator.validate(softwareBitmap) { isValid, reason ->
                    isAnalyzing = false
                    isSkinValid = isValid
                    if (isValid) {
                        analysisResult = context.getString(R.string.upload_success)
                        selectedImages.add(uri)
                        onImageSelected(uri)
                    } else {
                        analysisResult = reason
                    }
                }
            } catch (e: Exception) {
                isAnalyzing = false
                isSkinValid = false
                analysisResult = context.getString(R.string.upload_failed)
            }
        }
    }

    // ── Camera launcher ───────────────────────────────────────────────────────
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            cameraImageUri?.let { validateAndAddUri(it) }
        }
    }

    /** Build and fire the camera intent once permissions are confirmed */
    fun openCameraIntent() {
        val uri = createCameraUri()
        if (uri == null) {
            Toast.makeText(context, "Cannot create image file.", Toast.LENGTH_SHORT).show()
            return
        }
        cameraImageUri = uri
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (intent.resolveActivity(context.packageManager) == null) {
            Toast.makeText(context, "No camera app found on this device.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            cameraLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /** Permission launcher — requests CAMERA permission then opens camera */
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCameraIntent()
        else Toast.makeText(context, "Camera permission required to take photos.", Toast.LENGTH_LONG).show()
    }

    /** Entry point: check permission then open camera */
    fun launchCamera() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) openCameraIntent()
        else permissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    // ── Gallery launcher ─────────────────────────────────────────────────────
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { validateAndAddUri(it) }
    }



    Scaffold(
        containerColor = BackgroundMain,
        topBar = {
            UploadTopBar(onBack = onBack)
        },
        bottomBar = {
             Button(
                onClick = {
                    if (selectedImages.isNotEmpty()) {
                        onAnalyze(selectedImages.first())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(56.dp),
                enabled = selectedImages.isNotEmpty(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue,
                    disabledContainerColor = AccentBlue.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = stringResource(R.string.btn_analyze),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            
            Text(
                text = stringResource(R.string.upload_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Instruction Card
            InstructionCard()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Upload Zone
            UploadZone(
                onUploadClick = { 
                     if (selectedImages.size < maxImages && !isAnalyzing) galleryLauncher.launch("image/*") 
                }
            )
            
            // Validation Status UI
            AnimatedVisibility(visible = isAnalyzing || analysisResult != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isAnalyzing) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = AccentBlue,
                            trackColor = BorderLight
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.upload_analyzing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    } else if (analysisResult != null) {
                        Text(
                            text = analysisResult!!,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSkinValid) Color(0xFF4CAF50) else Color.Red, // Green for success, Red for fail
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Camera Button — opens camera directly
                Button(
                    onClick = {
                        if (selectedImages.size < maxImages && !isAnalyzing) {
                            launchCamera()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.upload_btn_camera))
                }
                
                // Gallery Button — opens photo picker
                OutlinedButton(
                    onClick = {
                        if (selectedImages.size < maxImages && !isAnalyzing) {
                            galleryLauncher.launch("image/*")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = BorderStroke(1.dp, BorderLight)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.upload_btn_gallery))
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Uploaded Photos Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.upload_list_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${selectedImages.size}/$maxImages",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier
                        .background(BorderLight, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Photo Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (i in 0 until maxImages) {
                    PhotoSlot(
                        imageUri = selectedImages.getOrNull(i),
                        onRemove = { 
                            if (i < selectedImages.size) selectedImages.removeAt(i) 
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Tip Card
            TipCard()
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadTopBar(onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.upload_top_bar),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.btn_back)
                )
            }
        },
        actions = {
            IconButton(onClick = { /* Todo: Show info dialog */ }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = AccentBlue
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = BackgroundMain,
            titleContentColor = TextPrimary
        )
    )
}

@Composable
fun InstructionCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(AccentBlue.copy(alpha = 0.05f))
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .background(AccentBlue)
        )
        Text(
            text = stringResource(R.string.upload_instruction),
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.padding(16.dp),
            lineHeight = 20.sp
        )
    }
}

@Composable
fun UploadZone(onUploadClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(MaterialTheme.shapes.large)
            .background(AccentBlue.copy(alpha = 0.03f))
            .clickable(onClick = onUploadClick)
            .border(BorderStroke(1.5.dp, AccentBlue.copy(alpha = 0.3f)), MaterialTheme.shapes.large),
            contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
             Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(AccentBlue.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add, // Replaced CloudUpload
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.upload_zone_hint),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.upload_formats),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PhotoSlot(
    imageUri: Uri?,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium)
            .background(BackgroundCard)
            .border(1.dp, BorderLight, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            Image(
                painter = rememberAsyncImagePainter(imageUri),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Remove Button
            Box(
                 modifier = Modifier.fillMaxSize(),
                 contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp).padding(4.dp).background(Color.White, CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_remove), modifier = Modifier.size(12.dp))
                }
            }
        } else {
             Icon(
                imageVector = Icons.Default.Add, // Replaced PhotoCamera
                contentDescription = null,
                tint = TextTertiary.copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun TipCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6)) // Light Yellow
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top // Fixed crossAxisAlignment
        ) {
            Icon(
                imageVector = Icons.Default.Info, // Replaced Lightbulb
                contentDescription = null,
                tint = Color(0xFFF59E0B), // Amber
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                     text = stringResource(R.string.upload_tip_title),
                     style = MaterialTheme.typography.bodySmall,
                     fontWeight = FontWeight.Bold,
                     color = Color(0xFFB45309) // Dark Amber
                )
                Text(
                    text = stringResource(R.string.upload_tip_msg),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB45309),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
