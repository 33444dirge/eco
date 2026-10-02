package com.willfp.eco.internal.spigot.data

import org.bukkit.Material

/**
 * Math for the player-placed block markers written by [PlayerBlockListener].
 *
 * Markers are keyed by `Location#hashCode` of the block location (pitch and yaw are 0), which is
 * `19^6*3 + 19^5*world + 19^4*hx + 19^3*hy + 19^2*hz` in wrapping int arithmetic. 19^3 is odd, so
 * it is invertible mod 2^32; given a hash and a column (x, z), hy - and therefore y - can be solved
 * exactly. This lets the cleaner find every position in a chunk a marker can belong to.
 */
internal object PlacedBlockKeys {
    private const val P2 = 361
    private const val P3 = 6859
    private const val P4 = 130321
    private const val P5 = 2476099
    private const val BASE = 141137643 // 19^6 * 3

    // Multiplicative inverse of 19^3 mod 2^32 (Newton iteration, wrapping arithmetic).
    private val P3_INVERSE: Int = run {
        var inv = P3
        repeat(5) { inv *= 2 - P3 * inv }
        inv
    }

    private val KEY_PATTERN = Regex("^-?[0-9a-f]{1,8}$")

    // Sorted y lookup tables per world height range; there are only a handful of distinct ranges.
    private val Y_TABLES = java.util.concurrent.ConcurrentHashMap<Long, Pair<IntArray, IntArray>>()

    /** Same value as `Location#hashCode` for a block location. */
    fun hash(worldHash: Int, x: Int, y: Int, z: Int): Int =
        BASE + P5 * worldHash + P4 * axis(x) + P3 * axis(y) + P2 * axis(z)

    /** Parse a marker key; null if it isn't in the format [PlayerBlockListener] writes. */
    fun parseKey(key: String): Int? {
        if (!KEY_PATTERN.matches(key)) {
            return null
        }

        val hash = key.toIntOrNull(16) ?: return null
        return if (hash.toString(16) == key) hash else null
    }

    /**
     * If a block turning into [material] means the placed block is gone, as opposed to changing
     * state (farmland to dirt, coral to dead coral) where the marker must survive.
     */
    fun isRemoved(material: Material): Boolean =
        material.isAir || material in REMOVED_MATERIALS

    private val REMOVED_MATERIALS = setOf(
        Material.WATER,
        Material.LAVA,
        Material.FIRE,
        Material.SOUL_FIRE
    )

    private fun axis(value: Int): Int {
        val bits = java.lang.Double.doubleToLongBits(value.toDouble())
        return (bits xor (bits ushr 32)).toInt()
    }

    data class Position(val x: Int, val y: Int, val z: Int)

    /** Resolves marker hashes to chunk-relative positions for a single chunk. */
    class ChunkResolver(
        worldHash: Int,
        chunkX: Int,
        chunkZ: Int,
        minY: Int,
        maxY: Int
    ) {
        private val columnPartial = IntArray(256)

        // (axis(y), y) pairs sorted by axis(y), for allocation-free binary search.
        private val yAxes: IntArray
        private val ys: IntArray

        init {
            val worldPart = BASE + P5 * worldHash
            for (rx in 0 until 16) {
                val xPart = worldPart + P4 * axis(chunkX * 16 + rx)
                for (rz in 0 until 16) {
                    columnPartial[rx * 16 + rz] = xPart + P2 * axis(chunkZ * 16 + rz)
                }
            }

            val table = Y_TABLES.computeIfAbsent((minY.toLong() shl 32) or (maxY.toLong() and 0xffffffffL)) {
                val sorted = (minY until maxY).sortedBy { axis(it) }
                IntArray(sorted.size) { axis(sorted[it]) } to IntArray(sorted.size) { sorted[it] }
            }
            yAxes = table.first
            ys = table.second
        }

        /** All chunk-relative positions whose block location hashes to [hash]. */
        fun resolve(hash: Int): List<Position> {
            var result: MutableList<Position>? = null

            for (rx in 0 until 16) {
                for (rz in 0 until 16) {
                    val hy = P3_INVERSE * (hash - columnPartial[rx * 16 + rz])
                    var i = yAxes.binarySearch(hy)
                    if (i < 0) {
                        continue
                    }

                    // Step back to the first equal entry in case several y share an axis value.
                    while (i > 0 && yAxes[i - 1] == hy) {
                        i--
                    }

                    while (i < yAxes.size && yAxes[i] == hy) {
                        if (result == null) {
                            result = mutableListOf()
                        }
                        result.add(Position(rx, ys[i], rz))
                        i++
                    }
                }
            }

            return result ?: emptyList()
        }
    }
}
