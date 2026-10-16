package com.willfp.eco.util

import com.gmail.nossr50.mcMMO
import com.gmail.nossr50.util.blockmeta.NullChunkManager
import com.gmail.nossr50.util.blockmeta.UserBlockTracker
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BlockUtilsTest {
    private val plugins = mockk<PluginManager>()
    private val mcmmo = mockk<Plugin>()
    private val tracker = mockk<UserBlockTracker>()
    private val block = mockk<Block>()

    @BeforeEach
    fun setUp() {
        mockkStatic(Bukkit::class, mcMMO::class)
        every { Bukkit.getPluginManager() } returns plugins
        every { plugins.getPlugin("mcMMO") } returns mcmmo
        every { mcmmo.isEnabled } returns true
        every { mcMMO.getUserBlockTracker() } returns tracker
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `disabled mcMMO must not expose stale tracker data`() {
        every { mcmmo.isEnabled } returns false
        every { tracker.isIneligible(block) } returns false
        assertTrue(BlockUtils.isPlayerPlaced(block))
        verify(exactly = 0) { mcMMO.getUserBlockTracker() }
        verify(exactly = 0) { tracker.isIneligible(any<Block>()) }
    }

    @Test
    fun `missing mcMMO denies rewards without loading its tracker`() {
        every { plugins.getPlugin("mcMMO") } returns null
        assertTrue(BlockUtils.isPlayerPlaced(block))
        verify(exactly = 0) { mcMMO.getUserBlockTracker() }
    }

    @Test
    fun `healthy tracker preserves both classifications`() {
        every { tracker.isIneligible(block) } returns false
        assertFalse(BlockUtils.isPlayerPlaced(block))
        every { tracker.isIneligible(block) } returns true
        assertTrue(BlockUtils.isPlayerPlaced(block))
    }

    @Test
    fun `missing or disabled storage denies rewards`() {
        every { mcMMO.getUserBlockTracker() } returns null
        assertTrue(BlockUtils.isPlayerPlaced(block))
        every { mcMMO.getUserBlockTracker() } returns NullChunkManager()
        assertTrue(BlockUtils.isPlayerPlaced(block))
    }

    @Test
    fun `read failures deny rewards`() {
        every { tracker.isIneligible(block) } throws IllegalStateException("Corrupt placed-block data")
        assertTrue(BlockUtils.isPlayerPlaced(block))
    }
}
