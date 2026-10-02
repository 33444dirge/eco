package com.willfp.eco.internal.spigot.data

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.util.BlockUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockMultiPlaceEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.world.StructureGrowEvent
import org.bukkit.persistence.PersistentDataType

class PlayerBlockListener(
    private val plugin: EcoPlugin
) : Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlace(event: BlockPlaceEvent) {
        val block = event.blockPlaced

        writeKey(block)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlace(event: BlockMultiPlaceEvent) {
        val block = event.blockPlaced

        writeKey(block)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val block = event.block

        this.plugin.scheduler.runAtLocation(block.location) {
            removeKey(block)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onGrow(event: StructureGrowEvent) {
        val block = event.location.block

        this.plugin.scheduler.runAtLocation(block.location) {
            removeKey(block)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onExplode(event: BlockExplodeEvent) {
        for (block in event.blockList()) {
            removeKeyNow(block)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onExplode(event: EntityExplodeEvent) {
        for (block in event.blockList()) {
            removeKeyNow(block)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBurn(event: BlockBurnEvent) {
        removeKeyNow(event.block)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onFade(event: BlockFadeEvent) {
        // Fading also covers state changes (farmland drying, coral dying) that must keep the marker.
        if (PlacedBlockKeys.isRemoved(event.newState.type)) {
            removeKeyNow(event.block)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDecay(event: LeavesDecayEvent) {
        removeKeyNow(event.block)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        // Falling blocks starting to fall, endermen picking up blocks, withers/ravagers breaking blocks.
        if (PlacedBlockKeys.isRemoved(event.to)) {
            removeKeyNow(event.block)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onFlow(event: BlockFromToEvent) {
        val source = event.block
        val target = event.toBlock

        // Dragon eggs teleport through this event; everything else is a fluid washing away the target.
        if (source.type == Material.DRAGON_EGG) {
            return
        }

        // Hot path: most flows are into air or other fluid, which can't hold a marker worth keeping.
        if (target.type.isAir || target.isLiquid) {
            return
        }

        // Fluids wash over plants constantly; only pay for a scheduled task if there's a marker.
        if (!BlockUtils.isPlayerPlaced(target)) {
            return
        }

        // The event fires before the flow happens, so check what's actually there afterwards.
        this.plugin.scheduler.runAtLocation(target.location) {
            if (PlacedBlockKeys.isRemoved(target.type)) {
                removeKey(target)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onExtend(event: BlockPistonExtendEvent) {
        val locs = mutableListOf<Location>()
        val toRemove = mutableListOf<Location>()

        for (block in event.blocks) {
            if (BlockUtils.isPlayerPlaced(block)) {
                locs.add(block.getRelative(event.direction).location)
                toRemove.add(block.location)
            }
        }

        for (loc in toRemove) {
            this.plugin.scheduler.runAtLocation(loc) {
                removeKey(loc)
            }
        }

        for (loc in locs) {
            this.plugin.scheduler.runAtLocation(loc) {
                writeKey(loc)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onRetract(event: BlockPistonRetractEvent) {
        val locs = mutableListOf<Location>()
        val toRemove = mutableListOf<Location>()

        for (block in event.blocks) {
            if (BlockUtils.isPlayerPlaced(block)) {
                locs.add(block.getRelative(event.direction).location)
                toRemove.add(block.location)
            }
        }

        for (loc in toRemove) {
            this.plugin.scheduler.runAtLocation(loc) {
                removeKey(loc)
            }
        }

        for (loc in locs) {
            this.plugin.scheduler.runAtLocation(loc) {
                writeKey(loc)
            }
        }
    }

    /**
     * Remove the marker for a block that the event has already committed to removing.
     *
     * Events fire on the thread owning the block, so this is normally done inline; on Folia a
     * block outside the current region (e.g. the edge of an explosion) is handed to its region.
     */
    internal fun removeKeyNow(block: Block) {
        if (this.plugin.scheduler.isFolia && !Bukkit.isOwnedByCurrentRegion(block)) {
            this.plugin.scheduler.runAtLocation(block.location) {
                removeKey(block)
            }
        } else {
            removeKey(block)
        }
    }

    private fun writeKey(block: Block) {
        writeKey(block.location)
    }

    private fun writeKey(location: Location) {
        val loc = location.hashCode().toString(16)
        location.chunk.persistentDataContainer.set(
            plugin.createNamespacedKey(loc.lowercase()),
            PersistentDataType.INTEGER,
            1
        )
    }

    private fun removeKey(block: Block) {
        removeKey(block.location)
    }

    private fun removeKey(location: Location) {
        val loc = location.hashCode().toString(16)
        val key = plugin.createNamespacedKey(loc.lowercase())
        val pdc = location.chunk.persistentDataContainer

        // Explosions and fluids hit mostly unmarked blocks; don't dirty the chunk for nothing.
        if (pdc.has(key)) {
            pdc.remove(key)
        }
    }
}
