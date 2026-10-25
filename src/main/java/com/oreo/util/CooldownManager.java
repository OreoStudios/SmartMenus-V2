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

    private void notifyBlocked(Player player, CooldownConfig config, long remainingMillis) {
        long seconds = (remainingMillis + 999L) / 1000L;
        long minutes = (seconds + 59L) / 60L;
        String formatted = formatTime(seconds);

        String message = applyPlaceholders(config.getMessage(), formatted, seconds, minutes);
        if (message != null && !message.isEmpty()) {
            if (config.isActionBar()) {
                ((Audience) player).sendActionBar(ColorUtil.colorComponent(message));
            } else {
                player.sendMessage(ColorUtil.color(message));
            }
        }

        String title    = applyPlaceholders(config.getTitle(),    formatted, seconds, minutes);
        String subtitle = applyPlaceholders(config.getSubtitle(), formatted, seconds, minutes);
        if ((title != null && !title.isEmpty()) || (subtitle != null && !subtitle.isEmpty())) {
            LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
            Title adventureTitle = Title.title(
                    legacy.deserialize(ColorUtil.color(title    == null ? "" : title)),
                    legacy.deserialize(ColorUtil.color(subtitle == null ? "" : subtitle)),
                    Title.Times.times(
                            Duration.ofMillis(config.getFadeIn()  * 50L),
                            Duration.ofMillis(config.getStay()    * 50L),
                            Duration.ofMillis(config.getFadeOut() * 50L)
                    )
            );
            ((Audience) player).showTitle(adventureTitle);
        }

        String soundName = config.getSound();
        if (soundName != null && !soundName.isEmpty()) {
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase(Locale.ENGLISH));
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown cooldown sound: " + soundName);
            }
        }
    }

    private String applyPlaceholders(String text, String formatted, long seconds, long minutes) {
        if (text == null || text.isEmpty()) return text;
        return text
                .replace("%time_left%", formatted)
                .replace("%time%",      formatted)
                .replace("%seconds%",   String.valueOf(seconds))
                .replace("%minutes%",   String.valueOf(minutes));
    }

    public static String formatTime(long totalSeconds) {
        if (totalSeconds <= 0L) return "0s";
        long days    = totalSeconds / 86_400L;
        long hours   = (totalSeconds % 86_400L) / 3_600L;
        long minutes = (totalSeconds % 3_600L)  / 60L;
        long seconds = totalSeconds % 60L;

        StringBuilder builder = new StringBuilder();
        if (days    > 0) builder.append(days).append("d ");
        if (hours   > 0) builder.append(hours).append("h ");
        if (minutes > 0) builder.append(minutes).append("m ");
        if (seconds > 0) builder.append(seconds).append("s");
        return builder.toString().trim();
    }
}
