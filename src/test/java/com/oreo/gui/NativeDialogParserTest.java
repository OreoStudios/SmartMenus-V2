package com.oreo.gui;

import com.oreo.SmartMenus;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NativeDialogParserTest {
    private BedrockFormParser parser() {
        SmartMenus plugin = mock(SmartMenus.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        return new BedrockFormParser(plugin, mock(ConditionListParser.class));
    }

    private YamlConfiguration yaml(String text) throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(text);
        return yaml;
    }

    @Test void parsesNamedInputsAndButtons() throws Exception {
        var config = yaml("""
            inputs:
              rank:
                type: SINGLE_OPTION
                options: [VIP, MVP]
              notes:
                type: MULTILINE_TEXT
            buttons:
              submit:
                label: Submit
                actions:
                  - type: PLAYER_MESSAGE
                    message: hello
            """);
        var dialog = NativeDialogParser.parse("test", "Title", config, parser());
        assertEquals(2, dialog.inputs().size());
        assertEquals("rank", dialog.inputs().getFirst().getKey());
        assertTrue(dialog.multilineKeys().contains("notes"));
        assertEquals(1, dialog.buttons().getFirst().getActions().size());
    }

    @Test void rejectsInvalidConfirmation() throws Exception {
        var config = yaml("type: CONFIRMATION\nbuttons:\n  yes:\n    label: Yes\n");
        assertThrows(IllegalArgumentException.class, () -> NativeDialogParser.parse("test", "Title", config, parser()));
    }

    @Test void rejectsDuplicateInputKeys() throws Exception {
        var config = yaml("""
            inputs:
              - {key: duplicate, type: TEXT}
              - {key: duplicate, type: BOOLEAN}
            buttons:
              submit:
                label: Submit
            """);
        assertThrows(IllegalArgumentException.class, () -> NativeDialogParser.parse("test", "Title", config, parser()));
    }

    @Test void rejectsUnknownButtonActions() throws Exception {
        var config = yaml("""
            buttons:
              submit:
                label: Submit
                actions:
                  - type: NO_SUCH_ACTION
            """);
        assertThrows(IllegalArgumentException.class, () -> NativeDialogParser.parse("test", "Title", config, parser()));
    }
}
