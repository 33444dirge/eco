package com.willfp.eco.core.packet;

import java.util.Collection;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;

/**
 * Listens to packets.
 */
public interface PacketListener {
    /**
     * Called when a handle is sent.
     *
     * @param event The event.
     */
    default void onSend(@NotNull final PacketEvent event) {
        // Override when needed.
    }

    /**
     * Called when a handle is received.
     *
     * @param event The event.
     */
    default void onReceive(@NotNull final PacketEvent event) {
        // Override when needed.
    }

    /**
     * Get the packet classes this listener handles when sent to the player.
     *
     * @return The handled send packet classes.
     */
    @NotNull
    default Collection<Class<?>> getSendPacketClasses() {
        return Collections.emptyList();
    }

    /**
     * Get the packet classes this listener handles when received from the player.
     *
     * @return The handled receive packet classes.
     */
    @NotNull
    default Collection<Class<?>> getReceivePacketClasses() {
        return Collections.emptyList();
    }

    /**
     * Get the priority of the listener.
     *
     * @return The priority.
     */
    default PacketPriority getPriority() {
        return PacketPriority.NORMAL;
    }
}
