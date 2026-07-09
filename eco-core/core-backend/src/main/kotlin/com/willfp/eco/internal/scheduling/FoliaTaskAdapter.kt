package com.willfp.eco.internal.scheduling

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

class FoliaTaskAdapter(
    private val task: ScheduledTask?,
    private val plugin: Plugin,
    private val sync: Boolean = true
) : BukkitTask {
    override fun getOwner(): Plugin {
        return plugin
    }

    override fun getTaskId(): Int {
        return task?.let { System.identityHashCode(it) } ?: -1
    }

    override fun isSync(): Boolean {
        return sync
    }

    override fun isCancelled(): Boolean {
        return task?.isCancelled ?: true
    }

    override fun cancel() {
        task?.cancel()
    }
}
