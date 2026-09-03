package com.willfp.eco.internal.spigot.drops

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.internal.drops.EcoDropQueue
import com.willfp.eco.internal.drops.EcoFastCollatedDropQueue

class CollatedRunnable(plugin: EcoPlugin) {
    init {
        plugin.scheduler.runTimer({
            val keys = EcoFastCollatedDropQueue.COLLATED_MAP.keys.toList()
            for (key in keys) {
                val value = EcoFastCollatedDropQueue.COLLATED_MAP.remove(key) ?: continue
                plugin.scheduler.runAtLocation(value.location) {
                    val queue = EcoDropQueue(value.player)
                        .setLocation(value.location.clone())
                        .addItems(value.drops)
                        .addXP(value.xp)

                    if (value.telekinetic) {
                        queue.forceTelekinesis()
                    }

                    queue.push()
                }
            }
        }, 0, 1)
    }
}
