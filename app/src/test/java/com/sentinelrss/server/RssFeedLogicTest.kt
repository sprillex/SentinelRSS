package com.sentinelrss.server

import org.junit.Assert.assertEquals
import org.junit.Test

class RssFeedLogicTest {
    @Test
    fun testRssScoreConversion() {
        // Test minrating to minScore conversion logic used in the Ktor endpoint

        // Simulating the logic:
        // val minScore = (minRatingStr?.toFloatOrNull() ?: 0f) / 10f

        fun parseMinRating(rating: String?): Float {
            return (rating?.toFloatOrNull() ?: 0f) / 10f
        }

        fun parseMaxRating(rating: String?): Float {
            return (rating?.toFloatOrNull() ?: 10f) / 10f
        }

        assertEquals(0.7f, parseMinRating("7"), 0.001f)
        assertEquals(0.0f, parseMinRating(null), 0.001f)

        assertEquals(1.0f, parseMaxRating("10"), 0.001f)
        assertEquals(1.0f, parseMaxRating(null), 0.001f)
        assertEquals(0.5f, parseMaxRating("5"), 0.001f)
    }

    @Test
    fun testDiscoveryFlag() {
        // Simulating the logic:
        // val includeCulled = discStr?.lowercase() != "no"

        fun parseDiscovery(discStr: String?): Boolean {
            return discStr?.lowercase() != "no"
        }

        assertEquals(true, parseDiscovery(null))
        assertEquals(true, parseDiscovery("yes"))
        assertEquals(false, parseDiscovery("no"))
        assertEquals(false, parseDiscovery("NO"))
    }
}
