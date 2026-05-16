package com.willfp.eco.internal.factory

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.factory.RunnableFactory
import com.willfp.eco.core.scheduling.RunnableTask
import com.willfp.eco.internal.scheduling.EcoRunnableTask
import com.willfp.eco.internal.scheduling.FoliaRunnableTask
import com.willfp.eco.internal.scheduling.SchedulerFactory
import java.util.function.Consumer

class EcoRunnableFactory(private val plugin: EcoPlugin) : RunnableFactory {
    override fun create(consumer: Consumer<RunnableTask>): RunnableTask {
        return if (SchedulerFactory.isFolia()) {
            FoliaRunnableTask(plugin) { task -> consumer.accept(task) }
        } else {
            object : EcoRunnableTask(plugin) {
                override fun run() {
                    consumer.accept(this)
                }
            }
        }
    }
}