package com.willfp.eco.internal.spigot.proxy.v1_21_8

import com.willfp.eco.core.fast.FastItemStack
import com.willfp.eco.internal.spigot.proxies.FastItemStackFactoryProxy
import com.willfp.eco.internal.spigot.proxy.common.item.EcoFastItemStack
import com.willfp.eco.internal.spigot.proxy.common.asNMSStack
import net.minecraft.core.component.DataComponents
import org.bukkit.inventory.ItemStack

class FastItemStackFactory : FastItemStackFactoryProxy {
    override fun hash(itemStack: ItemStack): Int {
        val handle = itemStack.asNMSStack()
        // Preserve the wrapper's malformed PDC check without copying the tag.
        handle.get(DataComponents.CUSTOM_DATA)?.unsafe?.let {
            if (it.contains("PublicBukkitValues")) it.getCompound("PublicBukkitValues").get()
        }
        return net.minecraft.world.item.ItemStack.hashItemAndComponents(handle)
    }

    override fun create(itemStack: ItemStack): FastItemStack {
        return EcoFastItemStack(itemStack)
    }
}
