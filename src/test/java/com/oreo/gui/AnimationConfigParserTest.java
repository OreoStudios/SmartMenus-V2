package com.oreo.gui;

import com.oreo.SmartMenus;
import com.oreo.animation.AnimationPlaybackMode;
import com.oreo.animation.GuiAnimation;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationConfigParserTest {

    private AnimationConfigParser parser;

    @BeforeEach
    void setUp() {
        SmartMenus plugin = Mockito.mock(SmartMenus.class);
        Mockito.lenient().when(plugin.getLogger()).thenReturn(Logger.getLogger("AnimationConfigParserTest"));
        com.oreo.condition.ConditionFactory cf = new com.oreo.condition.ConditionFactory(plugin);
        ConditionListParser clp = new ConditionListParser(plugin, cf);
        GuiItemParser itemParser = new GuiItemParser(plugin, clp);
        parser = new AnimationConfigParser(plugin, itemParser);
    }

    /** Builds an animations config section from nested maps, mirroring parsed YAML. */
    private ConfigurationSection animationsSection(Map<String, Object> raw) {
        return new MemoryConfiguration().createSection("animations", raw);
    }

    private static Map<String, Object> item(String material) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("material", material);
        m.put("name", "&8");
        return m;
    }

    private static Map<String, Object> frameWithSlots(Map<String, Object> slots) {
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("slots", slots);
        return frame;
    }

    @Test
    void parsesBasicLoopAnimation() {
        Map<String, Object> slots1 = new LinkedHashMap<>();
        slots1.put("0-8", item("RED_STAINED_GLASS_PANE"));
        Map<String, Object> slots2 = new LinkedHashMap<>();
        slots2.put("0-8", item("BLUE_STAINED_GLASS_PANE"));

        Map<String, Object> border = new LinkedHashMap<>();
        border.put("interval", 2);
        border.put("playback", "LOOP");
        border.put("frames", Arrays.asList(frameWithSlots(slots1), frameWithSlots(slots2)));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("border", border);

        Map<String, GuiAnimation> result = parser.parse("test", animationsSection(root));
        assertEquals(1, result.size());
        GuiAnimation anim = result.get("border");
        assertNotNull(anim);
        assertEquals(2, anim.getInterval());
        assertEquals(AnimationPlaybackMode.LOOP, anim.getPlayback());
        assertEquals(2, anim.getFrameCount());
        // both frames cover slots 0..8
        assertTrue(anim.getOwnedSlots().containsAll(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8)));
    }

    @Test
    void parsesPlaybackModeLeniently() {
        Map<String, Object> slots = new LinkedHashMap<>();
        slots.put("0", item("STONE"));
        Map<String, Object> anim = new LinkedHashMap<>();
        anim.put("mode", "ping-pong");
        anim.put("frames", List.of(frameWithSlots(slots), frameWithSlots(slots)));
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("a", anim);

        GuiAnimation parsed = parser.parse("test", animationsSection(root)).get("a");
        assertEquals(AnimationPlaybackMode.PING_PONG, parsed.getPlayback());
    }

    @Test
    void nullSectionYieldsEmptyMap() {
        assertTrue(parser.parse("test", null).isEmpty());
    }

    @Test
    void animationWithoutFramesIsSkipped() {
        Map<String, Object> anim = new LinkedHashMap<>();
        anim.put("interval", 5);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("broken", anim);
        assertTrue(parser.parse("test", animationsSection(root)).isEmpty());
    }

    @Test
    void frameWithoutSlotsIsSkipped() {
        Map<String, Object> emptyFrame = new LinkedHashMap<>(); // no "slots"
        Map<String, Object> anim = new LinkedHashMap<>();
        anim.put("frames", List.of(emptyFrame));
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("broken", anim);
        // no valid frames -> animation skipped entirely
        assertNull(parser.parse("test", animationsSection(root)).get("broken"));
    }

    @Test
    void autoplayDefaultsTrueAndLoopsDefaultInfinite() {
        Map<String, Object> slots = new LinkedHashMap<>();
        slots.put("0", item("STONE"));
        Map<String, Object> anim = new LinkedHashMap<>();
        anim.put("frames", List.of(frameWithSlots(slots), frameWithSlots(slots)));
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("a", anim);

        GuiAnimation parsed = parser.parse("test", animationsSection(root)).get("a");
        assertTrue(parsed.isAutoplay());
        assertEquals(GuiAnimation.INFINITE_LOOPS, parsed.getLoops());
    }
}
