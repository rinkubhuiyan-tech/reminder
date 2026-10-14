package com.example.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.TaskRepository
import com.example.model.FilterTab
import com.example.model.TaskCategory
import com.example.model.TaskItem
import com.example.model.TaskPriority
import com.example.reminder.ReminderScheduler
import com.example.voice.ParsedVoiceResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

data class TaskUiState(
    val tasks: List<TaskItem> = emptyList(),
    val filteredTasks: List<TaskItem> = emptyList(),
    val selectedFilter: FilterTab = FilterTab.TODAY,
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val isDarkMode: Boolean = false,
    val isBengali: Boolean = true,
    val isAddTaskSheetVisible: Boolean = false,
    val editingTask: TaskItem? = null,
    val isVoiceSheetVisible: Boolean = false,
    val voiceTranscript: String = "",
    val parsedVoiceResult: ParsedVoiceResult? = null,
    val isListening: Boolean = false,
    val isAuthLoading: Boolean = false,
    val currentUser: FirebaseUser? = null,
    val isSyncing: Boolean = false,
    val snackbarMessage: String? = null,
    val todayCompletedCount: Int = 0,
    val todayTotalCount: Int = 0
)

class TaskViewModel(
    private val repository: TaskRepository,
    private val appContext: Context
) : ViewModel() {

    private val auth = Firebase.auth

    private val _uiState = MutableStateFlow(TaskUiState(currentUser = auth.currentUser))
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private val _rawTasks = MutableStateFlow<List<TaskItem>>(emptyList())

    init {
        // Observe auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _uiState.value = _uiState.value.copy(currentUser = user)
            if (user != null) {
                observeCloudTasks()
            } else {
                _rawTasks.value = emptyList()
            }
        }

        if (auth.currentUser != null) {
            observeCloudTasks()
        }
    }

    private fun observeCloudTasks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            repository.observeTasks()
                .catch { e ->
                    Log.e("TaskViewModel", "Error observing tasks", e)
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        snackbarMessage = if (_uiState.value.isBengali) "টাস্ক লোড করতে সমস্যা হয়েছে" else "Failed to sync tasks"
                    )
                }
                .collect { list ->
                    _rawTasks.value = list
                    _uiState.value = _uiState.value.copy(isSyncing = false)
                    updateFilteredList()
                }
        }
    }

    fun updateFilteredList() {
        val state = _uiState.value
        val allTasks = _rawTasks.value

        val now = Calendar.getInstance()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val filtered = allTasks.filter { task ->
            // Category filter
            val matchesCategory = state.selectedCategory == null || task.category.equals(state.selectedCategory, ignoreCase = true)

            // Search filter
            val matchesSearch = state.searchQuery.isBlank() ||
                    task.title.contains(state.searchQuery, ignoreCase = true) ||
                    task.description.contains(state.searchQuery, ignoreCase = true)

            // Tab filter
            val matchesTab = when (state.selectedFilter) {
                FilterTab.TODAY -> {
                    if (task.isCompleted) {
                        task.reminderTimestamp in startOfToday..endOfToday ||
                                (task.createdAt?.toDate()?.time ?: 0L) in startOfToday..endOfToday
                    } else {
                        // Pending today: reminder is today or no reminder, or overdue
                        task.reminderTimestamp == 0L || task.reminderTimestamp <= endOfToday
                    }
                }
                FilterTab.UPCOMING -> {
                    !task.isCompleted && task.reminderTimestamp > endOfToday
                }
                FilterTab.ALL -> true
                FilterTab.COMPLETED -> task.isCompleted
            }

            matchesCategory && matchesSearch && matchesTab
        }

        // Calculate today stats
        val todayTasks = allTasks.filter {
            it.reminderTimestamp in startOfToday..endOfToday ||
                    (it.createdAt?.toDate()?.time ?: 0L) in startOfToday..endOfToday ||
                    (!it.isCompleted && it.reminderTimestamp <= endOfToday)
        }
        val completedToday = todayTasks.count { it.isCompleted }
        val totalToday = todayTasks.size

        _uiState.value = state.copy(
            tasks = allTasks,
            filteredTasks = filtered,
            todayCompletedCount = completedToday,
            todayTotalCount = totalToday
        )
    }

    fun selectFilter(tab: FilterTab) {
        _uiState.value = _uiState.value.copy(selectedFilter = tab)
        updateFilteredList()
    }

    fun selectCategory(categoryKey: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = categoryKey)
        updateFilteredList()
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        updateFilteredList()
    }

    fun toggleDarkMode() {
        _uiState.value = _uiState.value.copy(isDarkMode = !_uiState.value.isDarkMode)
    }

    fun toggleLanguage() {
        _uiState.value = _uiState.value.copy(isBengali = !_uiState.value.isBengali)
    }

    fun openAddTaskSheet(task: TaskItem? = null) {
        _uiState.value = _uiState.value.copy(
            isAddTaskSheetVisible = true,
            editingTask = task
        )
    }

    fun closeAddTaskSheet() {
        _uiState.value = _uiState.value.copy(
            isAddTaskSheetVisible = false,
            editingTask = null
        )
    }

    fun openVoiceSheet() {
        _uiState.value = _uiState.value.copy(
            isVoiceSheetVisible = true,
            voiceTranscript = "",
            parsedVoiceResult = null,
            isListening = false
        )
    }

    fun closeVoiceSheet() {
        _uiState.value = _uiState.value.copy(
            isVoiceSheetVisible = false,
            parsedVoiceResult = null,
            isListening = false
        )
    }

    fun onVoiceTranscriptReceived(transcript: String) {
        val parsed = com.example.voice.VoiceCommandParser.parse(transcript)
        _uiState.value = _uiState.value.copy(
            voiceTranscript = transcript,
            parsedVoiceResult = parsed,
            isListening = false
        )
    }

    fun setVoiceListening(listening: Boolean) {
        _uiState.value = _uiState.value.copy(isListening = listening)
    }

    fun saveTask(
        title: String,
        description: String,
        category: String,
        priority: String,
        reminderTimestamp: Long
    ) {
        val user = auth.currentUser
        if (user == null) {
            showSnackbar(if (_uiState.value.isBengali) "দয়া করে প্রথমে সাইন ইন করুন" else "Please sign in to save tasks")
            return
        }

        viewModelScope.launch {
            val editing = _uiState.value.editingTask
            if (editing != null) {
                // Update
                val updated = editing.copy(
                    title = title,
                    description = description,
                    category = category,
                    priority = priority,
                    reminderTimestamp = reminderTimestamp
                )
                val result = repository.updateTask(updated)
                if (result.isSuccess) {
                    if (reminderTimestamp > System.currentTimeMillis()) {
                        ReminderScheduler.schedule(appContext, updated)
                    } else {
                        ReminderScheduler.cancel(appContext, updated.id)
                    }
                    showSnackbar(if (_uiState.value.isBengali) "টাস্ক আপডেট করা হয়েছে" else "Task updated")
                    closeAddTaskSheet()
                } else {
                    showSnackbar(if (_uiState.value.isBengali) "আপডেট করতে সমস্যা হয়েছে" else "Failed to update task")
                }
            } else {
                // Create
                val newTask = TaskItem(
                    userId = user.uid,
                    title = title,
                    description = description,
                    category = category,
                    priority = priority,
                    reminderTimestamp = reminderTimestamp,
                    isCompleted = false
                )
                val result = repository.createTask(newTask)
                if (result.isSuccess) {
                    val createdId = result.getOrThrow()
                    val taskWithId = newTask.copy(id = createdId)
                    if (reminderTimestamp > System.currentTimeMillis()) {
                        ReminderScheduler.schedule(appContext, taskWithId)
                    }
                    showSnackbar(if (_uiState.value.isBengali) "নতুন টাস্ক ও রিমাইন্ডার সংরক্ষিত হয়েছে" else "Task and reminder saved")
                    closeAddTaskSheet()
                } else {
                    showSnackbar(if (_uiState.value.isBengali) "টাস্ক সংরক্ষণ করা যায়নি" else "Failed to create task")
                }
            }
        }
    }

    fun saveVoiceTask(parsed: ParsedVoiceResult) {
        saveTask(
            title = parsed.title,
            description = parsed.description,
            category = parsed.category,
            priority = parsed.priority,
            reminderTimestamp = parsed.reminderTimestamp
        )
        closeVoiceSheet()
    }

    fun toggleTaskComplete(task: TaskItem) {
        viewModelScope.launch {
            val result = repository.toggleTaskComplete(task)
            if (result.isSuccess) {
                if (!task.isCompleted) {
                    ReminderScheduler.cancel(appContext, task.id)
                }
            } else {
                showSnackbar(if (_uiState.value.isBengali) "টাস্ক স্ট্যাটাস পরিবর্তন ব্যর্থ হয়েছে" else "Failed to toggle task")
            }
        }
    }

    fun deleteTask(task: TaskItem) {
        viewModelScope.launch {
            ReminderScheduler.cancel(appContext, task.id)
            val result = repository.deleteTask(task.id)
            if (result.isSuccess) {
                showSnackbar(if (_uiState.value.isBengali) "টাস্ক মুছে ফেলা হয়েছে" else "Task deleted")
            } else {
                showSnackbar(if (_uiState.value.isBengali) "টাস্ক মুছতে সমস্যা হয়েছে" else "Failed to delete task")
            }
        }
    }

    fun signInWithGoogle(activity: Activity) {
        _uiState.value = _uiState.value.copy(isAuthLoading = true)
        val clientId = try {
            activity.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isAuthLoading = false)
            showSnackbar("Web Client ID missing from configuration")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
        val credentialManager = CredentialManager.create(activity)

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                    _uiState.value = _uiState.value.copy(
                        isAuthLoading = false,
                        currentUser = auth.currentUser
                    )
                    observeCloudTasks()
                    showSnackbar(if (_uiState.value.isBengali) "গুগল সাইন-ইন সফল হয়েছে! ক্লাউড সিঙ্ক সক্রিয়।" else "Signed in! Cloud sync active.")
                } else {
                    _uiState.value = _uiState.value.copy(isAuthLoading = false)
                    showSnackbar("Unexpected credential type")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("TaskViewModel", "Google Sign-In cancelled: ${e.message}", e)
                _uiState.value = _uiState.value.copy(isAuthLoading = false)
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Google Sign-In failed", e)
                _uiState.value = _uiState.value.copy(isAuthLoading = false)
                showSnackbar(e.localizedMessage ?: "Sign in failed")
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error clearing credentials", e)
            } finally {
                auth.signOut()
                _rawTasks.value = emptyList()
                _uiState.value = _uiState.value.copy(currentUser = null)
                showSnackbar(if (_uiState.value.isBengali) "সাইন আউট সম্পন্ন হয়েছে" else "Signed out")
            }
        }
    }

    fun dismissSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    private fun showSnackbar(message: String) {
        _uiState.value = _uiState.value.copy(snackbarMessage = message)
    }
}
