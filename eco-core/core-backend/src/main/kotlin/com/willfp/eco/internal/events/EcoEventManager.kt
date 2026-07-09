package com.willfp.eco.internal.events

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.events.EventManager
import com.willfp.eco.core.packet.PacketEvent
import com.willfp.eco.core.packet.PacketListener
import com.willfp.eco.core.packet.PacketPriority
import java.util.EnumMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener


private class RegisteredPacketListener(
    val plugin: EcoPlugin,
    val listener: PacketListener
)

private val listeners = EnumMap<PacketPriority, CopyOnWriteArrayList<RegisteredPacketListener>>(
    PacketPriority::class.java
).apply {
    for (priority in PacketPriority.entries) {
        this[priority] = CopyOnWriteArrayList()
    }
}

private fun listenersFor(priority: PacketPriority): CopyOnWriteArrayList<RegisteredPacketListener> =
    listeners[priority] ?: error("Missing packet listener list for $priority")

/**
 * Sets of packet classes that have at least one listener registered.
 * Uses Class identity (not String), so lookup is O(1) with no allocation.
 * If empty, all packets are processed (backward compatibility).
 */
private val handledSendPacketClasses = ConcurrentHashMap.newKeySet<Class<*>>()
private val handledReceivePacketClasses = ConcurrentHashMap.newKeySet<Class<*>>()

fun registerHandledSendPacket(packetClass: Class<*>) {
    handledSendPacketClasses.add(packetClass)
}

fun registerHandledReceivePacket(packetClass: Class<*>) {
    handledReceivePacketClasses.add(packetClass)
}

fun hasSendListeners(packetClass: Class<*>): Boolean {
    val set = handledSendPacketClasses
    return set.isEmpty() || packetClass in set
}

fun hasReceiveListeners(packetClass: Class<*>): Boolean {
    val set = handledReceivePacketClasses
    return set.isEmpty() || packetClass in set
}

fun PacketEvent.handleSend() {
    for (priority in PacketPriority.entries) {
        for (listener in listenersFor(priority)) {
            try {
                listener.listener.onSend(this)
            } catch (e: Exception) {
                listener.plugin.logger.warning(
                    "Exception in packet listener ${listener.listener.javaClass.name}" +
                            " for packet ${this.handle.javaClass.name}!"
                )
                e.printStackTrace()
            } catch (e: LinkageError) {
                listener.plugin.logger.warning(
                    "Error in packet listener ${listener.listener.javaClass.name}" +
                            " for packet ${this.handle.javaClass.name}!"
                )
                e.printStackTrace()
            }
        }
    }
}

fun PacketEvent.handleReceive() {
    for (priority in PacketPriority.entries) {
        for (listener in listenersFor(priority)) {
            try {
                listener.listener.onReceive(this)
            } catch (e: Exception) {
                listener.plugin.logger.warning(
                    "Exception in packet listener ${listener.listener.javaClass.name}" +
                            " for packet ${this.handle.javaClass.name}!"
                )
                e.printStackTrace()
            } catch (e: LinkageError) {
                listener.plugin.logger.warning(
                    "Error in packet listener ${listener.listener.javaClass.name}" +
                            " for packet ${this.handle.javaClass.name}!"
                )
                e.printStackTrace()
            }
        }
    }
}

class EcoEventManager(private val plugin: EcoPlugin) : EventManager {
    override fun registerListener(listener: Listener) {
        Bukkit.getPluginManager().registerEvents(listener, plugin)
    }

    override fun unregisterListener(listener: Listener) {
        HandlerList.unregisterAll(listener)
    }

    override fun unregisterAllListeners() {
        HandlerList.unregisterAll(plugin)
        for (value in listeners.values) {
            value.removeIf { it.plugin == plugin }
        }
    }

    override fun registerPacketListener(listener: PacketListener) {
        listenersFor(listener.priority).add(
            RegisteredPacketListener(
                plugin,
                listener
            )
        )
    }
}
