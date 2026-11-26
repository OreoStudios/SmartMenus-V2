package com.oreo.bedrock;

import java.util.List;
import java.util.Set;
import org.bukkit.Material;

/** Native dialog layout, independent of the legacy Bedrock-only form. */
public record NativeDialogDefinition(String title, String externalTitle, boolean closeOnEscape,
        int columns, boolean confirmation, List<Body> body, List<BedrockFormInput> inputs,
        List<BedrockButton> buttons, Set<String> multilineKeys) {
    public NativeDialogDefinition {
        body = List.copyOf(body);
        inputs = List.copyOf(inputs);
        buttons = List.copyOf(buttons);
        multilineKeys = Set.copyOf(multilineKeys);
    }
    public record Body(String text, Material material) {}
}
