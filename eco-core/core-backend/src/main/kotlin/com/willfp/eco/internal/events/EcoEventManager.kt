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

private val packetPriorities = PacketPriority.entries
private val registryLock = Any()

private fun newPriorityMap() = EnumMap<PacketPriority, CopyOnWriteArrayList<RegisteredPacketListener>>(
    PacketPriority::class.java
).apply {
    for (priority in packetPriorities) {
        this[priority] = CopyOnWriteArrayList()
    }
}

private val wildcardSendListeners = newPriorityMap()
private val wildcardReceiveListeners = newPriorityMap()
private val sendListenersByPacket = ConcurrentHashMap<Class<*>, EnumMap<PacketPriority, CopyOnWriteArrayList<RegisteredPacketListener>>>()
private val receiveListenersByPacket = ConcurrentHashMap<Class<*>, EnumMap<PacketPriority, CopyOnWriteArrayList<RegisteredPacketListener>>>()

private val knownSendPacketClasses = mutableSetOf<Class<*>>()
private val knownReceivePacketClasses = mutableSetOf<Class<*>>()

private fun listenersFor(
    listeners: EnumMap<PacketPriority, CopyOnWriteArrayList<RegisteredPacketListener>>,
    priority: PacketPriority
): CopyOnWriteArrayList<RegisteredPacketListener> =
    listeners[priority] ?: error("Missing packet listener list for $priority")

private fun typedListenersFor(
    listeners: ConcurrentHashMap<Class<*>, EnumMap<PacketPriority, CopyOnWriteArrayList<RegisteredPacketListener>>>,
    packetClass: Class<*>
): EnumMap<PacketPriority, CopyOnWriteArrayList<RegisteredPacketListener>> =
    listeners.computeIfAbsent(packetClass) { newPriorityMap() }

/**
 * Sets of packet classes that have at least one listener registered.
 * Uses Class identity (not String), so lookup is O(1) with no allocation.
 */
@Volatile
private var handledSendPacketClasses = emptySet<Class<*>>()

@Volatile
private var handledReceivePacketClasses = emptySet<Class<*>>()

@Volatile
private var hasWildcardSendListeners = false

@Volatile
private var hasWildcardReceiveListeners = false

fun registerHandledSendPacket(packetClass: Class<*>) {
    synchronized(registryLock) {
        knownSendPacketClasses.add(packetClass)
        refreshHandledPacketClasses()
    }
}

fun registerHandledReceivePacket(packetClass: Class<*>) {
    synchronized(registryLock) {
        knownReceivePacketClasses.add(packetClass)
        refreshHandledPacketClasses()
    }
}

fun hasSendListeners(packetClass: Class<*>): Boolean {
    return hasWildcardSendListeners || packetClass in handledSendPacketClasses
}

fun hasReceiveListeners(packetClass: Class<*>): Boolean {
    return hasWildcardReceiveListeners || packetClass in handledReceivePacketClasses
}

fun PacketEvent.handleSend(packetClass: Class<*> = this.handle.javaClass) {
    val typedListeners = sendListenersByPacket[packetClass]

    for (priority in packetPriorities) {
        if (typedListeners != null) {
            for (listener in listenersFor(typedListeners, priority)) {
                listener.handleSend(this)
            }
        }

        for (listener in listenersFor(wildcardSendListeners, priority)) {
            listener.handleSend(this)
        }
    }
}

fun PacketEvent.handleReceive(packetClass: Class<*> = this.handle.javaClass) {
    val typedListeners = receiveListenersByPacket[packetClass]

    for (priority in packetPriorities) {
        if (typedListeners != null) {
            for (listener in listenersFor(typedListeners, priority)) {
                listener.handleReceive(this)
            }
        }

        for (listener in listenersFor(wildcardReceiveListeners, priority)) {
            listener.handleReceive(this)
        }
    }
}

private fun RegisteredPacketListener.handleSend(event: PacketEvent) {
    try {
        listener.onSend(event)
    } catch (e: Exception) {
        logPacketException(event, e, "Exception")
    } catch (e: LinkageError) {
        logPacketException(event, e, "Error")
    }
}

private fun RegisteredPacketListener.handleReceive(event: PacketEvent) {
    try {
        listener.onReceive(event)
    } catch (e: Exception) {
        logPacketException(event, e, "Exception")
    } catch (e: LinkageError) {
        logPacketException(event, e, "Error")
    }
}

private fun RegisteredPacketListener.logPacketException(
    event: PacketEvent,
    error: Throwable,
    label: String
) {
    plugin.logger.warning(
        "$label in packet listener ${listener.javaClass.name}" +
                " for packet ${event.handle.javaClass.name}!"
    )
    error.printStackTrace()
}

private fun PacketListener.overrides(methodName: String): Boolean =
    this.javaClass.getMethod(methodName, PacketEvent::class.java).declaringClass != PacketListener::class.java

private fun registerListener(
    registered: RegisteredPacketListener,
    packetClasses: Collection<Class<*>>,
    wildcardListeners: EnumMap<PacketPriority, CopyOnWriteArrayList<RegisteredPacketListener>>,
    typedListeners: ConcurrentHashMap<Class<*>, EnumMap<PacketPriority, CopyOnWriteArrayList<RegisteredPacketListener>>>
) {
    if (packetClasses.isEmpty()) {
        listenersFor(wildcardListeners, registered.listener.priority).add(registered)
        return
    }

    for (packetClass in packetClasses) {
        listenersFor(typedListenersFor(typedListeners, packetClass), registered.listener.priority).add(registered)
    }
}

private fun unregisterPlugin(
    plugin: EcoPlugin,
    wildcardListeners: EnumMap<PacketPriority, CopyOnWriteArrayList<RegisteredPacketListener>>,
    typedListeners: ConcurrentHashMap<Class<*>, EnumMap<PacketPriority, CopyOnWriteArrayList<RegisteredPacketListener>>>
) {
    for (listeners in wildcardListeners.values) {
        listeners.removeIf { it.plugin == plugin }
    }

    for ((packetClass, listenersByPriority) in typedListeners) {
        for (listeners in listenersByPriority.values) {
            listeners.removeIf { it.plugin == plugin }
        }

        if (!listenersByPriority.hasListeners()) {
            typedListeners.remove(packetClass, listenersByPriority)
        }
    }
}

private fun EnumMap<PacketPriority, CopyOnWriteArrayList<RegisteredPacketListener>>.hasListeners(): Boolean =
    this.values.any { it.isNotEmpty() }

private fun refreshHandledPacketClasses() {
    handledSendPacketClasses = knownSendPacketClasses + sendListenersByPacket
        .filterValues { it.hasListeners() }
        .keys
    handledReceivePacketClasses = knownReceivePacketClasses + receiveListenersByPacket
        .filterValues { it.hasListeners() }
        .keys
    hasWildcardSendListeners = wildcardSendListeners.hasListeners()
    hasWildcardReceiveListeners = wildcardReceiveListeners.hasListeners()
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
        synchronized(registryLock) {
            unregisterPlugin(plugin, wildcardSendListeners, sendListenersByPacket)
            unregisterPlugin(plugin, wildcardReceiveListeners, receiveListenersByPacket)
            refreshHandledPacketClasses()
        }
    }

    override fun registerPacketListener(listener: PacketListener) {
        val registered = RegisteredPacketListener(
            plugin,
            listener
        )

        synchronized(registryLock) {
            val sendPacketClasses = listener.sendPacketClasses
            val receivePacketClasses = listener.receivePacketClasses

            if (sendPacketClasses.isNotEmpty() || listener.overrides("onSend")) {
                registerListener(
                    registered,
                    sendPacketClasses,
                    wildcardSendListeners,
                    sendListenersByPacket
                )
            }

            if (receivePacketClasses.isNotEmpty() || listener.overrides("onReceive")) {
                registerListener(
                    registered,
                    receivePacketClasses,
                    wildcardReceiveListeners,
                    receiveListenersByPacket
                )
            }

            refreshHandledPacketClasses()
        }
    }
}
