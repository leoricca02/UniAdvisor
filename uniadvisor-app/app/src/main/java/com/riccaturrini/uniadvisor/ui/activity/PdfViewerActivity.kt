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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.storage.FirebaseStorage
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
            MaterialTheme {
                PdfViewerScreen(
                    pdfUrl = pdfUrl,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    pdfUrl: String,
    onBackPressed: () -> Unit,
    onDownload: () -> Unit
) {
    var bitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var totalPages by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
                                text = "$totalPages pages",
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
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Default.Download, contentDescription = "Open externally")
                    }
                }
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
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Page ${index + 1}",
                                    modifier = Modifier.fillMaxWidth()
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
            val bitmap = Bitmap.createBitmap(
                page.width * 2,
                page.height * 2,
                Bitmap.Config.ARGB_8888
            )
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