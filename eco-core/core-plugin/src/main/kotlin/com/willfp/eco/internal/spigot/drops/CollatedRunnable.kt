package com.willfp.eco.internal.spigot.drops

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.internal.drops.EcoDropQueue
import com.willfp.eco.internal.drops.EcoFastCollatedDropQueue

class CollatedRunnable(plugin: EcoPlugin) {
    init {
        plugin.scheduler.runTimer({
            val entries = EcoFastCollatedDropQueue.COLLATED_MAP.entries.toList()
            for ((key, value) in entries) {
                plugin.scheduler.runAtLocation(value.location) {
                    val queue = EcoDropQueue(key)
                        .setLocation(value.location)
                        .addItems(value.drops)
                        .addXP(value.xp)

                    if (value.telekinetic) {
                        queue.forceTelekinesis()
                    }

                    queue.push()
                }
                EcoFastCollatedDropQueue.COLLATED_MAP.remove(key)
            }
        }, 0, 1)
    }
}