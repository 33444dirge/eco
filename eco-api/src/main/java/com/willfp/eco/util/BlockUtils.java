package com.willfp.eco.util;

import com.willfp.eco.core.blocks.TestableBlock;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;

/**
 * Utilities / API methods for blocks.
 */
public final class BlockUtils {
    /**
     * Get a set of all blocks in contact with each other of a specific type.
     *
     * @param start         The initial block.
     * @param allowedBlocks A list of all valid {@link TestableBlock}s.
     * @param limit         The maximum size of vein to return.
     * @return A set of all {@link Block}s.
     */
    @NotNull
    public static Set<Block> getVein(@NotNull final Block start,
                                     @NotNull final List<TestableBlock> allowedBlocks,
                                     final int limit) {
        Set<Block> blocks = new HashSet<>();
        Queue<Block> toProcess = new LinkedList<>();

        if (allowedBlocks.stream().anyMatch(testableBlock -> testableBlock.matches(start))) {
            toProcess.add(start);
        }

        while (!toProcess.isEmpty() && blocks.size() < limit) {
            Block currentBlock = toProcess.poll();

            if (blocks.contains(currentBlock)) {
                continue;
            }

            blocks.add(currentBlock);

            for (BlockFace face : BlockFace.values()) {
                Block adjacentBlock = currentBlock.getRelative(face);

                if (!blocks.contains(adjacentBlock) &&
                        allowedBlocks.stream().anyMatch(testableBlock -> testableBlock.matches(adjacentBlock))) {
                    toProcess.add(adjacentBlock);
                }
            }
        }

        return blocks;
    }

    /**
     * Get if a block was placed by a player.
     *
     * Uses mcMMO's reward-ineligibility tracker, which also includes blocks made unnatural
     * by pistons and other anti-exploit rules. If tracking is unavailable, treat the block
     * as ineligible instead of granting rewards based on missing data.
     *
     * @param block The block.
     * @return If placed by a player.
     */
    public static boolean isPlayerPlaced(@NotNull final Block block) {
        // Use mcMMO's BlockTracker API
        final var mcMMOPlugin = Bukkit.getPluginManager().getPlugin("mcMMO");
        if (mcMMOPlugin != null && mcMMOPlugin.isEnabled()) {
            try {
                final var tracker = com.gmail.nossr50.mcMMO.getUserBlockTracker();
                if (tracker == null || tracker instanceof com.gmail.nossr50.util.blockmeta.NullChunkManager) {
                    return true;
                }
                return tracker.isIneligible(block);
            } catch (Exception e) {
                // Missing tracking data must not make a potentially unnatural block rewardable.
                return true;
            }
        }

        return true;
    }

    private BlockUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
