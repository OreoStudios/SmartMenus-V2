package com.oreo.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownConfigTest {

    private ConfigurationSection section() {
        return new MemoryConfiguration().createSection("cooldown");
    }

    @Test
    void parseDurationPlainNumberIsSeconds() {
        assertEquals(30_000L, CooldownConfig.parseDuration("30"));
    }

    @Test
    void parseDurationUnits() {
        assertEquals(90_000L, CooldownConfig.parseDuration("90s"));
        assertEquals(1_800_000L, CooldownConfig.parseDuration("30m"));
        assertEquals(3_600_000L, CooldownConfig.parseDuration("1h"));
        assertEquals(86_400_000L, CooldownConfig.parseDuration("1d"));
    }

    @Test
    void parseDurationCombinedAndSpacingAndCase() {
        assertEquals(5_400_000L, CooldownConfig.parseDuration("1h30m"));
        assertEquals(5_410_000L, CooldownConfig.parseDuration("1h30m10s"));
        assertEquals(3_600_000L, CooldownConfig.parseDuration("1H"));
    }

    @Test
    void parseDurationInvalidOrEmptyIsZero() {
        assertEquals(0L, CooldownConfig.parseDuration(null));
        assertEquals(0L, CooldownConfig.parseDuration(""));
        assertEquals(0L, CooldownConfig.parseDuration("   "));
        assertEquals(0L, CooldownConfig.parseDuration("abc"));
    }

    @Test
    void parseReturnsNullWhenNoDurationGiven() {
        assertNull(CooldownConfig.parse(section(), "gui:test"));
        assertNull(CooldownConfig.parse(null, "gui:test"));
    }

    @Test
    void parseReturnsNullWhenExplicitlyDisabled() {
        ConfigurationSection s = section();
        s.set("minutes", 30);
        s.set("enabled", false);
        assertNull(CooldownConfig.parse(s, "gui:test"));
    }

    @Test
    void parseMinutesToMillis() {
        ConfigurationSection s = section();
        s.set("minutes", 30);
        CooldownConfig cfg = CooldownConfig.parse(s, "gui:test");
        assertTrue(cfg.isEnabled());
        assertEquals(1_800_000L, cfg.getDurationMillis());
    }

    @Test
    void parseSumsAllTimeUnits() {
        ConfigurationSection s = section();
        s.set("hours", 1);
        s.set("minutes", 30);
        s.set("seconds", 15);
        CooldownConfig cfg = CooldownConfig.parse(s, "gui:test");
        assertEquals(3_600_000L + 1_800_000L + 15_000L, cfg.getDurationMillis());
    }

    @Test
    void parseDurationStringField() {
        ConfigurationSection s = section();
        s.set("duration", "1h30m");
        CooldownConfig cfg = CooldownConfig.parse(s, "gui:test");
        assertEquals(5_400_000L, cfg.getDurationMillis());
    }

    @Test
    void parseUsesDefaultIdWhenAbsentAndCustomWhenPresent() {
        ConfigurationSection s = section();
        s.set("seconds", 5);
        assertEquals("gui:test", CooldownConfig.parse(s, "gui:test").getId());

        s.set("id", "sacrifice");
        assertEquals("sacrifice", CooldownConfig.parse(s, "gui:test").getId());
    }

    @Test
    void parseAppliesDefaultsForText() {
        ConfigurationSection s = section();
        s.set("seconds", 5);
        CooldownConfig cfg = CooldownConfig.parse(s, "gui:test");
        assertEquals("&c&lWAIT", cfg.getTitle());
        assertTrue(cfg.getMessage().contains("%time_left%"));
        assertTrue(cfg.getSubtitle().contains("%time_left%"));
    }

    @Test
    void parseHonorsExplicitOverridesIncludingEmptyToDisable() {
        ConfigurationSection s = section();
        s.set("seconds", 5);
        s.set("title", "&4WAIT %minutes% MINUTES");
        s.set("message", "");
        s.set("bypass-permission", "smartmenus.cooldown.bypass");
        CooldownConfig cfg = CooldownConfig.parse(s, "gui:test");
        assertEquals("&4WAIT %minutes% MINUTES", cfg.getTitle());
        assertEquals("", cfg.getMessage());
        assertEquals("smartmenus.cooldown.bypass", cfg.getBypassPermission());
    }

    @Test
    void parseZeroDurationIsNull() {
        ConfigurationSection s = section();
        s.set("seconds", 0);
        assertNull(CooldownConfig.parse(s, "gui:test"));
    }

    @Test
    void disabledConfigIsNotEnabledViaFactory() {
        assertFalse(CooldownConfig.parse(section(), "x") != null);
    }
}
