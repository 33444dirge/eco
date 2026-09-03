package com.willfp.eco.internal.drops

import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

class EcoFastCollatedDropQueue(player: Player) : EcoDropQueue(player) {
    override fun push() {
        val key = CollatedDropKey(player.uniqueId, location)
        COLLATED_MAP.compute(key) { _, fetched ->
            if (fetched == null) {
                CollatedDrops(player, items.toMutableList(), location.clone(), xp, hasTelekinesis)
            } else {
                fetched.addDrops(items)
                fetched.addXp(xp)
                if (hasTelekinesis) {
                    fetched.forceTelekinesis()
                }
                fetched
            }
        }
    }

    class CollatedDrops(
        val player: Player,
        val drops: MutableList<ItemStack>,
        val location: Location,
        var xp: Int,
        var telekinetic: Boolean
    ) {
        fun addDrops(toAdd: List<ItemStack>): CollatedDrops {
            drops.addAll(toAdd)
            return this
        }

        fun addXp(xp: Int): CollatedDrops {
            this.xp += xp
            return this
        }

        fun forceTelekinesis() {
            telekinetic = true
        }
    }

    companion object {
        val COLLATED_MAP = ConcurrentHashMap<CollatedDropKey, CollatedDrops>()
    }
}

data class CollatedDropKey(
    val playerId: UUID,
    val worldId: UUID,
    val x: Double,
    val y: Double,
    val z: Double
) {
    constructor(playerId: UUID, location: Location) : this(
        playerId,
        requireNotNull(location.world) { "Drop queue location must have a world" }.uid,
        location.x,
        location.y,
        location.z
    )
}
