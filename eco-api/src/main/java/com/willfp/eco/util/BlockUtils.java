package com.willfp.eco.util;

import com.willfp.eco.core.blocks.TestableBlock;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.persistence.PersistentDataType;
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
     * This method relies on mcMMO's BlockTracker API. If mcMMO is not installed,
     * this will always return false (conservative approach: assume natural blocks).
     *
     * @param block The block.
     * @return If placed by a player.
     */
    public static boolean isPlayerPlaced(@NotNull final Block block) {
        // Use mcMMO's BlockTracker API
        if (Bukkit.getPluginManager().getPlugin("mcMMO") != null) {
            try {
                return com.gmail.nossr50.mcMMO.getUserBlockTracker().isIneligible(block);
            } catch (Exception e) {
                // mcMMO API call failed, fallback to false
                return false;
            }
        }

        // mcMMO not installed, cannot determine (conservative: assume natural)
        return false;
    }

    private BlockUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}