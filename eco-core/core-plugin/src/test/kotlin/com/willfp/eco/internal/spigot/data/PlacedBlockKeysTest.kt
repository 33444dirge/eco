package com.willfp.eco.internal.spigot.data

import org.bukkit.Location
import org.bukkit.World
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy

class PlacedBlockKeysTest {
    private fun world(hash: Int): World = Proxy.newProxyInstance(
        World::class.java.classLoader,
        arrayOf(World::class.java)
    ) { _, method, _ ->
        when (method.name) {
            "hashCode" -> hash
            else -> throw UnsupportedOperationException(method.name)
        }
    } as World

    @Test
    fun `hash matches Location hashCode`() {
        for (worldHash in listOf(0, 1, -123456789, 0x7fffffff)) {
            val world = world(worldHash)
            for ((x, y, z) in listOf(Triple(0, 0, 0), Triple(-1, -64, -1), Triple(12345, 319, -54321), Triple(-30000000, 100, 30000000))) {
                val expected = Location(world, x.toDouble(), y.toDouble(), z.toDouble()).hashCode()
                assertEquals(expected, PlacedBlockKeys.hash(worldHash, x, y, z))
            }
        }
    }

    @Test
    fun `resolver finds the original position`() {
        val worldHash = 987654321
        for ((chunkX, chunkZ) in listOf(0 to 0, -1 to -1, 1234 to -5678)) {
            val resolver = PlacedBlockKeys.ChunkResolver(worldHash, chunkX, chunkZ, -64, 320)
            for (rx in listOf(0, 7, 15)) {
                for (rz in listOf(0, 3, 15)) {
                    for (y in listOf(-64, -1, 0, 1, 63, 64, 319)) {
                        val hash = PlacedBlockKeys.hash(worldHash, chunkX * 16 + rx, y, chunkZ * 16 + rz)
                        val positions = resolver.resolve(hash)
                        assertTrue(PlacedBlockKeys.Position(rx, y, rz) in positions, "missing $rx $y $rz in $positions")
                        for (p in positions) {
                            assertEquals(hash, PlacedBlockKeys.hash(worldHash, chunkX * 16 + p.x, p.y, chunkZ * 16 + p.z))
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `resolver ignores other worlds`() {
        val hash = PlacedBlockKeys.hash(1, 5, 70, 5)
        val resolver = PlacedBlockKeys.ChunkResolver(2, 0, 0, -64, 320)
        assertTrue(resolver.resolve(hash).none { it == PlacedBlockKeys.Position(5, 70, 5) })
    }

    @Test
    fun `parse key round trips listener format`() {
        for (hash in listOf(0, 1, -1, 0x7fffffff, Int.MIN_VALUE, 0x1a2b3c)) {
            val key = hash.toString(16).lowercase()
            assertEquals(hash, PlacedBlockKeys.parseKey(key))
        }
        assertNull(PlacedBlockKeys.parseKey("shot-from"))
        assertNull(PlacedBlockKeys.parseKey("00ff"))
        assertNull(PlacedBlockKeys.parseKey("123456789"))
        assertNull(PlacedBlockKeys.parseKey(""))
    }
}
