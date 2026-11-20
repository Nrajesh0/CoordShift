package com.example.domain

import kotlin.math.abs

data class ParsedCoord(
    val latitude: Double,
    val longitude: Double,
    val suggestedLabel: String?
)

/**
 * Resilient GPS coordinate parser supporting:
 * - Decimal comma/space pairs: "35.6762, 139.6503" or "35.6762 139.6503"
 * - Labelled lines: "Tokyo: 35.6762, 139.6503" or "35.6762, 139.6503 Shibuya"
 * - Google / Apple Maps URLs: "@35.6762,139.6503", "q=35.6762,139.6503", "ll=..."
 * - DMS coordinates: 35°40'34.3"N 139°39'01.1"E
 * - Multi-line bulk pastes (extracts all valid coordinates at once)
 */
object CoordinateParser {

    private val dmsRegex = Regex(
        """(\d{1,3})\s*[°d]\s*(\d{1,2})\s*['′m]\s*(\d{1,2}(?:\.\d+)?)\s*["″s]?\s*([NSns])\s*[,;\s]+\s*(\d{1,3})\s*[°d]\s*(\d{1,2})\s*['′m]\s*(\d{1,2}(?:\.\d+)?)\s*["″s]?\s*([EWew])"""
    )

    private val decimalPairRegex = Regex(
        """(?<![\d.])([+-]?(?:90(?:\.0+)?|[0-8]?\d(?:\.\d+)?))\s*[,;\s]+\s*([+-]?(?:180(?:\.0+)?|1[0-7]\d(?:\.\d+)?|[0-9]?\d(?:\.\d+)?))(?![\d.])"""
    )

    private val mapUrlCoordRegex = Regex(
        """(?:geo:|@|[?&](?:q|ll|query|destination|center)=)([+-]?\d{1,2}\.\d+)\s*(?:,|%2C)\s*([+-]?\d{1,3}\.\d+)""",
        RegexOption.IGNORE_CASE
    )

    fun parseAll(rawInput: String): List<ParsedCoord> {
        if (rawInput.isBlank()) return emptyList()

        val results = mutableListOf<ParsedCoord>()
        val lines = rawInput
            .replace("\r\n", "\n")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        for (line in lines) {
            val extracted = parseLine(line)
            for (coord in extracted) {
                val duplicate = results.any {
                    abs(it.latitude - coord.latitude) < 1e-6 &&
                        abs(it.longitude - coord.longitude) < 1e-6
                }
                if (!duplicate) {
                    results.add(coord)
                }
            }
        }
        return results
    }

    private fun parseLine(line: String): List<ParsedCoord> {
        // 1. Check DMS format
        val dmsMatches = dmsRegex.findAll(line).toList()
        if (dmsMatches.isNotEmpty()) {
            return dmsMatches.mapNotNull { match ->
                val latDeg = match.groupValues[1].toDoubleOrNull() ?: return@mapNotNull null
                val latMin = match.groupValues[2].toDoubleOrNull() ?: return@mapNotNull null
                val latSec = match.groupValues[3].toDoubleOrNull() ?: return@mapNotNull null
                val latHem = match.groupValues[4].uppercase()

                val lonDeg = match.groupValues[5].toDoubleOrNull() ?: return@mapNotNull null
                val lonMin = match.groupValues[6].toDoubleOrNull() ?: return@mapNotNull null
                val lonSec = match.groupValues[7].toDoubleOrNull() ?: return@mapNotNull null
                val lonHem = match.groupValues[8].uppercase()

                val lat = (latDeg + latMin / 60.0 + latSec / 3600.0) * if (latHem == "S") -1 else 1
                val lon = (lonDeg + lonMin / 60.0 + lonSec / 3600.0) * if (lonHem == "W") -1 else 1
                if (isValid(lat, lon)) {
                    ParsedCoord(lat, lon, extractLabel(line, match.value))
                } else null
            }
        }

        // 2. Check map URLs
        val urlMatches = mapUrlCoordRegex.findAll(line).toList()
        if (urlMatches.isNotEmpty()) {
            return urlMatches.mapNotNull { match ->
                val lat = match.groupValues[1].toDoubleOrNull() ?: return@mapNotNull null
                val lon = match.groupValues[2].toDoubleOrNull() ?: return@mapNotNull null
                if (isValid(lat, lon)) {
                    ParsedCoord(lat, lon, null)
                } else null
            }
        }

        // 3. Check standard decimal pairs (require at least one decimal point or comma separator
        // to avoid accidentally parsing random plain integers like "10 20" unless explicitly comma-separated)
        val decMatches = decimalPairRegex.findAll(line).toList()
        return decMatches.mapNotNull { match ->
            val raw = match.value
            val hasDecimalOrComma = raw.contains('.') || raw.contains(',')
            if (!hasDecimalOrComma) return@mapNotNull null
            val lat = match.groupValues[1].toDoubleOrNull() ?: return@mapNotNull null
            val lon = match.groupValues[2].toDoubleOrNull() ?: return@mapNotNull null
            if (isValid(lat, lon)) {
                val label = if (decMatches.size == 1) extractLabel(line, raw) else null
                ParsedCoord(lat, lon, label)
            } else null
        }
    }

    private fun extractLabel(line: String, matchedCoord: String): String? {
        val cleaned = line
            .replace(matchedCoord, "")
            .replace(Regex("""https?://\S+"""), "")
            .replace(Regex("""[()\[\]{}|:;\-—–/]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        return cleaned.takeIf { it.length in 2..28 && it.any(Char::isLetter) }
    }

    private fun isValid(lat: Double, lon: Double): Boolean {
        return lat in -90.0..90.0 && lon in -180.0..180.0
    }
}
