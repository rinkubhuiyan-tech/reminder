package com.example.voice

import com.example.model.TaskCategory
import com.example.model.TaskPriority
import java.util.Calendar

data class ParsedVoiceResult(
    val title: String,
    val description: String = "",
    val category: String,
    val priority: String,
    val reminderTimestamp: Long,
    val detectedTimeDescription: String = "",
    val rawTranscript: String
)

object VoiceCommandParser {

    /**
     * Parses spoken input in Bengali or English to extract task title,
     * scheduled reminder time, priority level, and category.
     */
    fun parse(input: String): ParsedVoiceResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ParsedVoiceResult(
                title = "",
                category = TaskCategory.PERSONAL.key,
                priority = TaskPriority.MEDIUM.code,
                reminderTimestamp = 0L,
                rawTranscript = input
            )
        }

        var workingText = trimmed

        // 1. Detect Priority
        val isHighPriority = listOf("জরুরি", "urgent", "important", "high priority", "জরুরী", "গুরুত্বপূর্ণ")
            .any { workingText.contains(it, ignoreCase = true) }
        val isLowPriority = listOf("কম জরুরি", "low priority", "পরে করলেও হবে", "not urgent")
            .any { workingText.contains(it, ignoreCase = true) }

        val priority = when {
            isHighPriority -> TaskPriority.HIGH.code
            isLowPriority -> TaskPriority.LOW.code
            else -> TaskPriority.MEDIUM.code
        }

        // Clean out priority words from working title
        listOf("জরুরি", "urgent", "important", "জরুরী", "গুরুত্বপূর্ণ", "কম জরুরি", "not urgent").forEach {
            workingText = workingText.replace(Regex(it, RegexOption.IGNORE_CASE), " ")
        }

        // 2. Detect Category
        val lowerText = workingText.lowercase()
        val category = when {
            listOf("বাজার", "শপিং", "কেনাকাটা", "grocery", "shopping", "buy", "market", "চাল", "ডাল", "সবজি")
                .any { lowerText.contains(it) } -> TaskCategory.SHOPPING.key

            listOf("অফিস", "মিটিং", "কাজ", "প্রজেক্ট", "mail", "email", "office", "meeting", "work", "client", "boss", "কোডিং")
                .any { lowerText.contains(it) } -> TaskCategory.WORK.key

            listOf("পড়া", "পড়াশোনা", "ক্লাস", "পরীক্ষা", "study", "exam", "assignment", "homework", "বই", "school", "college")
                .any { lowerText.contains(it) } -> TaskCategory.STUDY.key

            listOf("ডাক্তার", "ঔষধ", "ওষুধ", "ব্যায়াম", "হাসপাতাল", "জিমে", "doctor", "medicine", "exercise", "workout", "hospital", "পানি খাওয়া", "প্রেসার")
                .any { lowerText.contains(it) } -> TaskCategory.HEALTH.key

            else -> TaskCategory.PERSONAL.key
        }

        // 3. Detect Date and Time
        val (reminderTimestamp, timeDesc, textWithoutTime) = extractDateTime(workingText)
        workingText = textWithoutTime

        // 4. Clean up title from common voice prompt boilerplates
        val phrasesToStrip = listOf(
            "একটা রিমাইন্ডার দাও", "রিমাইন্ডার দাও", "মনে করিয়ে দাও", "টাস্ক যুক্ত করো",
            "নতুন টাস্ক", "লিস্টে যোগ করো", "লিখো", "লিখে রাখো",
            "remind me to", "set a reminder to", "add a task to", "create a task to",
            "add task", "remind me", "please remind me"
        )
        for (phrase in phrasesToStrip) {
            workingText = workingText.replace(Regex("^\\s*" + Regex.escape(phrase) + "\\s*", RegexOption.IGNORE_CASE), " ")
            workingText = workingText.replace(Regex("\\s*" + Regex.escape(phrase) + "\\s*$", RegexOption.IGNORE_CASE), " ")
        }

        // Clean extra spaces, punctuation
        var cleanedTitle = workingText.replace(Regex("\\s+"), " ").trim()
        cleanedTitle = cleanedTitle.trim(',', '.', '!', '?', '-', ':')

        if (cleanedTitle.isEmpty()) {
            cleanedTitle = trimmed
        }

        return ParsedVoiceResult(
            title = cleanedTitle,
            category = category,
            priority = priority,
            reminderTimestamp = reminderTimestamp,
            detectedTimeDescription = timeDesc,
            rawTranscript = trimmed
        )
    }

    private fun extractDateTime(text: String): Triple<Long, String, String> {
        val calendar = Calendar.getInstance()
        var dateOffsetDays = 0
        var cleaned = text
        var timeDescription = ""

        // Check relative day in Bengali & English
        if (cleaned.contains("আগামীকাল", ignoreCase = true) || cleaned.contains("কাল", ignoreCase = true) || cleaned.contains("tomorrow", ignoreCase = true)) {
            dateOffsetDays = 1
            timeDescription = "আগামীকাল "
            cleaned = cleaned.replace(Regex("আগামীকাল|কাল|tomorrow", RegexOption.IGNORE_CASE), " ")
        } else if (cleaned.contains("পরশু", ignoreCase = true) || cleaned.contains("day after tomorrow", ignoreCase = true)) {
            dateOffsetDays = 2
            timeDescription = "পরশু "
            cleaned = cleaned.replace(Regex("পরশু|day after tomorrow", RegexOption.IGNORE_CASE), " ")
        } else if (cleaned.contains("আজকে", ignoreCase = true) || cleaned.contains("আজ", ignoreCase = true) || cleaned.contains("today", ignoreCase = true)) {
            dateOffsetDays = 0
            timeDescription = "আজ "
            cleaned = cleaned.replace(Regex("আজকে|আজ|today", RegexOption.IGNORE_CASE), " ")
        }

        // Extract hour and am/pm or time period
        var hour = -1
        var minute = 0
        var isPm = false
        var isAm = false

        if (cleaned.contains("সকাল", ignoreCase = true) || cleaned.contains("ভোর", ignoreCase = true) || cleaned.contains("morning", ignoreCase = true) || cleaned.contains("am", ignoreCase = true)) {
            isAm = true
        }
        if (cleaned.contains("রাত", ignoreCase = true) || cleaned.contains("বিকেল", ignoreCase = true) || cleaned.contains("সন্ধ্যা", ignoreCase = true) || cleaned.contains("দুপুর", ignoreCase = true) || cleaned.contains("night", ignoreCase = true) || cleaned.contains("evening", ignoreCase = true) || cleaned.contains("afternoon", ignoreCase = true) || cleaned.contains("pm", ignoreCase = true)) {
            isPm = true
        }

        // Look for digit patterns like "১০টায়", "10 am", "8:30 pm", "৯ টা", "5pm"
        // Also support Bengali digits 0-9: ০, ১, ২, ৩, ৪, ৫, ৬, ৭, ৮, ৯
        val normalizedDigits = convertBanglaDigitsToEnglish(cleaned)

        val timeRegex = Regex("(\\d{1,2})(?:[:.](\\d{2}))?\\s*(?:টা|টায়|ta|am|pm|o'clock)?", RegexOption.IGNORE_CASE)
        val match = timeRegex.find(normalizedDigits)

        if (match != null) {
            val matchedHour = match.groupValues[1].toIntOrNull()
            if (matchedHour != null && matchedHour in 1..24) {
                hour = matchedHour
                minute = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0

                if (isPm && hour < 12) {
                    hour += 12
                } else if (isAm && hour == 12) {
                    hour = 0
                }

                // If no AM/PM specified and hour is like 1..6, usually evening/afternoon in voice context
                if (!isAm && !isPm && hour in 1..6) {
                    hour += 12
                }

                cleaned = cleaned.replace(match.value, " ")
            }
        }

        // Clean time period words
        listOf("সকাল", "ভোর", "দুপুর", "বিকেল", "সন্ধ্যা", "রাত", "morning", "afternoon", "evening", "night", "am", "pm", "টা", "টায়").forEach {
            cleaned = cleaned.replace(Regex("\\b" + it + "\\b", RegexOption.IGNORE_CASE), " ")
            cleaned = cleaned.replace(it, " ")
        }

        if (hour != -1) {
            calendar.add(Calendar.DAY_OF_YEAR, dateOffsetDays)
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            // If scheduled time is already in the past for today, push to tomorrow
            if (dateOffsetDays == 0 && calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                timeDescription = "আগামীকাল "
            }

            val displayHour = if (hour % 12 == 0) 12 else hour % 12
            val amPmStr = if (hour >= 12) "PM" else "AM"
            timeDescription += String.format("%02d:%02d %s", displayHour, minute, amPmStr)

            return Triple(calendar.timeInMillis, timeDescription, cleaned)
        }

        // If only relative day was mentioned without specific hour, set default 9:00 AM
        if (dateOffsetDays > 0) {
            calendar.add(Calendar.DAY_OF_YEAR, dateOffsetDays)
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            timeDescription += "09:00 AM"
            return Triple(calendar.timeInMillis, timeDescription, cleaned)
        }

        return Triple(0L, "", cleaned)
    }

    private fun convertBanglaDigitsToEnglish(input: String): String {
        val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val enDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        var result = input
        for (i in bnDigits.indices) {
            result = result.replace(bnDigits[i], enDigits[i])
        }
        return result
    }
}
