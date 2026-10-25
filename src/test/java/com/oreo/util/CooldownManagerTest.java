package com.oreo.util;

import com.oreo.SmartMenus;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownManagerTest {

    @TempDir
    File dataFolder;

    private SmartMenus plugin;

    @BeforeEach
    void setUp() {
        plugin = Mockito.mock(SmartMenus.class);
        Mockito.lenient().when(plugin.getDataFolder()).thenReturn(dataFolder);
        Mockito.lenient().when(plugin.getLogger()).thenReturn(Logger.getLogger("CooldownManagerTest"));

        FileConfiguration config = Mockito.mock(FileConfiguration.class);
        Mockito.lenient().when(config.getString("cooldown-storage.type", "YAML")).thenReturn("YAML");
        Mockito.lenient().when(plugin.getConfig()).thenReturn(config);
    }

    private CooldownManager newManager() {
        return new CooldownManager(plugin);
    }

    @Test
    void applyThenRemainingIsPositiveAndBounded() {
        CooldownManager mgr = newManager();
        UUID uuid = UUID.randomUUID();
        mgr.apply("sacrifice", uuid, 60_000L);

        long remaining = mgr.remainingMillis("sacrifice", uuid);
        assertTrue(remaining > 0L);
        assertTrue(remaining <= 60_000L);
    }

    @Test
    void unknownIdOrPlayerHasNoCooldown() {
        CooldownManager mgr = newManager();
        assertEquals(0L, mgr.remainingMillis("nope", UUID.randomUUID()));
    }

    @Test
    void expiredCooldownReportsZero() {
        CooldownManager mgr = newManager();
        UUID uuid = UUID.randomUUID();
        mgr.apply("sacrifice", uuid, -1_000L);
        assertEquals(0L, mgr.remainingMillis("sacrifice", uuid));
    }

    @Test
    void clearRemovesCooldown() {
        CooldownManager mgr = newManager();
        UUID uuid = UUID.randomUUID();
        mgr.apply("sacrifice", uuid, 60_000L);
        mgr.clear("sacrifice", uuid);
        assertEquals(0L, mgr.remainingMillis("sacrifice", uuid));
    }

    @Test
    void saveAndLoadRoundTripPersistsActiveCooldown() {
        UUID uuid = UUID.randomUUID();

        CooldownManager first = newManager();
        first.apply("sacrifice", uuid, 60_000L);
        first.save();

        assertTrue(new File(dataFolder, "cooldowns.yml").exists());

        CooldownManager reloaded = newManager();
        long remaining = reloaded.remainingMillis("sacrifice", uuid);
        assertTrue(remaining > 0L);
        assertTrue(remaining <= 60_000L);
    }

    @Test
    void expiredCooldownsAreNotPersisted() {
        UUID uuid = UUID.randomUUID();

        CooldownManager first = newManager();
        first.apply("sacrifice", uuid, -1_000L);
        first.save();

        assertFalse(new File(dataFolder, "cooldowns.yml").exists());

        CooldownManager reloaded = newManager();
        assertEquals(0L, reloaded.remainingMillis("sacrifice", uuid));
    }

    @Test
    void formatTimeIsHumanReadable() {
        assertEquals("0s", CooldownManager.formatTime(0));
        assertEquals("59s", CooldownManager.formatTime(59));
        assertEquals("1m", CooldownManager.formatTime(60));
        assertEquals("1m 30s", CooldownManager.formatTime(90));
        assertEquals("30m", CooldownManager.formatTime(1_800));
        assertEquals("1h", CooldownManager.formatTime(3_600));
        assertEquals("1h 1m 1s", CooldownManager.formatTime(3_661));
        assertEquals("1d", CooldownManager.formatTime(86_400));
    }
}
