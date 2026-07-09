package com.willfp.eco.internal.spigot.proxy.common.packet

import com.willfp.eco.core.packet.PacketEvent
import com.willfp.eco.internal.events.hasReceiveListeners
import com.willfp.eco.internal.events.hasSendListeners
import com.willfp.eco.internal.events.handleReceive
import com.willfp.eco.internal.events.handleSend
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import org.bukkit.entity.Player

class EcoChannelDuplexHandler(
    private val player: Player
) : ChannelDuplexHandler() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        // Fast path: check packet type BEFORE any player lookup
        val packetClass = msg.javaClass
        if (!hasReceiveListeners(packetClass)) {
            super.channelRead(ctx, msg)
            return
        }

        if (!player.isOnline) {
            super.channelRead(ctx, msg)
            return
        }

        val event = PacketEvent(msg, player)

        event.handleReceive(packetClass)

        if (!event.isCancelled) {
            super.channelRead(ctx, event.handle)
        }
    }

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        // Fast path: check packet type BEFORE any player lookup
        val packetClass = msg.javaClass
        if (!hasSendListeners(packetClass)) {
            super.write(ctx, msg, promise)
            return
        }

        if (!player.isOnline) {
            super.write(ctx, msg, promise)
            return
        }

        val event = PacketEvent(msg, player)

        event.handleSend(packetClass)

        if (!event.isCancelled) {
            super.write(ctx, event.handle, promise)
        }
    }
}
