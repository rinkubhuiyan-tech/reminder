package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FilterTab
import com.example.model.TaskCategory
import com.example.ui.components.AddEditTaskSheet
import com.example.ui.components.AuthDialog
import com.example.ui.components.StatsHeader
import com.example.ui.components.TaskCard
import com.example.ui.components.VoiceInputSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskReminderScreen(
    viewModel: TaskViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var isAuthDialogOpen by remember { mutableStateOf(false) }

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* handled */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Show snackbar when message changes
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isBengali) "রিমাইন্ডার ও টাস্ক" else "Daily Reminder",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                actions = {
                    // Dark mode toggle
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("dark_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (uiState.isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Toggle Dark Mode"
                        )
                    }

                    // Language toggle (বাংলা / English)
                    IconButton(
                        onClick = { viewModel.toggleLanguage() },
                        modifier = Modifier.testTag("language_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Translate,
                            contentDescription = "Language"
                        )
                    }

                    // Cloud Sync & Account
                    IconButton(
                        onClick = { isAuthDialogOpen = true },
                        modifier = Modifier.testTag("auth_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudSync,
                            contentDescription = "Cloud Sync & Account",
                            tint = if (uiState.currentUser != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                // Voice Command FAB
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openVoiceSheet() },
                    icon = { Icon(Icons.Filled.Mic, contentDescription = "Voice Input") },
                    text = { Text(if (uiState.isBengali) "ভয়েস কমান্ড" else "Voice Input") },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    modifier = Modifier.testTag("voice_input_fab")
                )

                // Add Task FAB
                FloatingActionButton(
                    onClick = { viewModel.openAddTaskSheet(null) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    modifier = Modifier.testTag("add_task_fab")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Task",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text(if (uiState.isBengali) "কাজের তালিকা খুঁজুন..." else "Search tasks...") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("search_text_field")
            )

            // Filter Tabs Row
            ScrollableTabRow(
                selectedTabIndex = FilterTab.entries.indexOf(uiState.selectedFilter),
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                FilterTab.entries.forEach { tab ->
                    val isSelected = uiState.selectedFilter == tab
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.selectFilter(tab) },
                        text = {
                            Text(
                                text = if (uiState.isBengali) tab.labelBn else tab.labelEn,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    )
                }
            }

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "All Categories" chip
                FilterChip(
                    selected = uiState.selectedCategory == null,
                    onClick = { viewModel.selectCategory(null) },
                    label = { Text(if (uiState.isBengali) "সকল ক্যাটাগরি" else "All Categories") },
                    leadingIcon = if (uiState.selectedCategory == null) {
                        { Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )

                TaskCategory.entries.forEach { cat ->
                    val isSelected = uiState.selectedCategory.equals(cat.key, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.selectCategory(if (isSelected) null else cat.key)
                        },
                        label = { Text(if (uiState.isBengali) cat.labelBn else cat.labelEn) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // Task List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("task_lazy_column"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stats Header Card at the top of the list
                item {
                    StatsHeader(
                        completedCount = uiState.todayCompletedCount,
                        totalCount = uiState.todayTotalCount,
                        isBengali = uiState.isBengali,
                        isSyncing = uiState.isSyncing,
                        isSignedIn = uiState.currentUser != null,
                        onOpenAuth = { isAuthDialogOpen = true }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (uiState.filteredTasks.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (uiState.isBengali) "কোনো কাজ পাওয়া যায়নি" else "No tasks found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (uiState.isBengali)
                                    "নিচের '+' বাটনে ট্যাপ করে অথবা মাইক দিয়ে কথা বলে নতুন কাজ যোগ করুন"
                                else
                                    "Tap the '+' button or speak via microphone to add a new task",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else {
                    items(
                        items = uiState.filteredTasks,
                        key = { it.id }
                    ) { task ->
                        TaskCard(
                            task = task,
                            isBengali = uiState.isBengali,
                            onToggleComplete = { viewModel.toggleTaskComplete(task) },
                            onEdit = { viewModel.openAddTaskSheet(task) },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Task Sheet
    if (uiState.isAddTaskSheetVisible) {
        AddEditTaskSheet(
            task = uiState.editingTask,
            isBengali = uiState.isBengali,
            onDismiss = { viewModel.closeAddTaskSheet() },
            onSave = { title, desc, cat, prio, reminderTime ->
                viewModel.saveTask(title, desc, cat, prio, reminderTime)
            },
            onOpenVoiceInput = {
                viewModel.closeAddTaskSheet()
                viewModel.openVoiceSheet()
            }
        )
    }

    // Voice Command Sheet
    if (uiState.isVoiceSheetVisible) {
        VoiceInputSheet(
            isBengali = uiState.isBengali,
            onDismiss = { viewModel.closeVoiceSheet() },
            onConfirmTask = { parsed ->
                viewModel.saveVoiceTask(parsed)
            }
        )
    }

    // Auth Dialog
    if (isAuthDialogOpen) {
        AuthDialog(
            currentUser = uiState.currentUser,
            isLoading = uiState.isAuthLoading,
            isBengali = uiState.isBengali,
            onDismiss = { isAuthDialogOpen = false },
            onSignIn = { act -> viewModel.signInWithGoogle(act) },
            onSignOut = { viewModel.signOut(context) }
        )
    }
}
