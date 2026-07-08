package com.willfp.eco.internal.spigot.proxy.v1_21_8

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.packet.PacketListener
import com.willfp.eco.internal.events.registerHandledReceivePacket
import com.willfp.eco.internal.events.registerHandledSendPacket
import com.willfp.eco.internal.spigot.proxies.PacketHandlerProxy
import com.willfp.eco.internal.spigot.proxy.common.packet.PacketInjectorListener
import com.willfp.eco.internal.spigot.proxy.common.packet.display.PacketHeldItemSlot
import com.willfp.eco.internal.spigot.proxy.common.packet.display.PacketSetSlot
import com.willfp.eco.internal.spigot.proxy.common.packet.display.frame.clearFrames
import com.willfp.eco.internal.spigot.proxy.v1_21_8.packet.NewItemsPacketOpenWindowMerchant
import com.willfp.eco.internal.spigot.proxy.v1_21_8.packet.NewItemsPacketSetCreativeSlot
import com.willfp.eco.internal.spigot.proxy.v1_21_8.packet.NewItemsPacketWindowItems
import com.willfp.eco.internal.spigot.proxy.v1_21_8.packet.PacketContainerClick
import com.willfp.eco.internal.spigot.proxy.v1_21_8.packet.PacketSetCursorItem
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player

class PacketHandler : PacketHandlerProxy {
    override fun sendPacket(player: Player, packet: com.willfp.eco.core.packet.Packet) {
        if (player !is CraftPlayer) {
            return
        }

        val handle = packet.handle

        if (handle !is Packet<*>) {
            return
        }

        player.handle.connection.send(handle)
    }

    override fun clearDisplayFrames() {
        clearFrames()
    }

    override fun getPacketListeners(plugin: EcoPlugin): List<PacketListener> {
        registerKnownPacketTypes()

        // Clean up originals map when player quits
        PacketInjectorListener.onPlayerQuit { uuid ->
            PacketContainerClick.clearPlayer(uuid)
        }

        return listOf(
            PacketHeldItemSlot,
            NewItemsPacketOpenWindowMerchant,
            NewItemsPacketSetCreativeSlot,
            PacketSetSlot,
            NewItemsPacketWindowItems(plugin),
            PacketContainerClick,
            PacketSetCursorItem
        )
    }

    private fun registerKnownPacketTypes() {
        // Send packets (server -> client) - Class identity check, zero allocation
        registerHandledSendPacket(ClientboundContainerSetSlotPacket::class.java)
        registerHandledSendPacket(ClientboundMerchantOffersPacket::class.java)
        registerHandledSendPacket(ClientboundContainerSetContentPacket::class.java)
        registerHandledSendPacket(ClientboundSetCursorItemPacket::class.java)

        // Receive packets (client -> server) - Class identity check, zero allocation
        registerHandledReceivePacket(ServerboundSetCarriedItemPacket::class.java)
        registerHandledReceivePacket(ServerboundSetCreativeModeSlotPacket::class.java)
        registerHandledReceivePacket(ServerboundContainerClickPacket::class.java)
    }
}