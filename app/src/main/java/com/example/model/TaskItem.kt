package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class TaskItem(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Personal",
    val priority: String = "MEDIUM",
    val reminderTimestamp: Long = 0L,
    val isCompleted: Boolean = false,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "userId" to userId,
            "title" to title,
            "category" to category,
            "priority" to priority,
            "reminderTimestamp" to reminderTimestamp,
            "isCompleted" to isCompleted
        )
        if (description.isNotBlank()) {
            map["description"] = description
        }
        return map
    }
}

enum class TaskPriority(val code: String, val labelEn: String, val labelBn: String) {
    HIGH("HIGH", "High", "জরুরি (উচ্চ)"),
    MEDIUM("MEDIUM", "Medium", "সাধারণ (মাঝারি)"),
    LOW("LOW", "Low", "কম গুরুত্বপূর্ণ (নিম্ন)");

    companion object {
        fun fromCode(code: String): TaskPriority =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: MEDIUM
    }
}

enum class TaskCategory(val key: String, val labelEn: String, val labelBn: String, val iconName: String) {
    WORK("Work", "Work", "কাজ ও অফিস", "work"),
    PERSONAL("Personal", "Personal", "ব্যক্তিগত", "person"),
    STUDY("Study", "Study", "পড়াশোনা", "school"),
    HEALTH("Health", "Health", "স্বাস্থ্য ও ঔষধ", "health"),
    SHOPPING("Shopping", "Shopping", "বাজার ও কেনাকাটা", "shopping_cart"),
    OTHER("Other", "Other", "অন্যান্য", "label");

    companion object {
        fun fromKey(key: String): TaskCategory =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: OTHER
    }
}

enum class FilterTab(val labelEn: String, val labelBn: String) {
    TODAY("Today", "আজকের কাজ"),
    UPCOMING("Upcoming", "আসন্ন"),
    ALL("All", "সব কাজ"),
    COMPLETED("Completed", "সম্পন্ন")
}
