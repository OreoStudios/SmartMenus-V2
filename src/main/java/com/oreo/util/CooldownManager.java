package com.oreo.util;

import com.oreo.SmartMenus;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

public class CooldownManager {

    private final SmartMenus plugin;
    private final CooldownProvider provider;

    public CooldownManager(SmartMenus plugin) {
        this.plugin = plugin;

        String type = plugin.getConfig().getString("cooldown-storage.type", "YAML").toUpperCase(Locale.ENGLISH);

        this.provider = switch (type) {
            case "REDIS" -> {
                String host     = plugin.getConfig().getString("cooldown-storage.redis.host", "localhost");
                int    port     = plugin.getConfig().getInt   ("cooldown-storage.redis.port", 6379);
                String password = plugin.getConfig().getString("cooldown-storage.redis.password", "");
                int    database = plugin.getConfig().getInt   ("cooldown-storage.redis.database", 0);
                plugin.getLogger().info("[Cooldowns] Storage: REDIS (" + host + ":" + port + ")");
                yield new RedisCooldownProvider(host, port, password, database);
            }
            case "PDC" -> {
                plugin.getLogger().info("[Cooldowns] Storage: PDC (single-server, player must be online)");
                yield new PDCCooldownProvider(plugin);
            }
            default -> {
                plugin.getLogger().info("[Cooldowns] Storage: YAML");
                yield new YamlCooldownProvider(plugin);
            }
        };
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public long remainingMillis(String id, UUID uuid) {
        return provider.remainingMillis(id, uuid);
    }

    public void apply(String id, UUID uuid, long durationMillis) {
        provider.apply(id, uuid, durationMillis);
    }

    public void clear(String id, UUID uuid) {
        provider.clear(id, uuid);
    }

    /** Called from SmartMenus#onDisable — flush/close the provider. */
    public void save() {
        provider.shutdown();
    }

    // ── GUI cooldown gate ─────────────────────────────────────────────────────

    public boolean tryUse(Player player, CooldownConfig config) {
        if (config == null || !config.isEnabled()) return true;

        String bypass = config.getBypassPermission();
        if (bypass != null && !bypass.isEmpty() && player.hasPermission(bypass)) return true;

        long remaining = remainingMillis(config.getId(), player.getUniqueId());
        if (remaining > 0L) {
            notifyBlocked(player, config, remaining);
            return false;
        }

        apply(config.getId(), player.getUniqueId(), config.getDurationMillis());
        return true;
    }

    // ── Private helpers ───────────────────────────────────────────────────────
