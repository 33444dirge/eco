package com.willfp.eco.internal.spigot.proxy.common.packet

import com.willfp.eco.core.packet.PacketEvent
import com.willfp.eco.internal.events.hasReceiveListeners
import com.willfp.eco.internal.events.hasSendListeners
import com.willfp.eco.internal.events.handleReceive
import com.willfp.eco.internal.events.handleSend
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class EcoChannelDuplexHandler(
    private val uuid: UUID
) : ChannelDuplexHandler() {

    private var cachedPlayer: Player? = null

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        // Fast path: check packet type BEFORE any player lookup
        if (!hasReceiveListeners(msg.javaClass)) {
            super.channelRead(ctx, msg)
            return
        }

        val player = getPlayer() ?: run {
            super.channelRead(ctx, msg)
            return
        }

        val event = PacketEvent(msg, player)

        event.handleReceive()

        if (!event.isCancelled) {
            super.channelRead(ctx, event.handle)
        }
    }

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        // Fast path: check packet type BEFORE any player lookup
        if (!hasSendListeners(msg.javaClass)) {
            super.write(ctx, msg, promise)
            return
        }

        val player = getPlayer() ?: run {
            super.write(ctx, msg, promise)
            return
        }

        val event = PacketEvent(msg, player)

        event.handleSend()

        if (!event.isCancelled) {
            super.write(ctx, event.handle, promise)
        }
    }

    private fun getPlayer(): Player? {
        cachedPlayer?.let { player ->
            if (player.isOnline) {
                return player
            }
        }
        return Bukkit.getPlayer(uuid)?.also { cachedPlayer = it }
    }
}