package com.codepalace.accelerometer.util

import com.codepalace.accelerometer.data.local.SpaceObjectEntity

/**
 * Handles all search logic against the full cached object list.
 * Uses regex with typo-tolerance via character-level fuzzy building.
 */
class SearchManager {

    companion object {
        /** Maximum magnitude for searchable objects. Adjust as needed. */
        const val SEARCH_MAX_MAGNITUDE = 4.0

        /** Max results to surface at once. */
        const val MAX_RESULTS = 30
    }

    /**
     * Searches [allObjects] using the given [query].
     *
     * Strategy (in order of priority):
     * 1. Exact prefix match (highest rank)
     * 2. Substring match
     * 3. Fuzzy regex match — allows 1 substitution/insertion/deletion per
     *    every 3 characters in the query, built as an alternation of patterns
     *    that each omit or replace one character.
     *
     * Objects are pre-filtered by [SEARCH_MAX_MAGNITUDE].
     *
     * @return Ranked list of up to [MAX_RESULTS] matching [SearchResult]s.
     */
    fun search(query: String, allObjects: List<SpaceObjectEntity>): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val q = query.trim()

        val candidates = allObjects.filter { it.magnitude <= SEARCH_MAX_MAGNITUDE }

        val exactPrefix = mutableListOf<SpaceObjectEntity>()
        val substring   = mutableListOf<SpaceObjectEntity>()
        val fuzzy       = mutableListOf<SpaceObjectEntity>()

        val fuzzyPattern = buildFuzzyPattern(q)

        for (obj in candidates) {
            val name = obj.displayName

            when {
                name.startsWith(q, ignoreCase = true) -> exactPrefix.add(obj)

                name.contains(q, ignoreCase = true) -> substring.add(obj)

                fuzzyPattern.containsMatchIn(name) -> fuzzy.add(obj)
            }
        }

        // Sort each bucket by magnitude (brighter = lower value = first)
        val comparator = compareBy<SpaceObjectEntity> { it.magnitude }

        return (exactPrefix.sortedWith(comparator) +
                substring.sortedWith(comparator) +
                fuzzy.sortedWith(comparator))
            .take(MAX_RESULTS)
            .map { it.toSearchResult() }
    }

    /**
     * Builds a case-insensitive regex that tolerates roughly 1 error per 3
     * characters by generating patterns where each character is optionally
     * skipped (deletion) or replaced by a wildcard (substitution/insertion).
     *
     * Example for "siris":
     *   - deletion  patterns: "iris", "sris", "siis", "sirs", "siri"
     *   - wildcard  patterns: ".iris", "s.ris", "si.is", "sir.s", "siri."
     * Combined as an alternation — if ANY of them matches, it's a hit.
     */
    private fun buildFuzzyPattern(query: String): Regex {
        if (query.length <= 2) {
            // For very short queries just do a plain contains — fuzzy would
            // produce too many false positives.
            return Regex(Regex.escape(query), RegexOption.IGNORE_CASE)
        }

        val escaped = Regex.escape(query)

        val allowedErrors = (query.length / 3).coerceAtLeast(1)

        val variants = mutableListOf<String>()

        // Original exact pattern always included
        variants.add(escaped)

        repeat(allowedErrors) { _ ->
            for (i in query.indices) {
                // Deletion: remove character at position i
                val deletion = query.removeRange(i, i + 1)
                if (deletion.isNotEmpty()) {
                    variants.add(Regex.escape(deletion))
                }

                // Substitution / insertion: replace char at i with a wildcard
                val substitution = query.substring(0, i) + "." + query.substring(i + 1)
                variants.add(Regex.escape(query.substring(0, i)) +
                        "." +
                        Regex.escape(query.substring(i + 1)))

                // Insertion: insert a wildcard at position i
                val insertion = query.substring(0, i) + "." + query.substring(i)
                variants.add(
                    Regex.escape(query.substring(0, i)) +
                            ".?" +
                            Regex.escape(query.substring(i))
                )
            }
        }

        val pattern = variants.distinct().joinToString("|") { "(?i)$it" }
        return Regex(pattern, RegexOption.IGNORE_CASE)
    }

    private fun SpaceObjectEntity.toSearchResult() = SearchResult(
        id          = id,
        displayName = displayName,
        objectType  = objectType,
        magnitude   = magnitude,
        raDeg       = raDeg,
        decDeg      = decDeg
    )
}

/**
 * Lightweight model returned by [SearchManager] — decoupled from the full entity.
 */
data class SearchResult(
    val id: Long,
    val displayName: String,
    val objectType: String,
    val magnitude: Double,
    val raDeg: Double,
    val decDeg: Double
)
