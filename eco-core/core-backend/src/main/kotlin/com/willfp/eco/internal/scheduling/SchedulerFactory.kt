package com.willfp.eco.internal.scheduling

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.scheduling.Scheduler

object SchedulerFactory {
    fun createScheduler(plugin: EcoPlugin): Scheduler {
        return if (isFolia()) {
            FoliaScheduler(plugin)
        } else {
            EcoScheduler(plugin)
        }
    }

    fun isFolia(): Boolean {
        return try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }
}