package com.willfp.eco.internal.scheduling

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.scheduling.RunnableTask
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

open class FoliaRunnableTask(
    protected val plugin: EcoPlugin,
    private val taskConsumer: Consumer<FoliaRunnableTask>
) : RunnableTask {
    private var task: ScheduledTask? = null

    override fun run() {
        taskConsumer.accept(this)
    }

    @Synchronized
    override fun runTask(): BukkitTask {
        val scheduledTask = org.bukkit.Bukkit.getGlobalRegionScheduler().run(plugin) { _ -> run() }
        this.task = scheduledTask
        return FoliaTaskAdapter(scheduledTask, plugin)
    }

    @Synchronized
    override fun runTaskAsynchronously(): BukkitTask {
        val scheduledTask = org.bukkit.Bukkit.getAsyncScheduler().runNow(plugin) { run() }
        this.task = scheduledTask
        return FoliaTaskAdapter(scheduledTask, plugin)
    }

    @Synchronized
    override fun runTaskLater(delay: Long): BukkitTask {
        val ticksDelay = maxOf(1L, delay)
        val scheduledTask = org.bukkit.Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { _ -> run() }, ticksDelay)
        this.task = scheduledTask
        return FoliaTaskAdapter(scheduledTask, plugin)
    }

    @Synchronized
    override fun runTaskLaterAsynchronously(delay: Long): BukkitTask {
        val msDelay = maxOf(1L, delay * 50L)
        val scheduledTask = org.bukkit.Bukkit.getAsyncScheduler().runDelayed(
            plugin,
            { run() },
            msDelay,
            TimeUnit.MILLISECONDS
        )
        this.task = scheduledTask
        return FoliaTaskAdapter(scheduledTask, plugin)
    }

    @Synchronized
    override fun runTaskTimer(delay: Long, period: Long): BukkitTask {
        val initialDelay = maxOf(1L, delay)
        val ticksPeriod = maxOf(1L, period)
        val scheduledTask = org.bukkit.Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ -> run() }, initialDelay, ticksPeriod)
        this.task = scheduledTask
        return FoliaTaskAdapter(scheduledTask, plugin)
    }

    @Synchronized
    override fun runTaskTimerAsynchronously(delay: Long, period: Long): BukkitTask {
        val initialDelay = maxOf(1L, delay * 50L)
        val msPeriod = maxOf(1L, period * 50L)
        val scheduledTask = org.bukkit.Bukkit.getAsyncScheduler().runAtFixedRate(
            plugin,
            { run() },
            initialDelay,
            msPeriod,
            TimeUnit.MILLISECONDS
        )
        this.task = scheduledTask
        return FoliaTaskAdapter(scheduledTask, plugin)
    }

    override fun cancel() {
        task?.cancel()
    }

    fun runTaskAtLocation(location: Location): BukkitTask {
        org.bukkit.Bukkit.getRegionScheduler().execute(plugin, location) { run() }
        return FoliaTaskAdapter(null, plugin)
    }

    fun runTaskAtLocationLater(location: Location, delay: Long): BukkitTask {
        val ticksDelay = maxOf(1L, delay)
        val scheduledTask = org.bukkit.Bukkit.getRegionScheduler().runDelayed(plugin, location, { _ -> run() }, ticksDelay)
        this.task = scheduledTask
        return FoliaTaskAdapter(scheduledTask, plugin)
    }

    fun runTaskAtEntity(entity: Entity): BukkitTask {
        val scheduledTask = entity.scheduler.run(plugin, { run() }, null)
        this.task = scheduledTask
        return FoliaTaskAdapter(scheduledTask, plugin)
    }

    fun runTaskAtEntityLater(entity: Entity, delay: Long): BukkitTask {
        val ticksDelay = maxOf(1L, delay)
        val scheduledTask = entity.scheduler.runDelayed(plugin, { run() }, null, ticksDelay)
        this.task = scheduledTask
        return FoliaTaskAdapter(scheduledTask, plugin)
    }
}