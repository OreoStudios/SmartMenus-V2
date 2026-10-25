package com.oreo.util;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SmartScheduler {

    private static final boolean FOLIA;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException ignored) {
        }
        FOLIA = folia;
    }

    private SmartScheduler() {}

    public static boolean isFolia() {
        return FOLIA;
    }

    public static void runTask(JavaPlugin plugin, Runnable task) {
        if (FOLIA) {
            Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static void runTaskForPlayer(JavaPlugin plugin, Player player, Runnable task) {
        if (FOLIA) {
            player.getScheduler().run(plugin, t -> task.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static void runTaskLater(JavaPlugin plugin, Runnable task, long delayTicks) {
        if (FOLIA) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), Math.max(1, delayTicks));
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static void runTaskLaterForPlayer(JavaPlugin plugin, Player player, Runnable task, long delayTicks) {
        if (FOLIA) {
            player.getScheduler().runDelayed(plugin, t -> task.run(), null, Math.max(1, delayTicks));
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static Object runTaskTimer(JavaPlugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (FOLIA) {
            return Bukkit.getGlobalRegionScheduler()
                    .runAtFixedRate(plugin, t -> task.run(), Math.max(1, delayTicks), Math.max(1, periodTicks));
        } else {
            return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
    }

    public static void cancelTask(Object handle) {
        if (handle == null) return;
        if (handle instanceof org.bukkit.scheduler.BukkitTask bukkitTask) {
            bukkitTask.cancel();
        } else if (handle instanceof ScheduledTask scheduledTask) {
            scheduledTask.cancel();
        }
    }
}
