package com.riccaturrini.uniadvisor.ui.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.storage.FirebaseStorage
import com.riccaturrini.uniadvisor.ui.theme.UniAdvisorTheme
import com.riccaturrini.uniadvisor.utils.LightSensorMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

class PdfViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pdfUrl = intent.getStringExtra("PDF_URL") ?: ""

        setContent {
            val context = LocalContext.current

            // --- SENSOR INTEGRATION ---
            // 1. Init the sensor monitor
            val sensorMonitor = remember { LightSensorMonitor(context) }

            // 2. Get system default preference as initial state
            val systemDark = isSystemInDarkTheme()

            // 3. Collect sensor flow (True = Dark Environment/Mode, False = Light)
            val isDarkEnv by sensorMonitor.isDarkEnvironment.collectAsState(initial = systemDark)

            // 4. Apply Theme dynamically
            UniAdvisorTheme(darkTheme = isDarkEnv) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PdfViewerScreen(
                        pdfUrl = pdfUrl,
                        isDarkMode = isDarkEnv, // Pass this to invert PDF colors
                        onBackPressed = { finish() },
                        onDownload = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(pdfUrl), "application/pdf")
                                    flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("PdfViewerActivity", "Could not open PDF intent", e)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    pdfUrl: String,
    isDarkMode: Boolean,
    onBackPressed: () -> Unit,
    onDownload: () -> Unit
) {
    var bitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var totalPages by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Prepare ColorFilter for Night Mode (Inverts colors: White -> Black)
    val colorFilter = if (isDarkMode) {
        // Negative Color Matrix
        ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )))
    } else {
        null
    }

    LaunchedEffect(pdfUrl) {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null

                val fileToRender: File = if (pdfUrl.startsWith("http") || pdfUrl.startsWith("gs:")) {
                    // Remote file (Firebase) -> Download it
                    val storage = FirebaseStorage.getInstance()
                    val pdfRef = storage.getReferenceFromUrl(pdfUrl)
                    val localFile = File(context.cacheDir, "temp_preview.pdf")
                    pdfRef.getFile(localFile).await()
                    localFile
                } else {
                    // Local file -> Use directly
                    val uri = Uri.parse(pdfUrl)
                    if (uri.scheme == "file" && uri.path != null) {
                        File(uri.path!!)
                    } else {
                        throw IllegalArgumentException("Unsupported URI scheme: $pdfUrl")
                    }
                }

                // Render PDF pages
                val renderResult = withContext(Dispatchers.IO) {
                    renderPdfPages(fileToRender)
                }

                bitmaps = renderResult.first
                totalPages = renderResult.second
                isLoading = false

                // Only delete if it was a temp download
                if (pdfUrl.startsWith("http") || pdfUrl.startsWith("gs:")) {
                    fileToRender.delete()
                }
            } catch (e: Exception) {
                Log.e("PdfViewerScreen", "Error loading PDF", e)
                errorMessage = "Failed to load PDF: ${e.message}"
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PDF Preview")
                        if (totalPages > 0) {
                            Text(
                                text = "$totalPages pages • ${if(isDarkMode) "Night Mode" else "Day Mode"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Visual indicator for mode (Sensor Controlled)
                    Icon(
                        imageVector = Icons.Default.BrightnessMedium,
                        contentDescription = "Light Sensor",
                        tint = if(isDarkMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Default.Download, contentDescription = "Open externally")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Render all pages
                    bitmaps.forEachIndexed { index, bitmap ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Page ${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Page ${index + 1}",
                                    modifier = Modifier.fillMaxWidth(),
                                    // Apply ColorFilter if Dark Mode to invert colors (White paper -> Black paper)
                                    colorFilter = colorFilter
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

private fun renderPdfPages(file: File): Pair<List<Bitmap>, Int> {
    val bitmaps = mutableListOf<Bitmap>()
    var totalPages = 0

    try {
        val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val pdfRenderer = PdfRenderer(fileDescriptor)

        totalPages = pdfRenderer.pageCount

        for (i in 0 until totalPages) {
            val page = pdfRenderer.openPage(i)
            // Render at x2 resolution for better quality on zoom/high-dpi screens
            val bitmap = Bitmap.createBitmap(
                page.width * 2,
                page.height * 2,
                Bitmap.Config.ARGB_8888
            )
            // Render white background first (PDFs are transparent by default)
            bitmap.eraseColor(android.graphics.Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmaps.add(bitmap)
            page.close()
        }

        pdfRenderer.close()
        fileDescriptor.close()
    } catch (e: Exception) {
        Log.e("renderPdfPages", "Error rendering PDF", e)
    }

    return Pair(bitmaps, totalPages)
}