package com.example.notivib.framework.utils

/**
 * Pure helper for combining notification text fragments pulled from various notification
 * styles (plain, BigText, InboxStyle, MessagingStyle, etc.) into a single string that can be
 * scanned for keywords.
 *
 * Deliberately free of any Android types so it can be unit tested without Robolectric.
 */
object NotificationTextExtractor {

    /**
     * Combines [fragments] into a single space-separated string, trimming blanks and dropping
     * exact duplicates (e.g. the same line appearing in both EXTRA_TEXT and EXTRA_TEXT_LINES)
     * while preserving the original order of first appearance.
     */
    fun combine(fragments: List<String?>): String {
        val seen = LinkedHashSet<String>()
        for (raw in fragments) {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isEmpty()) continue
            seen.add(trimmed)
        }
        return seen.joinToString(" ")
    }

    /**
     * Returns a short, privacy-friendly preview of [text] (truncated to [maxLen] chars) suitable
     * for inclusion in a diagnostic log line.
     */
    fun preview(text: String, maxLen: Int = 40): String {
        val trimmed = text.trim()
        return if (trimmed.length <= maxLen) trimmed else trimmed.take(maxLen) + "…"
    }
}
