package com.oreo.condition;

import com.oreo.SmartMenus;
import com.oreo.condition.impl.PermissionCondition;
import com.oreo.condition.impl.PlaceholderCompareCondition;
import com.oreo.util.MessageManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

class ConditionFactoryTest {

    private ConditionFactory factory;

    @BeforeEach
    void setUp() {
        SmartMenus plugin = Mockito.mock(SmartMenus.class);
        MessageManager messages = Mockito.mock(MessageManager.class);
        Mockito.lenient().when(plugin.getLogger()).thenReturn(Logger.getLogger("ConditionFactoryTest"));
        Mockito.lenient().when(plugin.getMessageManager()).thenReturn(messages);
        Mockito.lenient().when(messages.getMessage(anyString(), any(Map.class))).thenReturn("");
        factory = new ConditionFactory(plugin);
    }

    private ConfigurationSection section() {
        return new MemoryConfiguration().createSection("condition");
    }

    @Test
    void missingTypeReturnsNull() {
        assertNull(factory.parseCondition(section()));
    }

    @Test
    void unknownTypeReturnsNull() {
        ConfigurationSection s = section();
        s.set("type", "TOTALLY_UNKNOWN");
        assertNull(factory.parseCondition(s));
    }

    @Test
    void permissionConditionIsBuilt() {
        ConfigurationSection s = section();
        s.set("type", "PERMISSION");
        s.set("permission", "smartmenus.use");
        assertInstanceOf(PermissionCondition.class, factory.parseCondition(s));
    }

    @Test
    void typeMatchingIsCaseInsensitive() {
        ConfigurationSection s = section();
        s.set("type", "permission");
        s.set("permission", "smartmenus.use");
        assertNotNull(factory.parseCondition(s));
    }

    @Test
    void placeholderEqualsConditionIsBuilt() {
        ConfigurationSection s = section();
        s.set("type", "PLACEHOLDER_EQUALS");
        s.set("placeholder", "%player_world%");
        s.set("value", "world");
        assertInstanceOf(PlaceholderCompareCondition.class, factory.parseCondition(s));
    }

    @Test
    void worldConditionWithoutAnyWorldReturnsNull() {
        ConfigurationSection s = section();
        s.set("type", "WORLD");
        assertNull(factory.parseCondition(s));
    }
}
