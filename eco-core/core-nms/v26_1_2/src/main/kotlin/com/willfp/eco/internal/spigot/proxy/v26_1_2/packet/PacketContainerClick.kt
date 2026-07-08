package com.willfp.eco.internal.spigot.proxy.v26_1_2.packet

import com.willfp.eco.core.packet.PacketEvent
import com.willfp.eco.core.packet.PacketListener
import com.willfp.eco.internal.spigot.proxy.common.packet.display.frame.DisplayFrame
import com.willfp.eco.internal.spigot.proxy.common.packet.display.frame.lastDisplayFrame
import com.willfp.eco.internal.spigot.proxy.v1_21_8.packet.hash
import net.minecraft.network.HashedStack
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object PacketContainerClick : PacketListener {
    // Player UUID -> (Displayed hash -> Original), required to avoid ghost items in cursor
    private val originals = ConcurrentHashMap<UUID, ConcurrentHashMap<Int, HashedStack>>()

    fun map(player: UUID, original: HashedStack, displayed: Int) {
        originals.getOrPut(player) { ConcurrentHashMap() }[displayed] = original
    }

    fun clearPlayer(player: UUID) {
        originals.remove(player)
    }

    override fun onReceive(event: PacketEvent) {
        val packet = event.handle as? ServerboundContainerClickPacket ?: return

        val carried = packet.carriedItem as? HashedStack.ActualItem ?: return
        val player = event.player
        val playerOriginals = originals[player.uniqueId] ?: return

        val original = playerOriginals.remove(carried.hash()) ?: return

        event.handle = ServerboundContainerClickPacket(
            packet.containerId,
            packet.stateId,
            packet.slotNum,
            packet.buttonNum,
            packet.containerInput,
            packet.changedSlots,
            original
        )

        player.lastDisplayFrame = DisplayFrame.EMPTY
    }
}