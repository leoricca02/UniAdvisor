package com.riccaturrini.uniadvisor.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.riccaturrini.uniadvisor.data.Course
import com.riccaturrini.uniadvisor.data.Note
import com.riccaturrini.uniadvisor.data.UploadNoteState
import com.riccaturrini.uniadvisor.ui.activity.PdfViewerActivity
import com.riccaturrini.uniadvisor.viewmodel.*

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    navController: NavController, // Necessario per la navigazione alla Camera
    authViewModel: AuthViewModel,
    notesViewModel: NotesViewModel = viewModel(),
    courseViewModel: CourseViewModel = viewModel()
) {
    val notesState by notesViewModel.notesState.collectAsState()
    val uploadState by notesViewModel.uploadState.collectAsState()
    val deleteState by notesViewModel.deleteState.collectAsState()
    val currentUser by authViewModel.currentUserData.collectAsState()

    var showUploadDialog by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    // --- LOGICA SCANNER (Gestione URI di ritorno) ---
    var scannedPdfUri by remember { mutableStateOf<Uri?>(null) }

    val currentBackStackEntry = navController.currentBackStackEntry
    val savedStateHandle = currentBackStackEntry?.savedStateHandle

    // Ascolta il risultato "scanned_pdf_uri" dalla schermata Camera
    val scannedUriString by savedStateHandle?.getLiveData<String>("scanned_pdf_uri")
        ?.observeAsState() ?: mutableStateOf(null)

    LaunchedEffect(scannedUriString) {
        scannedUriString?.let { uriStr ->
            scannedPdfUri = Uri.parse(uriStr)
            showUploadDialog = true // Riapre automaticamente il dialog con il file pronto
            savedStateHandle?.remove<String>("scanned_pdf_uri")
        }
    }
    // ------------------------------------------------

    // Carica le note all'avvio
    LaunchedEffect(Unit) {
        notesViewModel.loadMyNotes()
    }

    // Gestione successo Upload
    LaunchedEffect(uploadState) {
        if (uploadState is UploadNoteState.Success) {
            showUploadDialog = false
            scannedPdfUri = null
            notesViewModel.resetUploadState()
        }
    }

    // Gestione successo Eliminazione
    LaunchedEffect(deleteState) {
        if (deleteState is DeleteNoteState.Success) {
            noteToDelete = null
            notesViewModel.resetDeleteState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Notes") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { notesViewModel.loadMyNotes() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scannedPdfUri = null // Reset per nuovo caricamento
                    showUploadDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Upload Note")
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (notesState) {
                is NotesUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Loading notes...")
                        }
                    }
                }

                is NotesUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyNotesState()
                    }
                }

                is NotesUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = (notesState as NotesUiState.Error).message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { notesViewModel.loadMyNotes() }) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                }

                is NotesUiState.Success -> {
                    val notes = (notesState as NotesUiState.Success).notes

                    SwipeRefresh(
                        state = rememberSwipeRefreshState(isRefreshing = false),
                        onRefresh = { notesViewModel.loadMyNotes() }
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(notes) { note ->
                                NoteCard(
                                    note = note,
                                    onDownload = { /* Handled inside NoteCard */ },
                                    onDelete = { noteToDelete = note }
                                )
                            }

                            // Bottom spacing for FAB
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }

            // --- DIALOG DI UPLOAD (IBRIDO: CAMERA + FILE + CORSO) ---
            if (showUploadDialog) {
                UploadNoteDialog(
                    onDismiss = {
                        showUploadDialog = false
                        scannedPdfUri = null
                        notesViewModel.resetUploadState()
                    },
                    onUpload = { fileUri, courseId, description, fileName ->
                        notesViewModel.uploadNote(fileUri, courseId, description, fileName)
                    },
                    onScanClick = {
                        // Chiudi il dialog e vai alla camera
                        showUploadDialog = false
                        navController.navigate("camera_ocr")
                    },
                    uploadState = uploadState,
                    facultyId = currentUser?.faculty_id,
                    courseViewModel = courseViewModel,
                    initialUri = scannedPdfUri // Passa il file scansionato se c'è
                )
            }

            // Delete Confirmation Dialog
            if (noteToDelete != null) {
                DeleteNoteConfirmDialog(
                    note = noteToDelete!!,
                    onConfirm = {
                        notesViewModel.deleteNote(noteToDelete!!.id)
                    },
                    onDismiss = { noteToDelete = null },
                    deleteState = deleteState
                )
            }
        }
    }
}

@Composable
fun NoteCard(
    note: Note,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    // 1. Logica per la Data e il Titolo
    val formattedDate = remember(note.created_at) {
        formatReviewDate(note.created_at)
    }
    val noteTitle = (note.description ?: "").ifBlank { "Untitled Note" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header con Titolo (Descrizione) e Data
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lato Sinistro: Icona e Titolo (Descrizione della nota)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    // ⬅️ MODIFICA: La descrizione è il nuovo titolo
                    Text(
                        text = noteTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2, // Limita a due linee
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Data (allineata a destra)
                Text(
                    text = formattedDate, // ⬅️ MODIFICA: Data formattata
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
            }

            HorizontalDivider()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = note.course_name ?: "Unknown Course",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(context, PdfViewerActivity::class.java).apply {
                            putExtra("PDF_URL", note.file_id)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Preview")
                }

                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun EmptyNotesState() {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "No notes yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Upload your first course note!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// --- DIALOG DI UPLOAD (COMPLETO E UNIFICATO) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadNoteDialog(
    onDismiss: () -> Unit,
    onUpload: (Uri, Int, String, String) -> Unit,
    onScanClick: () -> Unit, // Callback per scanner
    uploadState: UploadNoteState,
    facultyId: Int?,
    courseViewModel: CourseViewModel = viewModel(),
    initialUri: Uri? = null // URI opzionale
) {
    var selectedFileUri by remember { mutableStateOf(initialUri) }
    var selectedFileName by remember { mutableStateOf(initialUri?.lastPathSegment ?: "") }
    var selectedCourse by remember { mutableStateOf<Course?>(null) }
    var description by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    // Se abbiamo initialUri (dalla camera), saltiamo la fase di scelta iniziale
    var showUploadOptions by remember { mutableStateOf(initialUri == null) }

    val courseListState by courseViewModel.courseListState.collectAsState()

    // Aggiorna se cambia initialUri (es. tornando dalla fotocamera)
    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            selectedFileUri = initialUri
            selectedFileName = initialUri.lastPathSegment ?: "scanned_document.pdf"
            showUploadOptions = false // Vai al form
        }
    }

    LaunchedEffect(facultyId) {
        facultyId?.let { courseViewModel.loadCoursesByFaculty(it) }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            selectedFileName = it.lastPathSegment ?: "note.pdf"
            showUploadOptions = false // Vai al form
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Note") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (showUploadOptions) {
                    // --- FASE 1: SCEGLI METODO (Camera vs File) ---
                    Text(
                        text = "Choose upload method:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Opzione 1: Scanner
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss() // Chiudi dialog per navigare
                                onScanClick()
                            },
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Scan with Camera", fontWeight = FontWeight.Bold)
                                Text("Take photos & convert", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Opzione 2: File Picker
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                filePickerLauncher.launch("application/pdf")
                            },
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Select PDF File", fontWeight = FontWeight.Bold)
                                Text("Choose from device", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                } else {
                    // --- FASE 2: FORM COMPLETO (File + Corso + Descrizione) ---

                    // Display File Selezionato (con tasto per cambiarlo)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedFileName,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(onClick = {
                                selectedFileUri = null
                                showUploadOptions = true // Torna alla scelta
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    }

                    // Dropdown Corsi (Fondamentale in NotesScreen)
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCourse?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Course") },
                            placeholder = { Text("Choose a course") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )

                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            when (val state = courseListState) {
                                is CourseListState.Success -> {
                                    if (state.courses.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No courses available") },
                                            onClick = { }
                                        )
                                    } else {
                                        state.courses.forEach { courseWithRatings ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(
                                                            text = courseWithRatings.course.name,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = "Prof. ${courseWithRatings.teacherName}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    selectedCourse = courseWithRatings.course
                                                    expandedDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                                is CourseListState.Loading -> {
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                                Text("Loading courses...")
                                            }
                                        },
                                        onClick = { }
                                    )
                                }
                                is CourseListState.Error -> {
                                    DropdownMenuItem(
                                        text = { Text("Error loading courses") },
                                        onClick = { }
                                    )
                                }
                            }
                        }
                    }

                    // Descrizione
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optional)") },
                        placeholder = { Text("Brief description of the notes...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    // Upload progress
                    when (uploadState) {
                        is UploadNoteState.Uploading -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = uploadState.progress / 100f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "Uploading... ${uploadState.progress}%",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        is UploadNoteState.Error -> {
                            Text(
                                text = uploadState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        else -> {}
                    }

                    // Faculty warning
                    if (facultyId == null) {
                        Text(
                            text = "⚠️ Please select a faculty in your profile first",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!showUploadOptions) {
                Button(
                    onClick = {
                        selectedFileUri?.let { uri ->
                            selectedCourse?.let { course ->
                                onUpload(uri, course.id, description.ifBlank { "" }, selectedFileName)
                            }
                        }
                    },
                    enabled = uploadState !is UploadNoteState.Uploading &&
                            selectedFileUri != null &&
                            selectedCourse != null &&
                            facultyId != null
                ) {
                    if (uploadState is UploadNoteState.Uploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Upload")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = uploadState !is UploadNoteState.Uploading
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteNoteConfirmDialog(
    note: Note,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    deleteState: DeleteNoteState
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Delete Note?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Are you sure you want to delete this note?")

                if (!note.description.isNullOrBlank()) {
                    Text(
                        text = "\"${note.description}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (deleteState is DeleteNoteState.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = deleteState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = deleteState !is DeleteNoteState.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (deleteState is DeleteNoteState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = deleteState !is DeleteNoteState.Loading
            ) {
                Text("Cancel")
            }
        }
    )
}