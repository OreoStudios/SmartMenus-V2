package com.oreo.util;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * PDC-based cooldown storage.
 * NOTE: single-server only — data is NOT shared across backend servers.
 * Also requires the player to be online; offline players are ignored.
 */
public class PDCCooldownProvider implements CooldownProvider {

    private final Plugin plugin;

    public PDCCooldownProvider(Plugin plugin) {
        this.plugin = plugin;
    }

    private NamespacedKey makeKey(String id) {
        // Replace characters illegal in NamespacedKey
        String safe = id.toLowerCase(java.util.Locale.ENGLISH).replaceAll("[^a-z0-9/_.-]", "_");
        return new NamespacedKey(plugin, "cooldown_" + safe);
    }

    @Override
    public long remainingMillis(String id, UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return 0L;
        Long expiry = player.getPersistentDataContainer().get(makeKey(id), PersistentDataType.LONG);
        if (expiry == null) return 0L;
        long remaining = expiry - System.currentTimeMillis();
        if (remaining <= 0L) {
            player.getPersistentDataContainer().remove(makeKey(id));
            return 0L;
        }
        return remaining;
    }

    @Override
    public void apply(String id, UUID uuid, long durationMillis) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        long expiry = System.currentTimeMillis() + durationMillis;
        player.getPersistentDataContainer().set(makeKey(id), PersistentDataType.LONG, expiry);
    }

    @Override
    public void clear(String id, UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        player.getPersistentDataContainer().remove(makeKey(id));
    }

    @Override
    public void shutdown() {}
}
