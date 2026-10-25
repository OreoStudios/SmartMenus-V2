package com.oreo.bedrock;

import com.oreo.action.Action;
import com.oreo.condition.Condition;

import java.util.Collections;
import java.util.List;

public class BedrockButton {

    private final String text;

    private final String imageType;
    private final String imageData;
    private final List<Condition> conditions;
    private final List<Action> actions;

    public BedrockButton(String text, String imageType, String imageData,
                         List<Condition> conditions, List<Action> actions) {
        this.text = text == null ? "" : text;
        this.imageType = imageType;
        this.imageData = imageData;
        this.conditions = conditions == null ? Collections.emptyList() : conditions;
        this.actions = actions == null ? Collections.emptyList() : actions;
    }

    public String getText() { return text; }
    public String getImageType() { return imageType; }
    public String getImageData() { return imageData; }
    public boolean hasImage() { return imageType != null && imageData != null && !imageData.isEmpty(); }
    public List<Condition> getConditions() { return conditions; }
    public List<Action> getActions() { return actions; }
}
