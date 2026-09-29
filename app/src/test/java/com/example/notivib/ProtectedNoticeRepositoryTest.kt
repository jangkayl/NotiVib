package com.example.notivib

import com.example.notivib.domain.repository.ProtectedNotice
import com.example.notivib.domain.repository.parseNotices
import com.example.notivib.domain.repository.removeNoticeFromList
import com.example.notivib.domain.repository.serializeNotices
import com.example.notivib.domain.repository.upsertNoticeInList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectedNoticeRepositoryTest {

    @Test
    fun parseAndSerialize_roundTripsCorrectly() {
        val notice1 = ProtectedNotice(
            key = "com.slack|urgent",
            notifId = 12345,
            packageName = "com.slack",
            appName = "Slack",
            title = "urgent",
            text = "Build failed!",
            timeMillis = 1000L,
            ruleName = "Slack Work Rule"
        )
        val notice2 = ProtectedNotice(
            key = "com.whatsapp|family",
            notifId = 67890,
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "family",
            text = "Dinner at 7",
            timeMillis = 2000L,
            ruleName = "WhatsApp Family"
        )

        val serialized = serializeNotices(listOf(notice1, notice2))
        val parsed = parseNotices(serialized)

        assertEquals(2, parsed.size)
        assertEquals(notice1, parsed[0])
        assertEquals(notice2, parsed[1])
    }

    @Test
    fun upsert_newNotice_prependsToFront() {
        val notice1 = ProtectedNotice(
            key = "key1",
            notifId = 1,
            packageName = "pkg1",
            appName = "App1",
            title = "Title1",
            text = "Text1",
            timeMillis = 1000L,
            ruleName = "Rule1"
        )
        val notice2 = ProtectedNotice(
            key = "key2",
            notifId = 2,
            packageName = "pkg2",
            appName = "App2",
            title = "Title2",
            text = "Text2",
            timeMillis = 2000L,
            ruleName = "Rule2"
        )

        val (list1, evicted1) = upsertNoticeInList(emptyList(), notice1)
        assertFalse(evicted1)
        assertEquals(1, list1.size)
        assertEquals("key1", list1[0].key)

        val (list2, evicted2) = upsertNoticeInList(list1, notice2)
        assertFalse(evicted2)
        assertEquals(2, list2.size)
        assertEquals("key2", list2[0].key)
        assertEquals("key1", list2[1].key)
    }

    @Test
    fun upsert_existingKey_replacesAndMovesToFront() {
        val notice1 = ProtectedNotice(
            key = "key1",
            notifId = 1,
            packageName = "pkg1",
            appName = "App1",
            title = "Title1",
            text = "Text1",
            timeMillis = 1000L,
            ruleName = "Rule1"
        )
        val notice2 = ProtectedNotice(
            key = "key2",
            notifId = 2,
            packageName = "pkg2",
            appName = "App2",
            title = "Title2",
            text = "Text2",
            timeMillis = 2000L,
            ruleName = "Rule2"
        )
        val notice1Updated = ProtectedNotice(
            key = "key1",
            notifId = 1,
            packageName = "pkg1",
            appName = "App1",
            title = "Title1",
            text = "Text1 Updated",
            timeMillis = 3000L,
            ruleName = "Rule1"
        )

        val (list1, _) = upsertNoticeInList(listOf(notice2, notice1), notice1Updated)
        assertEquals(2, list1.size)
        assertEquals("key1", list1[0].key)
        assertEquals("Text1 Updated", list1[0].text)
        assertEquals("key2", list1[1].key)
    }

    @Test
    fun upsert_overCapOf40_evictsOldest() {
        var current = emptyList<ProtectedNotice>()
        for (i in 1..40) {
            val notice = ProtectedNotice(
                key = "key$i",
                notifId = i,
                packageName = "pkg$i",
                appName = "App$i",
                title = "Title$i",
                text = "Text$i",
                timeMillis = i.toLong(),
                ruleName = "Rule$i"
            )
            val (updated, evicted) = upsertNoticeInList(current, notice)
            assertFalse("Should not evict when <= 40", evicted)
            current = updated
        }
        assertEquals(40, current.size)
        assertEquals("key40", current.first().key)
        assertEquals("key1", current.last().key)

        // Add 41st notice
        val notice41 = ProtectedNotice(
            key = "key41",
            notifId = 41,
            packageName = "pkg41",
            appName = "App41",
            title = "Title41",
            text = "Text41",
            timeMillis = 41L,
            ruleName = "Rule41"
        )
        val (cappedList, evicted) = upsertNoticeInList(current, notice41)
        assertTrue("Should report eviction when over 40", evicted)
        assertEquals(40, cappedList.size)
        assertEquals("key41", cappedList.first().key)
        // Oldest (key1) should have been dropped; last should now be key2
        assertEquals("key2", cappedList.last().key)
        assertFalse(cappedList.any { it.key == "key1" })
    }

    @Test
    fun remove_dropsMatchingKey() {
        val notice1 = ProtectedNotice(
            key = "key1",
            notifId = 1,
            packageName = "pkg1",
            appName = "App1",
            title = "Title1",
            text = "Text1",
            timeMillis = 1000L,
            ruleName = "Rule1"
        )
        val notice2 = ProtectedNotice(
            key = "key2",
            notifId = 2,
            packageName = "pkg2",
            appName = "App2",
            title = "Title2",
            text = "Text2",
            timeMillis = 2000L,
            ruleName = "Rule2"
        )

        val updated = removeNoticeFromList(listOf(notice1, notice2), "key1")
        assertEquals(1, updated.size)
        assertEquals("key2", updated[0].key)
    }
}
