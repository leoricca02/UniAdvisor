package com.riccaturrini.uniadvisor.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.riccaturrini.uniadvisor.data.Course
import com.riccaturrini.uniadvisor.data.Note
import com.riccaturrini.uniadvisor.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
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

    // Load notes on start
    LaunchedEffect(Unit) {
        notesViewModel.loadMyNotes()
    }

    // Handle upload success
    LaunchedEffect(uploadState) {
        if (uploadState is UploadNoteState.Success) {
            showUploadDialog = false
            notesViewModel.resetUploadState()
        }
    }

    // Handle delete success
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
                onClick = { showUploadDialog = true },
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

            // Upload Dialog
            if (showUploadDialog) {
                UploadNoteDialog(
                    onDismiss = {
                        showUploadDialog = false
                        notesViewModel.resetUploadState()
                    },
                    onUpload = { fileUri, courseId, description, fileName ->
                        notesViewModel.uploadNote(fileUri, courseId, description, fileName)
                    },
                    uploadState = uploadState,
                    facultyId = currentUser?.faculty_id,
                    courseViewModel = courseViewModel
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Note",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = note.created_at,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Description
            if (!note.description.isNullOrBlank()) {
                Text(
                    text = note.description,
                    style = MaterialTheme.typography.bodyMedium
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
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(note.file_id))  // ✅ CORRETTO
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View PDF")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadNoteDialog(
    onDismiss: () -> Unit,
    onUpload: (Uri, Int, String, String) -> Unit,
    uploadState: UploadNoteState,
    facultyId: Int?,
    courseViewModel: CourseViewModel = viewModel()
) {
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedCourse by remember { mutableStateOf<Course?>(null) }
    var description by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    val courseListState by courseViewModel.courseListState.collectAsState()

    // Load courses when dialog opens
    LaunchedEffect(facultyId) {
        facultyId?.let {
            courseViewModel.loadCoursesByFaculty(it)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            // Extract filename from URI
            selectedFileName = it.lastPathSegment ?: "note.pdf"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Upload, contentDescription = null)
        },
        title = { Text("Upload Note") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // File selector
                OutlinedButton(
                    onClick = { filePickerLauncher.launch("application/pdf") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedFileName.isNotEmpty()) selectedFileName else "Select PDF file"
                    )
                }

                // Course Dropdown
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

                // Description
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
        },
        confirmButton = {
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