package com.riccaturrini.uniadvisor.ui.activity

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
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
                        // Trigger download
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse(pdfUrl)
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
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

                // Download PDF from Firebase Storage
                val storage = FirebaseStorage.getInstance()
                val pdfRef = storage.getReferenceFromUrl(pdfUrl)
                val localFile = File(context.cacheDir, "temp_preview.pdf")

                pdfRef.getFile(localFile).await()

                // Render PDF pages
                val renderResult = withContext(Dispatchers.IO) {
                    renderPdfPages(localFile)
                }

                bitmaps = renderResult.first
                totalPages = renderResult.second
                isLoading = false

                // Clean up
                localFile.delete()
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
                        Icon(Icons.Default.Download, contentDescription = "Download")
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Loading PDF preview...")
                        Text(
                            "This may take a moment for large files",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onDownload) {
                            Text("Download Instead")
                        }
                    }
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
                    // Info banner
                    if (bitmaps.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Showing all $totalPages pages",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                TextButton(onClick = onDownload) {
                                    Text("Download PDF")
                                }
                            }
                        }
                    }

                    // Render all pages
                    bitmaps.forEachIndexed { index, bitmap ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Page ${index + 1} of ${bitmaps.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
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
                            if (index < bitmaps.size - 1) {
                                Divider(modifier = Modifier.padding(vertical = 12.dp))
                            }
                        }
                    }

                    // Download button at bottom
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download PDF")
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

        // RENDER ALL PAGES (no limit)
        for (i in 0 until totalPages) {
            val page = pdfRenderer.openPage(i)

            // Create bitmap with appropriate size
            val bitmap = Bitmap.createBitmap(
                page.width * 2, // Increase resolution for better quality
                page.height * 2,
                Bitmap.Config.ARGB_8888
            )

            // Render page to bitmap
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmaps.add(bitmap)
            page.close()

            Log.d("renderPdfPages", "Rendered page ${i + 1} of $totalPages")
        }

        pdfRenderer.close()
        fileDescriptor.close()
    } catch (e: Exception) {
        Log.e("renderPdfPages", "Error rendering PDF", e)
    }

    return Pair(bitmaps, totalPages)
}