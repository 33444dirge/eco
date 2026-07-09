package com.willfp.eco.internal.scheduling

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.scheduling.Scheduler
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.TimeUnit

class FoliaScheduler(private val plugin: EcoPlugin) : Scheduler {
    override fun isFolia(): Boolean = true

    override fun runLater(
        runnable: Runnable,
        ticksLater: Long
    ): BukkitTask {
        val delay = maxOf(1L, ticksLater)
        val task = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { _ -> runnable.run() }, delay)
        return FoliaTaskAdapter(task, plugin)
    }

    override fun runTimer(
        runnable: Runnable,
        delay: Long,
        repeat: Long
    ): BukkitTask {
        val initialDelay = maxOf(1L, delay)
        val period = maxOf(1L, repeat)
        val task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ -> runnable.run() }, initialDelay, period)
        return FoliaTaskAdapter(task, plugin)
    }

    override fun runAsyncTimer(
        runnable: Runnable,
        delay: Long,
        repeat: Long
    ): BukkitTask {
        val initialDelay = maxOf(1L, delay * 50L)
        val period = maxOf(1L, repeat * 50L)
        val task = Bukkit.getAsyncScheduler().runAtFixedRate(
            plugin,
            { runnable.run() },
            initialDelay,
            period,
            TimeUnit.MILLISECONDS
        )
        return FoliaTaskAdapter(task, plugin, false)
    }

    override fun run(runnable: Runnable): BukkitTask {
        val task = Bukkit.getGlobalRegionScheduler().run(plugin) { _ -> runnable.run() }
        return FoliaTaskAdapter(task, plugin)
    }

    override fun runAsync(runnable: Runnable): BukkitTask {
        val task = Bukkit.getAsyncScheduler().runNow(plugin) { runnable.run() }
        return FoliaTaskAdapter(task, plugin, false)
    }

    override fun syncRepeating(
        runnable: Runnable,
        delay: Long,
        repeat: Long
    ): Int {
        val initialDelay = maxOf(1L, delay)
        val period = maxOf(1L, repeat)
        val task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ -> runnable.run() }, initialDelay, period)
        return System.identityHashCode(task)
    }

    override fun cancelAll() {
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin)
        Bukkit.getAsyncScheduler().cancelTasks(plugin)
    }

    override fun runAtLocation(location: Location, runnable: Runnable): BukkitTask {
        Bukkit.getRegionScheduler().execute(plugin, location) { runnable.run() }
        return FoliaTaskAdapter(null, plugin)
    }

    override fun runAtLocationLater(location: Location, runnable: Runnable, delay: Long): BukkitTask {
        val ticksDelay = maxOf(1L, delay)
        val task = Bukkit.getRegionScheduler().runDelayed(plugin, location, { _ -> runnable.run() }, ticksDelay)
        return FoliaTaskAdapter(task, plugin)
    }

    override fun runAtEntity(entity: Entity, runnable: Runnable): BukkitTask {
        val task = entity.scheduler.run(plugin, { runnable.run() }, null)
        return FoliaTaskAdapter(task, plugin)
    }

    override fun runAtEntityLater(entity: Entity, runnable: Runnable, delay: Long): BukkitTask {
        val ticksDelay = maxOf(1L, delay)
        val task = entity.scheduler.runDelayed(plugin, { runnable.run() }, null, ticksDelay)
        return FoliaTaskAdapter(task, plugin)
    }
}
