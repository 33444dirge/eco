package com.willfp.eco.core.scheduling;

import com.willfp.eco.core.EcoPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

/**
 * Thread scheduler to handle tasks and asynchronous code.
 */
public interface Scheduler {
    /**
     * Run the task after a specified tick delay.
     *
     * @param runnable   The lambda to run.
     * @param ticksLater The amount of ticks to wait before execution.
     * @return The created {@link BukkitTask}.
     */
    BukkitTask runLater(@NotNull Runnable runnable,
                        long ticksLater);

    /**
     * Run the task after a specified tick delay.
     * <p>
     * Reordered for better kotlin interop.
     *
     * @param runnable   The lambda to run.
     * @param ticksLater The amount of ticks to wait before execution.
     * @return The created {@link BukkitTask}.
     */
    default BukkitTask runLater(long ticksLater,
                                @NotNull Runnable runnable) {
        return runLater(runnable, ticksLater);
    }

    /**
     * Run the task repeatedly on a timer.
     *
     * @param runnable The lambda to run.
     * @param delay    The amount of ticks to wait before the first execution.
     * @param repeat   The amount of ticks to wait between executions.
     * @return The created {@link BukkitTask}.
     */
    BukkitTask runTimer(@NotNull Runnable runnable,
                        long delay,
                        long repeat);

    /**
     * Run the task repeatedly on a timer.
     * <p>
     * Reordered for better kotlin interop.
     *
     * @param runnable The lambda to run.
     * @param delay    The amount of ticks to wait before the first execution.
     * @param repeat   The amount of ticks to wait between executions.
     * @return The created {@link BukkitTask}.
     */
    default BukkitTask runTimer(long delay,
                                long repeat,
                                @NotNull Runnable runnable) {
        return runTimer(runnable, delay, repeat);
    }

    /**
     * Run the task repeatedly and asynchronously on a timer.
     *
     * @param runnable The lambda to run.
     * @param delay    The amount of ticks to wait before the first execution.
     * @param repeat   The amount of ticks to wait between executions.
     * @return The created {@link BukkitTask}.
     */
    BukkitTask runAsyncTimer(@NotNull Runnable runnable,
                             long delay,
                             long repeat);

    /**
     * Run the task repeatedly and asynchronously on a timer.
     * <p>
     * Reordered for better kotlin interop.
     *
     * @param runnable The lambda to run.
     * @param delay    The amount of ticks to wait before the first execution.
     * @param repeat   The amount of ticks to wait between executions.
     * @return The created {@link BukkitTask}.
     */
    default BukkitTask runAsyncTimer(long delay,
                                     long repeat,
                                     @NotNull Runnable runnable) {
        return runAsyncTimer(runnable, delay, repeat);
    }

    /**
     * Run the task.
     *
     * @param runnable The lambda to run.
     * @return The created {@link BukkitTask}.
     */
    BukkitTask run(@NotNull Runnable runnable);

    /**
     * Run the task asynchronously.
     *
     * @param runnable The lambda to run.
     * @return The created {@link BukkitTask}.
     */
    BukkitTask runAsync(@NotNull Runnable runnable);

    /**
     * Schedule the task to be ran repeatedly on a timer.
     *
     * @param runnable The lambda to run.
     * @param delay    The amount of ticks to wait before the first execution.
     * @param repeat   The amount of ticks to wait between executions.
     * @return The id of the task.
     */
    int syncRepeating(@NotNull Runnable runnable,
                      long delay,
                      long repeat);

    /**
     * Schedule the task to be ran repeatedly on a timer.
     * <p>
     * Reordered for better kotlin interop.
     *
     * @param runnable The lambda to run.
     * @param delay    The amount of ticks to wait before the first execution.
     * @param repeat   The amount of ticks to wait between executions.
     * @return The id of the task.
     */
    default int syncRepeating(long delay,
                              long repeat,
                              @NotNull Runnable runnable) {
        return syncRepeating(runnable, delay, repeat);
    }

    /**
     * Cancel all running tasks from the linked {@link EcoPlugin}.
     */
    void cancelAll();

    /**
     * Run the task at a specific location (Folia compatible).
     * <p>
     * On non-Folia servers, this will run on the main thread.
     *
     * @param location The location to run the task at.
     * @param runnable The lambda to run.
     * @return The created {@link BukkitTask}.
     */
    default BukkitTask runAtLocation(@NotNull Location location,
                                     @NotNull Runnable runnable) {
        return run(runnable);
    }

    /**
     * Run the task at a specific location after a delay (Folia compatible).
     * <p>
     * On non-Folia servers, this will run on the main thread.
     *
     * @param location The location to run the task at.
     * @param runnable The lambda to run.
     * @param delay    The amount of ticks to wait before execution.
     * @return The created {@link BukkitTask}.
     */
    default BukkitTask runAtLocationLater(@NotNull Location location,
                                          @NotNull Runnable runnable,
                                          long delay) {
        return runLater(runnable, delay);
    }

    /**
     * Run the task at a specific entity (Folia compatible).
     * <p>
     * On non-Folia servers, this will run on the main thread.
     *
     * @param entity   The entity to run the task at.
     * @param runnable The lambda to run.
     * @return The created {@link BukkitTask}.
     */
    default BukkitTask runAtEntity(@NotNull Entity entity,
                                   @NotNull Runnable runnable) {
        return run(runnable);
    }

    /**
     * Run the task at a specific entity after a delay (Folia compatible).
     * <p>
     * On non-Folia servers, this will run on the main thread.
     *
     * @param entity   The entity to run the task at.
     * @param runnable The lambda to run.
     * @param delay    The amount of ticks to wait before execution.
     * @return The created {@link BukkitTask}.
     */
    default BukkitTask runAtEntityLater(@NotNull Entity entity,
                                        @NotNull Runnable runnable,
                                        long delay) {
        return runLater(runnable, delay);
    }

    /**
     * Check if the server is running Folia.
     *
     * @return True if running on Folia, false otherwise.
     */
    default boolean isFolia() {
        return false;
    }
}