package com.willfp.eco.internal.spigot.proxy.common.packet

import com.willfp.eco.internal.spigot.proxy.common.toNMS
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

private const val BASE_NAME = "packet_handler"
private const val ECO_NAME = "eco_packets"

object PacketInjectorListener : Listener {
    private val quitCallbacks = CopyOnWriteArrayList<(UUID) -> Unit>()

    fun onPlayerQuit(callback: (UUID) -> Unit) {
        quitCallbacks.add(callback)
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player

        val channel = player.toNMS().connection.connection.channel

        if (BASE_NAME !in channel.pipeline().names()) {
            return
        }

        if (ECO_NAME in channel.pipeline().names()) {
            return
        }

        channel.pipeline().addBefore(BASE_NAME, ECO_NAME, EcoChannelDuplexHandler(player.uniqueId))
    }

    @EventHandler
    fun onLeave(event: PlayerQuitEvent) {
        val player = event.player

        val channel = player.toNMS().connection.connection.channel

        channel.eventLoop().submit {
            if (channel.pipeline().get(ECO_NAME) != null) {
                channel.pipeline().remove(ECO_NAME)
            }
        }

        val uuid = player.uniqueId
        quitCallbacks.forEach { it(uuid) }
    }
}