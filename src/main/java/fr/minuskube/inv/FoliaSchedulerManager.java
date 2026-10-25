package fr.minuskube.inv;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class FoliaSchedulerManager implements SchedulerManager {

    private final JavaPlugin plugin;

    private final Map<Player, ScheduledTask> tasks = new HashMap<>();

    public FoliaSchedulerManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runTask(BukkitRunnable runnable, Player player,
                        long delay, long period, SchedulerManager.SchedulerType type) {

        Consumer<ScheduledTask> task = scheduled -> runnable.run();

        ScheduledTask handle;
        if (delay == 0 && period == 0) {
            handle = player.getScheduler().run(plugin, task, null);
        } else if (period == 0) {
            handle = player.getScheduler().runDelayed(plugin, task, null, Math.max(1, delay));
        } else {
            handle = player.getScheduler()
                    .runAtFixedRate(plugin, task, null, Math.max(1, delay), Math.max(1, period));
        }

        if (handle != null) {
            tasks.put(player, handle);
        }
    }

    @Override
    public void cancelTaskByPlayer(Player player) {
        ScheduledTask handle = tasks.remove(player);
        if (handle != null) {
            handle.cancel();
        }
    }
}
