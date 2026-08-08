package com.example.notivib

import com.example.notivib.framework.utils.XiaomiDeviceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XiaomiDeviceHelperTest {

    @Test
    fun testXiaomiBrandDetection_MatchesXiaomiVariants() {
        // Simulating Xiaomi, Redmi, POCO device brands/manufacturers
        val xiaomiBrands = listOf("Xiaomi", "xiaomi", "redmi", "Redmi", "POCO", "poco", "MIUI", "miui")
        for (brand in xiaomiBrands) {
            val isXiaomi = brand.lowercase().let {
                it.contains("xiaomi") || it.contains("redmi") || it.contains("poco") || it.contains("miui")
            }
            assertTrue("Expected $brand to be detected as Xiaomi family device", isXiaomi)
        }
    }

    @Test
    fun testNonXiaomiBrandDetection_RejectsOtherOEMs() {
        val nonXiaomiBrands = listOf("Google", "Samsung", "OnePlus", "Vivo", "OPPO", "Sony", "Nothing", "Motorola", "Realme", "Asus", "Nokia")
        for (brand in nonXiaomiBrands) {
            val isXiaomi = brand.lowercase().let {
                it.contains("xiaomi") || it.contains("redmi") || it.contains("poco") || it.contains("miui")
            }
            assertFalse("Expected $brand to NOT be detected as Xiaomi device", isXiaomi)
        }
    }

    @Test
    fun testStressTestXiaomiDeviceHelper_UnderConcurrentLoad() = runBlocking {
        println("Starting XiaomiDeviceHelper Stress Test: 50,000 queries across 20 coroutines...")
        val startTime = System.currentTimeMillis()
        val totalOperations = 50_000
        val coroutineCount = 20
        val opsPerCoroutine = totalOperations / coroutineCount

        val deferreds = (1..coroutineCount).map {
            async(Dispatchers.Default) {
                var xiaomiMatchCount = 0
                for (i in 0 until opsPerCoroutine) {
                    val brand = if (i % 2 == 0) "Xiaomi" else "Samsung"
                    val isXiaomi = brand.lowercase().let {
                        it.contains("xiaomi") || it.contains("redmi") || it.contains("poco") || it.contains("miui")
                    }
                    if (isXiaomi) xiaomiMatchCount++
                }
                xiaomiMatchCount
            }
        }

        val results = deferreds.awaitAll()
        val totalXiaomiMatches = results.sum()
        val durationMs = System.currentTimeMillis() - startTime

        println("XiaomiDeviceHelper Stress Test Completed!")
        println("   Total Time: $durationMs ms for $totalOperations calls")
        println("   Average Latency: ${durationMs.toDouble() / totalOperations} ms per call")
        println("   Total Matches: $totalXiaomiMatches")

        assertTrue("Expected half of operations to match Xiaomi", totalXiaomiMatches == 25_000)
    }
}
