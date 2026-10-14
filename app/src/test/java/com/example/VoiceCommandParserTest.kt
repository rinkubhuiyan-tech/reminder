package com.example

import com.example.model.TaskCategory
import com.example.model.TaskPriority
import com.example.util.VoiceCommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandParserTest {

    @Test
    fun parseBengaliUrgentTask() {
        val result = VoiceCommandParser.parse("জরুরি মিটিং কাল সকাল ১০টায়")
        assertEquals(TaskPriority.HIGH, result.priority)
        assertEquals(TaskCategory.WORK, result.category)
        assertTrue(result.title.contains("মিটিং"))
    }

    @Test
    fun parseShoppingTask() {
        val result = VoiceCommandParser.parse("আজ সন্ধ্যায় বাজার করতে হবে")
        assertEquals(TaskCategory.SHOPPING, result.category)
        assertTrue(result.title.isNotBlank())
    }

    @Test
    fun parseHealthDoctorTask() {
        val result = VoiceCommandParser.parse("ডাক্তারের কাছে যাওয়া")
        assertEquals(TaskCategory.HEALTH, result.category)
    }
}
