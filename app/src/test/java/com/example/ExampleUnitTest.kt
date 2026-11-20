package com.example

import com.example.data.CoordPoint
import com.example.domain.CooldownCalculator
import com.example.domain.CoordinateParser
import com.example.domain.PlaceNameResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun cooldownChartExactBreakpointsMatchReferenceImage() {
        assertEquals(60, CooldownCalculator.calculateCooldownSeconds(2.0))
        assertEquals(90, CooldownCalculator.calculateCooldownSeconds(3.0))
        assertEquals(120, CooldownCalculator.calculateCooldownSeconds(5.0))
        assertEquals(600, CooldownCalculator.calculateCooldownSeconds(18.0))
        assertEquals(1500, CooldownCalculator.calculateCooldownSeconds(81.0))
        assertEquals(2100, CooldownCalculator.calculateCooldownSeconds(100.0))
        assertEquals(5940, CooldownCalculator.calculateCooldownSeconds(1000.0))
        assertEquals(7200, CooldownCalculator.calculateCooldownSeconds(1350.0))
        assertEquals(7200, CooldownCalculator.calculateCooldownSeconds(5000.0))

        assertEquals("< 1 min", CooldownCalculator.formatCooldown(30))
        assertEquals("1 min", CooldownCalculator.formatCooldown(60))
        assertEquals("1m 30s", CooldownCalculator.formatCooldown(90))
        assertEquals("2h", CooldownCalculator.formatCooldown(7200))
    }

    @Test
    fun coordinateParserHandlesDecimalBulkUrlsGeoAndDms() {
        val bulk = """
            Tokyo: 35.6895, 139.6917
            https://maps.google.com/?q=34.6937,135.5023
            geo:-33.8688,151.2093
            37°33'59.4"N 126°58'40.8"E Seoul
        """.trimIndent()

        val parsed = CoordinateParser.parseAll(bulk)
        assertEquals(4, parsed.size)
        assertEquals("Tokyo", parsed[0].suggestedLabel)
        assertEquals(35.6895, parsed[0].latitude, 1e-4)
        assertEquals(34.6937, parsed[1].latitude, 1e-4)
        assertEquals(-33.8688, parsed[2].latitude, 1e-4)
        assertEquals(null, parsed[2].suggestedLabel)
        assertTrue(parsed[3].latitude in 37.56..37.57)
    }

    @Test
    fun routeOptimizerReducesBacktrackingCooldown() {
        val pts = listOf(
            CoordPoint("1", 35.6895, 139.6917, "Tokyo"),
            CoordPoint("2", 34.6937, 135.5023, "Osaka"),
            CoordPoint("3", 35.4437, 139.6380, "Yokohama")
        )
        val summary = CooldownCalculator.computeRouteSummary(pts, safetyBufferMinutes = 0)
        assertNotNull(summary)
        assertTrue(summary!!.optimalTotalCooldownSeconds < summary.currentTotalCooldownSeconds)
        assertEquals("Yokohama", summary.optimalOrderPoints[1].label)
    }

    @Test
    fun offlinePlaceResolverNamesMajorCitiesAndSkipsOpenOcean() {
        assertEquals("Tokyo, JP", PlaceNameResolver.lookupOfflineCity(35.6895, 139.6917))
        assertEquals("New York, US", PlaceNameResolver.lookupOfflineCity(40.7128, -74.0060))
        assertNull(PlaceNameResolver.lookupOfflineCity(0.0, -160.0))
    }
}
