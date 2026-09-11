package com.example.notivib

import com.example.notivib.framework.utils.NotificationTextExtractor
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationTextExtractorTest {

    @Test
    fun `combine joins non-blank fragments with spaces`() {
        val result = NotificationTextExtractor.combine(listOf("Hello", "World"))
        assertEquals("Hello World", result)
    }

    @Test
    fun `combine skips blank and null fragments`() {
        val result = NotificationTextExtractor.combine(listOf("", null, "  ", "Slack message"))
        assertEquals("Slack message", result)
    }

    @Test
    fun `combine drops exact duplicate fragments while preserving order`() {
        val result = NotificationTextExtractor.combine(
            listOf("New message", "New message", "from Alice")
        )
        assertEquals("New message from Alice", result)
    }

    @Test
    fun `combine trims surrounding whitespace on each fragment`() {
        val result = NotificationTextExtractor.combine(listOf("  padded  ", "text"))
        assertEquals("padded text", result)
    }

    @Test
    fun `combine returns empty string when all fragments are blank`() {
        val result = NotificationTextExtractor.combine(listOf("", null, "   "))
        assertEquals("", result)
    }

    @Test
    fun `preview returns text unchanged when within max length`() {
        val result = NotificationTextExtractor.preview("short text", maxLen = 40)
        assertEquals("short text", result)
    }

    @Test
    fun `preview truncates long text and appends ellipsis`() {
        val longText = "a".repeat(50)
        val result = NotificationTextExtractor.preview(longText, maxLen = 40)
        assertEquals("a".repeat(40) + "…", result)
    }

    @Test
    fun `preview trims before measuring length`() {
        val result = NotificationTextExtractor.preview("   padded text   ", maxLen = 40)
        assertEquals("padded text", result)
    }
}
