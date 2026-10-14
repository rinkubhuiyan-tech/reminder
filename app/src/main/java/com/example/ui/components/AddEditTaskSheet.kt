package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.TaskCategory
import com.example.model.TaskItem
import com.example.model.TaskPriority
import com.example.ui.theme.PriorityHighColor
import com.example.ui.theme.PriorityLowColor
import com.example.ui.theme.PriorityMediumColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTaskSheet(
    task: TaskItem?,
    isBengali: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, category: String, priority: String, reminderTimestamp: Long) -> Unit,
    onOpenVoiceInput: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var title by remember(task) { mutableStateOf(task?.title ?: "") }
    var description by remember(task) { mutableStateOf(task?.description ?: "") }
    var selectedCategory by remember(task) { mutableStateOf(task?.category ?: TaskCategory.PERSONAL.key) }
    var selectedPriority by remember(task) { mutableStateOf(task?.priority ?: TaskPriority.MEDIUM.code) }
    var reminderTimestamp by remember(task) { mutableLongStateOf(task?.reminderTimestamp ?: 0L) }

    var titleError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (task == null) {
                        if (isBengali) "নতুন টাস্ক ও রিমাইন্ডার" else "New Task & Reminder"
                    } else {
                        if (isBengali) "টাস্ক সম্পাদনা" else "Edit Task"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenVoiceInput,
                        modifier = Modifier.testTag("sheet_voice_input_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Voice Input",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Task Title TextField
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) titleError = false
                },
                label = { Text(if (isBengali) "কাজের শিরোনাম *" else "Task Title *") },
                placeholder = { Text(if (isBengali) "যেমন: কাল সকাল ১০টায় মিটিং" else "e.g., Team meeting tomorrow 10am") },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text(if (isBengali) "শিরোনাম লিখুন" else "Please enter a title") }
                } else null,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Task Description TextField
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(if (isBengali) "বিস্তারিত নোট (ঐচ্ছিক)" else "Notes / Description (Optional)") },
                placeholder = { Text(if (isBengali) "প্রয়োজনীয় তথ্য লিখে রাখুন..." else "Add any extra details...") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_description_input")
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Category Selection
            Text(
                text = if (isBengali) "ক্যাটাগরি" else "Category",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_selector"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskCategory.entries.forEach { cat ->
                    val isSelected = selectedCategory.equals(cat.key, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat.key },
                        label = { Text(if (isBengali) cat.labelBn else cat.labelEn) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Priority Selection
            Text(
                text = if (isBengali) "কাজের অগ্রাধিকার" else "Priority",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("priority_selector"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskPriority.entries.forEach { prio ->
                    val isSelected = selectedPriority == prio.code
                    val prioColor = when (prio) {
                        TaskPriority.HIGH -> PriorityHighColor
                        TaskPriority.MEDIUM -> PriorityMediumColor
                        TaskPriority.LOW -> PriorityLowColor
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPriority = prio.code },
                        label = { Text(if (isBengali) prio.labelBn else prio.labelEn) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = prioColor.copy(alpha = 0.2f),
                            selectedLabelColor = prioColor
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Reminder Date & Time Selection
            Text(
                text = if (isBengali) "রিমাইন্ডার ও নোটিফিকেশন" else "Reminder & Notification",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Display current selected reminder
            if (reminderTimestamp > 0L) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Alarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val dateFormat = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
                            Text(
                                text = dateFormat.format(Date(reminderTimestamp)),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(
                            onClick = { reminderTimestamp = 0L },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear reminder",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Quick reminder buttons & custom date picker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Custom picker button
                OutlinedButton(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        if (reminderTimestamp > 0L) {
                            calendar.timeInMillis = reminderTimestamp
                        }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                calendar.set(Calendar.YEAR, year)
                                calendar.set(Calendar.MONTH, month)
                                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        calendar.set(Calendar.MINUTE, minute)
                                        calendar.set(Calendar.SECOND, 0)
                                        reminderTimestamp = calendar.timeInMillis
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                ) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isBengali) "কাস্টম সময়" else "Pick Time")
                }

                // Quick shortcuts
                val quickOptions = listOf(
                    Pair(if (isBengali) "আজ বিকাল ৫টা" else "Today 5 PM", {
                        Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 17)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }.timeInMillis
                    }),
                    Pair(if (isBengali) "আজ রাত ৮টা" else "Today 8 PM", {
                        Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 20)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }.timeInMillis
                    }),
                    Pair(if (isBengali) "কাল সকাল ৯টা" else "Tomorrow 9 AM", {
                        Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }.timeInMillis
                    }),
                    Pair(if (isBengali) "কাল বিকাল ৪টা" else "Tomorrow 4 PM", {
                        Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                            set(Calendar.HOUR_OF_DAY, 16)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }.timeInMillis
                    })
                )

                quickOptions.forEach { (label, getTimestamp) ->
                    OutlinedButton(onClick = { reminderTimestamp = getTimestamp() }) {
                        Text(label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Save Task Button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        onSave(title, description, selectedCategory, selectedPriority, reminderTimestamp)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_task_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (task == null) {
                        if (isBengali) "টাস্ক সংরক্ষণ করুন" else "Save Task"
                    } else {
                        if (isBengali) "আপডেট সংরক্ষণ করুন" else "Update Task"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
