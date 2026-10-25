package com.oreo.bedrock;

import com.oreo.action.Action;

import java.util.Collections;
import java.util.List;

public class BedrockFormDefinition {

    public enum FormType {
        SIMPLE_FORM, MODAL_FORM, CUSTOM_FORM, DIALOGUE
    }

    private final FormType type;
    private final String title;
    private final String content;
    private final List<BedrockButton> buttons;
    private final String confirmButton;
    private final String denyButton;
    private final List<Action> confirmActions;
    private final List<Action> denyActions;
    private final List<BedrockFormInput> inputs;
    private final List<Action> submitActions;
    private final String submitButtonText;
    private final List<Action> closeActions;
    private final String npcName;
    private final String dialogueTag;

    private BedrockFormDefinition(Builder b) {
        this.type           = b.type;
        this.title          = b.title == null ? "" : b.title;
        this.content        = b.content == null ? "" : b.content;
        this.buttons        = b.buttons == null ? Collections.emptyList() : b.buttons;
        this.confirmButton  = b.confirmButton == null ? "Confirm" : b.confirmButton;
        this.denyButton     = b.denyButton == null ? "Cancel" : b.denyButton;
        this.confirmActions = b.confirmActions == null ? Collections.emptyList() : b.confirmActions;
        this.denyActions    = b.denyActions == null ? Collections.emptyList() : b.denyActions;
        this.inputs         = b.inputs == null ? Collections.emptyList() : b.inputs;
        this.submitActions  = b.submitActions == null ? Collections.emptyList() : b.submitActions;
        this.submitButtonText = b.submitButtonText == null ? "Submit" : b.submitButtonText;
        this.closeActions   = b.closeActions == null ? Collections.emptyList() : b.closeActions;
        this.npcName        = b.npcName == null ? "NPC" : b.npcName;
        this.dialogueTag    = b.dialogueTag == null ? "dialogue" : b.dialogueTag;
    }

    public static final class Builder {
        private final FormType type;
        private String title = "";
        private String content = "";
        private List<BedrockButton> buttons;
        private String confirmButton = "Confirm";
        private String denyButton = "Cancel";
        private List<Action> confirmActions;
        private List<Action> denyActions;
        private List<BedrockFormInput> inputs;
        private List<Action> submitActions;
