package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.TaskRepository
import com.example.ui.TaskReminderScreen
import com.example.ui.TaskViewModel
import com.example.ui.theme.TaskReminderTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val repository = remember { TaskRepository(applicationContext) }
            val viewModel: TaskViewModel = viewModel { TaskViewModel(repository, applicationContext) }
            val uiState by viewModel.uiState.collectAsState()

            TaskReminderTheme(darkTheme = uiState.isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TaskReminderScreen(viewModel = viewModel)
                }
            }
        }
    }
}
