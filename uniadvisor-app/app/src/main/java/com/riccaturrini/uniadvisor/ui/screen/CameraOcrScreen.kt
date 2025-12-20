package com.riccaturrini.uniadvisor.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.riccaturrini.uniadvisor.data.*
import com.riccaturrini.uniadvisor.ui.activity.PdfViewerActivity
import com.riccaturrini.uniadvisor.viewmodel.CameraOcrViewModel
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraOcrScreen(
    courseId: Int? = null, // Reso opzionale per supportare scansioni generiche
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit = {},
    onResult: ((Uri) -> Unit)? = null, // NUOVO: Callback per restituire il PDF
    viewModel: CameraOcrViewModel = viewModel()
) {
    val context = LocalContext.current
    val capturedImages by viewModel.capturedImages.collectAsState()
    val ocrState by viewModel.ocrState.collectAsState()
    val pdfState by viewModel.pdfState.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()

    var hasCameraPermission by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(true) }
    var showPreview by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }

    // Flag per capire se stiamo generando il PDF per restituirlo o solo per preview
    var isGeneratingForReturn by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        hasCameraPermission = permission == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Gestione Successo Upload (Caso CourseDetail)
    LaunchedEffect(uploadState) {
        if (uploadState is UploadNoteState.Success) {
            onSuccess()
            viewModel.resetUploadState()
        }
    }

    // Gestione Creazione PDF (Preview o Ritorno Risultato)
    LaunchedEffect(pdfState) {
        if (pdfState is PdfGenerationState.Success) {
            val pdfUri = (pdfState as PdfGenerationState.Success).pdfUri

            if (isGeneratingForReturn && onResult != null) {
                // CASO 1: Restituisci il risultato al chiamante (NotesScreen)
                onResult(pdfUri)
            } else {
                // CASO 2: Apri la Preview
                val intent = Intent(context, PdfViewerActivity::class.java).apply {
                    putExtra("PDF_URL", pdfUri.toString())
                }
                context.startActivity(intent)
                viewModel.resetPdfState()
            }
            isGeneratingForReturn = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showCamera) "Scan Note" else "Review & Save") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showPreview) {
                            showPreview = false
                            showCamera = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                !hasCameraPermission -> {
                    CameraPermissionRequired(onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    })
                }

                showCamera && !showPreview -> {
                    CameraView(
                        onImageCaptured = { uri -> viewModel.addCapturedImage(uri) },
                        onError = { }
                    )
                    CameraControls(
                        capturedCount = capturedImages.size,
                        onDone = {
                            if (capturedImages.isNotEmpty()) {
                                showCamera = false
                                showPreview = true
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                showPreview -> {
                    OcrPreviewScreen(
                        images = capturedImages,
                        ocrState = ocrState,
                        pdfState = pdfState,
                        uploadState = uploadState,
                        description = description,
                        onDescriptionChange = { description = it },
                        onRemoveImage = { uri ->
                            viewModel.removeCapturedImage(uri)
                            if (capturedImages.size <= 1) {
                                showPreview = false
                                showCamera = true
                            }
                        },
                        onProcessOcr = { viewModel.processOcrOnImages(context) },
                        onPreviewPdf = {
                            isGeneratingForReturn = false
                            viewModel.generatePdfForPreview(context)
                        },
                        // LOGICA UNIFICATA:
                        onConfirmAction = {
                            if (onResult != null) {
                                // Se c'è un callback di risultato, Genera PDF e ritorna
                                isGeneratingForReturn = true
                                viewModel.generatePdfForPreview(context)
                            } else if (courseId != null) {
                                // Altrimenti, fai Upload diretto
                                viewModel.uploadPdfAsNote(context, courseId, description)
                            }
                        },
                        isUploadMode = onResult == null, // Cambia UI in base alla modalità
                        onAddMore = {
                            showPreview = false
                            showCamera = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun OcrPreviewScreen(
    images: List<CapturedImage>,
    ocrState: OcrProcessingState,
    pdfState: PdfGenerationState,
    uploadState: UploadNoteState,
    description: String,
    onDescriptionChange: (String) -> Unit,
    onRemoveImage: (Uri) -> Unit,
    onProcessOcr: () -> Unit,
    onPreviewPdf: () -> Unit,
    onConfirmAction: () -> Unit, // Callback unificata (Upload o Return)
    isUploadMode: Boolean,
    onAddMore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ... Images Preview Card ...
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Captured Pages (${images.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onAddMore) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Add More")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(images) { image ->
                        ImagePreviewItem(imageUri = image.uri, onRemove = { onRemoveImage(image.uri) })
                    }
                }
            }
        }

        // OCR Section
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Text Recognition (OCR)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                when (ocrState) {
                    is OcrProcessingState.Idle -> {
                        Button(onClick = onProcessOcr, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.TextFields, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Extract Text")
                        }
                    }
                    is OcrProcessingState.Processing -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            CircularProgressIndicator()
                            Text("Extracting text...")
                        }
                    }
                    is OcrProcessingState.Success -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                            Text("Text extracted (${ocrState.text.length} chars)", color = Color(0xFF4CAF50))
                        }
                    }
                    is OcrProcessingState.Error -> {
                        Text(text = ocrState.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Description Field (Solo se in modalità Upload diretto)
        if (isUploadMode) {
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Description") },
                placeholder = { Text("Add a description...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
        }

        // Action Buttons
        when (uploadState) {
            is UploadNoteState.Uploading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(progress = uploadState.progress / 100f, modifier = Modifier.fillMaxWidth())
                    Text("Uploading... ${uploadState.progress}%")
                }
            }
            is UploadNoteState.Error -> {
                Text(text = uploadState.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = onConfirmAction, modifier = Modifier.fillMaxWidth()) { Text("Retry") }
            }
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Preview Button
                    OutlinedButton(
                        onClick = onPreviewPdf,
                        modifier = Modifier.weight(1f),
                        enabled = ocrState is OcrProcessingState.Success || ocrState is OcrProcessingState.Idle
                    ) {
                        if (pdfState is PdfGenerationState.Processing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Visibility, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Preview")
                    }

                    // Confirm Button (Changes based on mode)
                    Button(
                        onClick = onConfirmAction,
                        modifier = Modifier.weight(1f),
                        enabled = ocrState is OcrProcessingState.Success || ocrState is OcrProcessingState.Idle
                    ) {
                        Icon(if (isUploadMode) Icons.Default.CloudUpload else Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isUploadMode) "Upload" else "Use Scan")
                    }
                }
            }
        }
    }
}

@Composable
fun CameraView(onImageCaptured: (Uri) -> Unit, onError: (ImageCaptureException) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preview = remember { Preview.Builder().build() }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build() }
    val cameraSelector = remember { CameraSelector.DEFAULT_BACK_CAMERA }
    LaunchedEffect(Unit) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    this.scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }.also { previewView -> preview.setSurfaceProvider(previewView.surfaceProvider) }
            },
            modifier = Modifier.fillMaxSize()
        )
        FloatingActionButton(
            onClick = {
                val photoFile = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) { onImageCaptured(Uri.fromFile(photoFile)) }
                    override fun onError(exception: ImageCaptureException) { onError(exception) }
                })
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp).size(72.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Capture", modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
fun CameraControls(capturedCount: Int, onDone: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), tonalElevation = 8.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$capturedCount page${if (capturedCount != 1) "s" else ""} captured", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Button(onClick = onDone, enabled = capturedCount > 0) {
                Icon(Icons.Default.Done, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Done")
            }
        }
    }
}

@Composable
fun ImagePreviewItem(imageUri: Uri, onRemove: () -> Unit) {
    Box {
        Image(
            painter = rememberAsyncImagePainter(imageUri),
            contentDescription = "Captured page",
            modifier = Modifier.size(120.dp).clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Crop
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd).size(28.dp).background(MaterialTheme.colorScheme.error.copy(alpha = 0.8f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun CameraPermissionRequired(onRequestPermission: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text(text = "Camera Permission Required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = "To scan notes, we need access to your camera", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onRequestPermission) { Text("Grant Permission") }
        }
    }
}

suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { future ->
        future.addListener({ continuation.resume(future.get()) }, ContextCompat.getMainExecutor(this))
    }
}