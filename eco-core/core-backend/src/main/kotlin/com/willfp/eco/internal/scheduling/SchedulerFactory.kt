package com.willfp.eco.internal.scheduling

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.scheduling.Scheduler

object SchedulerFactory {
    private val folia = try {
        Class.forName("io.papermc.paper.threadedregions.RegionizedServer", false, SchedulerFactory::class.java.classLoader)
        true
    } catch (_: ClassNotFoundException) {
        false
    }

    fun createScheduler(plugin: EcoPlugin): Scheduler {
        return if (folia) {
            FoliaScheduler(plugin)
        } else {
            EcoScheduler(plugin)
        }
    }

    fun isFolia(): Boolean = folia
}
