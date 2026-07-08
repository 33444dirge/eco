package com.willfp.eco.core.packet;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a packet being sent or received.
 */
public class PacketEvent implements Cancellable {
    /**
     * The raw packet handle (NMS object).
     * Use this directly instead of via the Packet wrapper to avoid object allocation.
     */
    private Object handle;

    /**
     * The player.
     */
    private final Player player;

    /**
     * If the event should be cancelled.
     */
    private boolean cancelled = false;

    /**
     * Create a new packet event from a raw NMS handle.
     * This is the preferred constructor - avoids Packet wrapper allocation.
     *
     * @param handle The raw NMS packet handle.
     * @param player The player.
     */
    public PacketEvent(@NotNull final Object handle,
                       @NotNull final Player player) {
        this.handle = handle;
        this.player = player;
    }

    /**
     * Get the raw packet handle.
     *
     * @return The raw packet handle.
     */
    @NotNull
    public Object getHandle() {
        return handle;
    }

    /**
     * Set the raw packet handle.
     *
     * @param handle The raw packet handle.
     */
    public void setHandle(@NotNull final Object handle) {
        this.handle = handle;
    }

    /**
     * Get the packet wrapper.
     *
     * @return The packet wrapper.
     * @deprecated Use {@link #getHandle()} directly to avoid object allocation.
     */
    @Deprecated
    @NotNull
    public Packet getPacket() {
        return new Packet(handle);
    }

    /**
     * Get the player.
     *
     * @return The player.
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }
}