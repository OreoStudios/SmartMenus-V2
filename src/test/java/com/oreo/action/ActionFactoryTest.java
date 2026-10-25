package com.oreo.action;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionFactoryTest {

    @Test
    void replaceVarsSubstitutesNamedVars() {
        Map<String, String> vars = new HashMap<>();
        vars.put("name", "Bob");
        assertEquals("hi Bob", ActionFactory.replaceVars("hi %name%", null, vars));
    }

    @Test
    void replaceVarsSubstitutesMultipleVars() {
        Map<String, String> vars = new HashMap<>();
        vars.put("a", "1");
        vars.put("b", "2");
        assertEquals("1 and 2", ActionFactory.replaceVars("%a% and %b%", null, vars));
    }

    @Test
    void replaceVarsLeavesUnknownPlaceholderUntouched() {
        Map<String, String> vars = new HashMap<>();
        vars.put("y", "1");
        assertEquals("%x%", ActionFactory.replaceVars("%x%", null, vars));
    }

    @Test
    void replaceVarsIsNullSafe() {
        assertEquals("", ActionFactory.replaceVars(null, null, null));
        assertEquals("plain", ActionFactory.replaceVars("plain", null, null));
    }

    @Test
    void parseActionReturnsNullForNullOrMissingType() {
        assertNull(ActionFactory.parseAction(null));
        assertNull(ActionFactory.parseAction(new HashMap<>()));
    }

    @Test
    void parseActionReturnsNullForUnknownType() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "TOTALLY_UNKNOWN");
        assertNull(ActionFactory.parseAction(map));
    }

    @Test
    void parseActionBuildsKnownTypes() {
        Map<String, Object> title = new HashMap<>();
        title.put("type", "TITLE");
        title.put("title", "Hello");
        assertNotNull(ActionFactory.parseAction(title));

        Map<String, Object> message = new HashMap<>();
        message.put("type", "PLAYER_MESSAGE");
        message.put("message", "hi");
        assertNotNull(ActionFactory.parseAction(message));
    }

    @Test
    void parseActionTypeIsCaseInsensitive() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "broadcast");
        map.put("message", "x");
        assertNotNull(ActionFactory.parseAction(map));
    }

    @Test
    void parseActionsFiltersNonMapsAndUnknownEntries() {
        List<Object> raw = new ArrayList<>();

        Map<String, Object> valid = new HashMap<>();
        valid.put("type", "BROADCAST");
        valid.put("message", "x");
        raw.add(valid);

        raw.add("not a map");

        Map<String, Object> unknown = new HashMap<>();
        unknown.put("type", "NOPE");
        raw.add(unknown);

        List<Action> actions = ActionFactory.parseActions(raw);
        assertEquals(1, actions.size());
    }

    @Test
    void parseActionsNullYieldsEmptyList() {
        assertTrue(ActionFactory.parseActions(null).isEmpty());
    }
}
