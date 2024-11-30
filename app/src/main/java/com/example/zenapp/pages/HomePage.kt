@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.zenapp.pages

import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.zenapp.AuthState
import com.example.zenapp.AuthViewModel
import com.example.zenapp.ToDoItem
import com.example.zenapp.ToDoViewModel
import com.example.zenapp.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import androidx.appcompat.app.AppCompatActivity
import com.example.zenapp.CameraBool
import android.util.Log
import androidx.compose.runtime.SideEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    toDoViewModel: ToDoViewModel = viewModel(),
) {
    // Observe authentication state
    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current

    // Check for unauthenticated state and navigate to login if true
    LaunchedEffect(authState.value) {
        when (authState.value) {
            is AuthState.UnAuthenticated -> navController.navigate("login")
            else -> Unit
        }
    }

    // Variables for to-do input
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // To-do list state from ViewModel
    val toDoList by toDoViewModel.toDoList.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var selectedItemId by remember { mutableStateOf("") }
    // Coroutine scope for operations
    val scope = rememberCoroutineScope()

    // Load to-do items on page load
    LaunchedEffect(Unit) {
        toDoViewModel.loadToDoItems()
    }

    // Layout
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Home Page") },
                actions = {
                    TextButton(onClick = { authViewModel.signOut() }) {
                        Text(text = "Sign Out")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                content = {
                    Icon(Icons.Default.Add, contentDescription = "Add To-Do")
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                //Camera?
                Button(onClick = {
                    CameraBool.value = !CameraBool.value
                    val tag = "ZenApp"
                    Log.d(tag, CameraBool.value.toString())
                }) {
                    Text("Open Camera")
                }
                // Display to-do list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(toDoList) { item ->
                        ToDoItemCard(
                            item,
                            onStatusChange = { toDoViewModel.updateToDoItemStatus(item.id) },
                            onEdit = {
                                showDialog = true
                                isEditMode = true
                                selectedItemId = item.id
                                title = item.title
                                description = item.description
                            },
                            onDelete = {
                                scope.launch {
                                    toDoViewModel.deleteToDoItem(item.id)
                                }
                            }
                        )
                    }
                }
            }

            // Show dialog when FAB is pressed or for editing
            if (showDialog) {
                AddToDoDialog(
                    title = title,
                    description = description,
                    onTitleChange = { title = it },
                    onDescriptionChange = { description = it },
                    onDismiss = {
                        showDialog = false
                        isEditMode = false
                        selectedItemId = ""
                    },
                    onAdd = {
                        if (title.isNotEmpty() && description.isNotEmpty()) {
                            if (isEditMode) {
                                toDoViewModel.editToDoItem(selectedItemId, title, description)
                            } else {
                                toDoViewModel.addToDoItem(title, description)
                            }
                            title = ""
                            description = ""
                            showDialog = false
                        }
                    },
                    isEditMode = isEditMode
                )
            }
        }
    )
}

@Composable
fun AddToDoDialog(
    title: String,
    description: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    isEditMode: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditMode) "Edit To-Do" else "Add New To-Do"
            )
        },
        text = {
            Column {
                TextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onAdd) {
                Text(if (isEditMode) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ToDoItemCard(item: ToDoItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = item.title, style = MaterialTheme.typography.titleMedium)
            Text(text = item.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (item.isDone) "Done" else "Pending",
                style = MaterialTheme.typography.bodySmall
            )
            }
    }
}

@Composable
fun ToDoItemCard(item: ToDoItem, onStatusChange: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = item.title, style = MaterialTheme.typography.titleMedium)
            Text(text = item.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (item.isDone) "Done" else "Pending",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable { onStatusChange(item.id) } // Click on text only
            )
        }
    }
}

@Composable
fun ToDoItemCard(
    item: ToDoItem,
    onStatusChange: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = item.title, style = MaterialTheme.typography.titleMedium)
            Text(text = item.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (item.isDone) "Done" else "Pending",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable { onStatusChange(item.id) } // Click on text only
            )
            Row {
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
                Spacer(modifier = Modifier.width(8.dp))
                var showDeleteConfirmation by remember { mutableStateOf(false) }
                TextButton(onClick = { showDeleteConfirmation = true }) {
                    Text("Delete")
                }

                if (showDeleteConfirmation) {
                    DeleteConfirmationDialog(
                        onConfirmDelete = {
                            onDelete()
                            showDeleteConfirmation = false
                        },
                        onDismiss = { showDeleteConfirmation = false }
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title
    = { Text(text = "Confirm Delete") },
    text = { Text(text = "Are you sure you want to delete this to-do item?") },
    confirmButton = {
        Button(onClick = onConfirmDelete) {
            Text("Delete")
        }
    },
    dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    }
    )
}