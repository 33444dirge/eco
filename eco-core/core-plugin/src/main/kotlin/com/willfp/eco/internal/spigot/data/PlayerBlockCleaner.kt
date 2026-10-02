package com.willfp.eco.internal.spigot.data

import com.willfp.eco.core.EcoPlugin
import org.bukkit.Chunk
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.persistence.PersistentDataType
import java.util.concurrent.atomic.AtomicLong

/**
 * Removes stale player-placed markers left behind in chunk PDC.
 *
 * Older builds didn't clear markers when a block was removed by explosions, fire, fading, fluids
 * and so on, so chunks accumulated keys pointing at air. When a chunk loads, every marker key is
 * resolved back to the positions it can belong to (see [PlacedBlockKeys]); if all of them are
 * empty the marker is dropped. Markers that could still belong to an existing block, or that
 * can't be resolved at all (not ours, different world identity), are always kept.
 */
class PlayerBlockCleaner(
    private val plugin: EcoPlugin
) : Listener {
    private val removed = AtomicLong()
    private val scanned = AtomicLong()
    private val totalRemoved = AtomicLong()
    private val totalScanned = AtomicLong()

    /** Whether chunks are cleaned as they load; starts from config, toggled by /ecocleaner. */
    @Volatile
    var isEnabled: Boolean = plugin.configYml.getBool("clean-stale-block-markers")

    /** Markers removed and chunks scanned since startup. */
    val totals: Pair<Long, Long>
        get() = totalRemoved.get() to totalScanned.get()

    @EventHandler(priority = EventPriority.MONITOR)
    fun onChunkLoad(event: ChunkLoadEvent) {
        if (event.isNewChunk || !isEnabled) {
            return
        }

        clean(event.chunk)
    }

    /**
     * Clean a chunk; must be called on the thread owning it.
     *
     * @return The number of markers removed.
     */
    fun clean(chunk: Chunk): Int {
        val pdc = chunk.persistentDataContainer
        if (pdc.isEmpty) {
            return 0
        }

        // The listener now clears markers itself, so each chunk only needs cleaning once. Check
        // this before getKeys(), which allocates a NamespacedKey for every entry in the chunk.
        val cleanedKey = plugin.namespacedKeyFactory.create(CLEANED_KEY)
        if (pdc.has(cleanedKey)) {
            return 0
        }

        val namespace = cleanedKey.namespace
        val keys = pdc.keys.filter { it.namespace == namespace }
        if (keys.isEmpty()) {
            // Only other plugins' keys; flag it so the next load doesn't list them all again.
            pdc.set(cleanedKey, PersistentDataType.BYTE, 1)
            recordScan(0)
            return 0
        }

        val world = chunk.world
        val resolver = PlacedBlockKeys.ChunkResolver(
            world.hashCode(),
            chunk.x,
            chunk.z,
            world.minHeight,
            world.maxHeight
        )

        var count = 0

        for (key in keys) {
            val hash = PlacedBlockKeys.parseKey(key.key) ?: continue

            // Only markers exactly as PlayerBlockListener writes them.
            if (pdc.get(key, PersistentDataType.INTEGER) != 1) {
                continue
            }

            val positions = resolver.resolve(hash)
            if (positions.isEmpty()) {
                continue
            }

            val stale = positions.all {
                PlacedBlockKeys.isRemoved(chunk.getBlock(it.x, it.y, it.z).type)
            }

            if (stale) {
                pdc.remove(key)
                count++
            }
        }

        // A chunk left with an empty PDC is already skipped by the isEmpty check; anything else
        // (live markers or other plugins' keys) gets the flag.
        if (!pdc.isEmpty) {
            pdc.set(cleanedKey, PersistentDataType.BYTE, 1)
        }

        recordScan(count)
        return count
    }

    private fun recordScan(count: Int) {
        scanned.incrementAndGet()
        totalScanned.incrementAndGet()
        if (count > 0) {
            removed.addAndGet(count.toLong())
            totalRemoved.addAndGet(count.toLong())
        }
    }

    private companion object {
        // Not valid hex, so it can never be mistaken for a marker.
        const val CLEANED_KEY = "placed-markers-cleaned-v1"
    }

    /** Log and reset progress since the last report. */
    fun report() {
        val removedCount = removed.getAndSet(0)
        val scannedCount = scanned.getAndSet(0)

        if (removedCount > 0) {
            plugin.logger.info(
                "Removed $removedCount stale player-placed block markers from $scannedCount chunks"
            )
        }
    }
}
