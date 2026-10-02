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
 * Removes ALL legacy player-placed block markers from chunk PDC.
 *
 * Since eco now uses mcMMO's BlockTracker instead of PDC storage, this cleaner removes
 * all eco block markers left by older versions. It performs a full cleanup by removing
 * any PDC key matching the block marker format (hex hash), regardless of whether the
 * block still exists.
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
     * Clean a chunk, removing ALL eco block markers; must be called on the thread owning it.
     *
     * @return The number of markers removed.
     */
    fun clean(chunk: Chunk): Int {
        val pdc = chunk.persistentDataContainer
        if (pdc.isEmpty) {
            return 0
        }

        val namespace = plugin.namespacedKeyFactory.create("dummy").namespace
        val keys = pdc.keys.filter { it.namespace == namespace }

        if (keys.isEmpty()) {
            return 0
        }

        var count = 0
        val blockKeyPattern = Regex("^-?[0-9a-f]{1,8}$")

        for (key in keys) {
            // Match block marker format (hex hash)
            if (blockKeyPattern.matches(key.key)) {
                if (pdc.get(key, PersistentDataType.INTEGER) == 1) {
                    pdc.remove(key)
                    count++
                }
            }
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

    /** Log and reset progress since the last report. */
    fun report() {
        val removedCount = removed.getAndSet(0)
        val scannedCount = scanned.getAndSet(0)

        if (removedCount > 0) {
            plugin.logger.info(
                "Removed $removedCount legacy PDC block markers from $scannedCount chunks"
            )
        }
    }
}
