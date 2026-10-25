package com.oreo.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class YamlCooldownProvider implements CooldownProvider {

    private final Plugin plugin;
    private final File storageFile;
    private final Map<String, Map<UUID, Long>> store = new ConcurrentHashMap<>();

    public YamlCooldownProvider(Plugin plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "cooldowns.yml");
        load();
    }

    @Override
    public long remainingMillis(String id, UUID uuid) {
        Map<UUID, Long> bucket = store.get(id);
        if (bucket == null) return 0L;
        Long expiry = bucket.get(uuid);
        if (expiry == null) return 0L;
        long remaining = expiry - System.currentTimeMillis();
        if (remaining <= 0L) {
            bucket.remove(uuid);
            return 0L;
        }
        return remaining;
    }

    @Override
    public void apply(String id, UUID uuid, long durationMillis) {
        store.computeIfAbsent(id, k -> new ConcurrentHashMap<>())
                .put(uuid, System.currentTimeMillis() + durationMillis);
    }

    @Override
    public void clear(String id, UUID uuid) {
        Map<UUID, Long> bucket = store.get(id);
        if (bucket != null) bucket.remove(uuid);
    }

    private void load() {
        store.clear();
        if (!storageFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(storageFile);
        long now = System.currentTimeMillis();
        int loaded = 0;

        for (String id : config.getKeys(false)) {
            ConfigurationSection bucketSection = config.getConfigurationSection(id);
            if (bucketSection == null) continue;
            for (String uuidKey : bucketSection.getKeys(false)) {
                long expiry = bucketSection.getLong(uuidKey, 0L);
                if (expiry <= now) continue;
                try {
                    UUID uuid = UUID.fromString(uuidKey);
                    store.computeIfAbsent(id, k -> new ConcurrentHashMap<>()).put(uuid, expiry);
                    loaded++;
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        if (loaded > 0) {
            plugin.getLogger().info("Loaded " + loaded + " active cooldown(s) from YAML.");
        }
    }

    @Override
    public void shutdown() {
        YamlConfiguration config = new YamlConfiguration();
        long now = System.currentTimeMillis();
        int saved = 0;

        for (Map.Entry<String, Map<UUID, Long>> bucket : store.entrySet()) {
            for (Map.Entry<UUID, Long> entry : bucket.getValue().entrySet()) {
                if (entry.getValue() <= now) continue;
                config.set(bucket.getKey() + "." + entry.getKey(), entry.getValue());
                saved++;
            }
        }

        try {
            if (saved == 0 && storageFile.exists()) {
                storageFile.delete();
                return;
            }
            if (saved == 0) return;
            File parent = storageFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            config.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save cooldowns.yml: " + e.getMessage());
        }
    }
}
