package com.oreo.gui;

import com.oreo.SmartMenus;
import com.oreo.animation.AnimationFrame;
import com.oreo.animation.AnimationPlaybackMode;
import com.oreo.animation.GuiAnimation;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a GUI's {@code animations:} configuration section into reusable {@link GuiAnimation}
 * definitions.
 *
 * <p>Animated frame items reuse the standard SmartMenus item format via {@link GuiItemParser}, so
 * every animated slot supports the same materials, names, lore, placeholders, custom items, glow and
 * even click actions as a normal menu item — there is no second item format and no duplicated
 * placeholder handling.</p>
 *
 * <pre>{@code
 * animations:
 *   border:
 *     interval: 2
 *     start-delay: 0
 *     playback: LOOP        # LOOP | ONCE | REVERSE | PING_PONG
 *     autoplay: true
 *     loops: 0              # 0 = infinite
 *     frames:
 *       - slots:
 *           0-8: { material: RED_STAINED_GLASS_PANE, name: "&8" }
 *       - slots:
 *           0-8: { material: BLUE_STAINED_GLASS_PANE, name: "&8" }
 * }</pre>
 */
public final class AnimationConfigParser {

    private final SmartMenus plugin;
    private final GuiItemParser itemParser;

    public AnimationConfigParser(SmartMenus plugin, GuiItemParser itemParser) {
        this.plugin = plugin;
        this.itemParser = itemParser;
    }

    /**
     * Parses every animation defined under {@code animationsSection}.
     *
     * @return an ordered map of animation name → definition; empty if the section is null or holds
     *         no valid animations
     */
    public Map<String, GuiAnimation> parse(String guiId, ConfigurationSection animationsSection) {
        Map<String, GuiAnimation> result = new LinkedHashMap<>();
        if (animationsSection == null) return result;

        for (String name : animationsSection.getKeys(false)) {
            ConfigurationSection section = animationsSection.getConfigurationSection(name);
            if (section == null) {
                plugin.getLogger().warning("Animation '" + name + "' in GUI '" + guiId + "' is not a section — skipped.");
                continue;
            }
            try {
                GuiAnimation animation = parseAnimation(guiId, name, section);
                if (animation != null) {
                    result.put(name, animation);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse animation '" + name + "' in GUI '" + guiId + "': " + e.getMessage());
            }
        }
        return result;
    }

    private GuiAnimation parseAnimation(String guiId, String name, ConfigurationSection section) {
        int interval = section.getInt("interval", 2);
        int startDelay = section.getInt("start-delay", section.getInt("start_delay", 0));
        AnimationPlaybackMode playback = AnimationPlaybackMode.fromString(
                section.getString("playback", section.getString("mode", "LOOP")));
        boolean autoplay = section.getBoolean("autoplay", true);
        int loops = section.getInt("loops", GuiAnimation.INFINITE_LOOPS);

        List<?> rawFrames = section.getList("frames");
        if (rawFrames == null || rawFrames.isEmpty()) {
            plugin.getLogger().warning("Animation '" + name + "' in GUI '" + guiId + "' has no frames — skipped.");
            return null;
        }

        GuiAnimation.Builder builder = GuiAnimation.builder()
                .interval(interval)
                .startDelay(startDelay)
                .playback(playback)
                .autoplay(autoplay)
                .loops(loops);

        int frameCount = 0;
        for (Object rawFrame : rawFrames) {
            AnimationFrame frame = parseFrame(guiId, name, rawFrame);
            if (frame != null) {
                builder.frame(frame);
                frameCount++;
            }
        }

        if (frameCount == 0) {
            plugin.getLogger().warning("Animation '" + name + "' in GUI '" + guiId + "' has no valid frames — skipped.");
            return null;
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private AnimationFrame parseFrame(String guiId, String animName, Object rawFrame) {
        if (!(rawFrame instanceof Map)) return null;
        ConfigurationSection frameSection = toSection((Map<String, Object>) rawFrame);

        int duration = frameSection.getInt("duration", AnimationFrame.USE_ANIMATION_INTERVAL);

        ConfigurationSection slotsSection = frameSection.getConfigurationSection("slots");
        if (slotsSection == null) {
            plugin.getLogger().warning("A frame of animation '" + animName + "' in GUI '" + guiId + "' has no 'slots' — skipped.");
            return null;
        }

        AnimationFrame.Builder frameBuilder = AnimationFrame.builder().duration(duration);
        boolean any = false;

        for (String slotKey : slotsSection.getKeys(false)) {
            ConfigurationSection itemSection = slotsSection.getConfigurationSection(slotKey);
            if (itemSection == null) continue;

            GuiItem item = itemParser.loadElseItem(guiId, itemSection);
            if (item == null) continue;

            for (int slot : SlotParser.parse(slotKey)) {
                if (slot < 0) continue;
                frameBuilder.set(slot, item);
                any = true;
            }
        }

        return any ? frameBuilder.build() : null;
    }

    /** Wraps a raw YAML map into a {@link ConfigurationSection} so the standard parsers can read it. */
    private static ConfigurationSection toSection(Map<String, Object> map) {
        // createSection(path, map) recursively converts nested maps into real sections.
        return new MemoryConfiguration().createSection("__root", map);
    }
}
