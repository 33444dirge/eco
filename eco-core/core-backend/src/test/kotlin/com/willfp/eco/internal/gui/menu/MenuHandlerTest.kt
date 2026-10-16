package com.willfp.eco.internal.gui.menu

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MenuHandlerTest {
    @Test
    fun `different region threads retain every registered menu`() {
        val player = mockk<Player>()
        val menu = mockk<EcoMenu>()
        val workers = 8
        val inventories = List(workers) { List(512) { inventory() } }
        val barrier = CyclicBarrier(workers)
        val executor = Executors.newFixedThreadPool(workers)

        try {
            val tasks = inventories.map { owned ->
                Callable {
                    barrier.await(10, TimeUnit.SECONDS)
                    val rendered = owned.map { MenuHandler.registerInventory(it, menu, player) }
                    barrier.await(10, TimeUnit.SECONDS)
                    owned.zip(rendered).forEach { (inventory, expected) ->
                        assertSame(expected, inventory.asRenderedInventory())
                    }
                    owned.forEach {
                        MenuHandler.unregisterInventory(it)
                        assertNull(it.asRenderedInventory())
                    }
                }
            }
            executor.invokeAll(tasks, 30, TimeUnit.SECONDS).forEach { it.get() }
        } finally {
            executor.shutdownNow()
            inventories.flatten().forEach(MenuHandler::unregisterInventory)
        }
    }

    @Test
    fun `queued render does not refresh an older inventory of the same menu`() {
        val player = mockk<Player>()
        val menu = mockk<EcoMenu>()
        val oldInventory = mockk<Inventory>()
        val newInventory = inventory()
        val view = mockk<InventoryView>()
        every { player.uniqueId } returns UUID.randomUUID()
        every { player.openInventory } returns view
        every { view.topInventory } returns newInventory
        val stale = MenuHandler.registerInventory(oldInventory, menu, player)
        val active = MenuHandler.registerInventory(newInventory, menu, player)

        try {
            stale.render()
            assertNull(oldInventory.asRenderedInventory())
            assertSame(active, newInventory.asRenderedInventory())
            verify(exactly = 0) { menu.rows }
            verify(exactly = 0) { oldInventory.setItem(any(), any()) }
        } finally {
            MenuHandler.unregisterInventory(oldInventory)
            MenuHandler.unregisterInventory(newInventory)
        }
    }

    private fun inventory(): Inventory = Proxy.newProxyInstance(
        Inventory::class.java.classLoader,
        arrayOf(Inventory::class.java)
    ) { proxy, method, arguments ->
        when (method.name) {
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === arguments?.get(0)
            "toString" -> "TestInventory@${System.identityHashCode(proxy)}"
            else -> error("Unexpected inventory call: ${method.name}")
        }
    } as Inventory
}
