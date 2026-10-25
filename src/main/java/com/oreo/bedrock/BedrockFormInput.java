package com.oreo.bedrock;

import java.util.Collections;
import java.util.List;

public class BedrockFormInput {

    public enum InputType {

        INPUT,

        DROPDOWN,

        SLIDER,

        STEP_SLIDER,

        TOGGLE,

        LABEL
    }

    private final String key;
    private final InputType type;
    private final String label;

    private final String placeholder;
    private final String defaultText;
    private final int maxLength;

    private final List<String> options;
    private final int defaultIndex;

    private final float min;
    private final float max;
    private final float step;
    private final float defaultFloat;

    private final boolean defaultBool;

    private BedrockFormInput(String key, InputType type, String label,
                             String placeholder, String defaultText, int maxLength,
                             List<String> options, int defaultIndex,
                             float min, float max, float step, float defaultFloat,
                             boolean defaultBool) {
        this.key = key == null ? type.name().toLowerCase() : key;
        this.type = type;
        this.label = label == null ? "" : label;
        this.placeholder = placeholder == null ? "" : placeholder;
        this.defaultText = defaultText == null ? "" : defaultText;
        this.maxLength = maxLength <= 0 ? 256 : maxLength;
        this.options = options == null ? Collections.emptyList() : options;
        this.defaultIndex = defaultIndex;
        this.min = min;
        this.max = max == 0 ? 100f : max;
        this.step = step <= 0 ? 1f : step;
        this.defaultFloat = defaultFloat;
        this.defaultBool = defaultBool;
    }

    public static BedrockFormInput input(String key, String label, String placeholder,
                                         String defaultText, int maxLength) {
        return new BedrockFormInput(key, InputType.INPUT, label, placeholder, defaultText,
                maxLength, null, 0, 0, 0, 0, 0, false);
    }

    public static BedrockFormInput dropdown(String key, String label,
                                             List<String> options, int defaultIndex) {
        return new BedrockFormInput(key, InputType.DROPDOWN, label, null, null, 0,
                options, defaultIndex, 0, 0, 0, 0, false);
    }

    public static BedrockFormInput slider(String key, String label,
                                           float min, float max, float step, float defaultFloat) {
        return new BedrockFormInput(key, InputType.SLIDER, label, null, null, 0,
                null, 0, min, max, step, defaultFloat, false);
    }

    public static BedrockFormInput stepSlider(String key, String label,
                                               List<String> steps, int defaultIndex) {
        return new BedrockFormInput(key, InputType.STEP_SLIDER, label, null, null, 0,
                steps, defaultIndex, 0, steps.size() - 1f, 1f, defaultIndex, false);
    }

    public static BedrockFormInput toggle(String key, String label, boolean defaultBool) {
        return new BedrockFormInput(key, InputType.TOGGLE, label, null, null, 0,
                null, 0, 0, 0, 0, 0, defaultBool);
    }

    public static BedrockFormInput label(String label) {
        return new BedrockFormInput("label", InputType.LABEL, label, null, null, 0,
                null, 0, 0, 0, 0, 0, false);
    }

    public String getKey() { return key; }
    public InputType getType() { return type; }
    public String getLabel() { return label; }
    public String getPlaceholder() { return placeholder; }
    public String getDefaultText() { return defaultText; }
    public int getMaxLength() { return maxLength; }
    public List<String> getOptions() { return options; }
    public int getDefaultIndex() { return defaultIndex; }
    public float getMin() { return min; }
    public float getMax() { return max; }
    public float getStep() { return step; }
    public float getDefaultFloat() { return defaultFloat; }
    public boolean getDefaultBool() { return defaultBool; }
}
