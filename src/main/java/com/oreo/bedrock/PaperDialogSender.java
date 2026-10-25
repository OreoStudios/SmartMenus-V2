package com.oreo.bedrock;

import com.oreo.SmartMenus;
import com.oreo.gui.ArgManager;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogInstancesProvider;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.dialog.DialogLike;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class PaperDialogSender {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private static final ClickCallback.Options CALLBACK_OPTIONS =
            ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).build();

    static void sendSimpleForm(SmartMenus plugin, Player player, BedrockFormDefinition form) {
        DialogInstancesProvider inst = DialogInstancesProvider.instance();

        List<ActionButton> buttons = buildButtons(plugin, player, form.getButtons());
        DialogBase base = buildBase(inst, player, form.getTitle(), form.getContent(),
                List.of(), List.of());

        Dialog dialog = Dialog.create(factory -> {
            var b = factory.empty();
            b.base(base);
            b.type(inst.multiAction(buttons).build());
        });

        showDialog(player, dialog);
    }

    static void sendModalForm(SmartMenus plugin, Player player, BedrockFormDefinition form) {
        DialogInstancesProvider inst = DialogInstancesProvider.instance();

        ActionButton yesBtn = ActionButton.builder(comp(player, form.getConfirmButton()))
                .action(DialogAction.customClick(
                        (response, audience) -> onAudiencePlayer(audience, p ->
                                BedrockManager.executeActions(p, form.getConfirmActions(), plugin)),
                        CALLBACK_OPTIONS))
                .build();

        ActionButton noBtn = ActionButton.builder(comp(player, form.getDenyButton()))
                .action(DialogAction.customClick(
                        (response, audience) -> onAudiencePlayer(audience, p ->
                                BedrockManager.executeActions(p, form.getDenyActions(), plugin)),
                        CALLBACK_OPTIONS))
                .build();

        DialogBase base = buildBase(inst, player, form.getTitle(), form.getContent(),
                List.of(), List.of());

        Dialog dialog = Dialog.create(factory -> {
            var b = factory.empty();
            b.base(base);
            b.type(inst.confirmation(yesBtn, noBtn));
        });

        showDialog(player, dialog);
    }

    static void sendCustomForm(SmartMenus plugin, Player player, BedrockFormDefinition form) {
        DialogInstancesProvider inst = DialogInstancesProvider.instance();

        List<DialogInput> dialogInputs = buildInputs(inst, player, form.getInputs());
        List<DialogBody> labelBodies   = buildLabelBodies(inst, player, form.getInputs());

        ActionButton submitBtn = ActionButton.builder(comp(player, form.getSubmitButtonText()))
                .action(DialogAction.customClick(
                        (response, audience) -> onAudiencePlayer(audience, p -> {
                            storeInputValues(p, form.getInputs(), response);
                            BedrockManager.executeActions(p, form.getSubmitActions(), plugin);
                        }),
                        CALLBACK_OPTIONS))
                .build();

        DialogBase base = buildBase(inst, player, form.getTitle(), form.getContent(),
                labelBodies, dialogInputs);

        Dialog dialog = Dialog.create(factory -> {
            var b = factory.empty();
            b.base(base);
            b.type(inst.multiAction(List.of(submitBtn)).build());
        });

        showDialog(player, dialog);
    }

    static void sendDialogue(SmartMenus plugin, Player player, BedrockFormDefinition form) {
        DialogInstancesProvider inst = DialogInstancesProvider.instance();

        List<ActionButton> buttons = buildButtons(plugin, player, form.getButtons());

        String titleStr   = applyPlaceholders(player, form.getTitle());
        String npcNameStr = applyPlaceholders(player, form.getNpcName());
        String contentStr = applyPlaceholders(player, form.getContent());

        DialogBase base = DialogBase.builder(LEGACY.deserialize(titleStr))
                .externalTitle(LEGACY.deserialize(npcNameStr))
                .body(List.of(inst.plainMessageDialogBody(LEGACY.deserialize(contentStr))))
                .build();

        Dialog dialog = Dialog.create(factory -> {
            var b = factory.empty();
            b.base(base);
            b.type(inst.multiAction(buttons).build());
        });

        showDialog(player, dialog);
    }

    private static List<ActionButton> buildButtons(SmartMenus plugin, Player player,
                                                   List<BedrockButton> buttons) {
        List<ActionButton> result = new ArrayList<>(buttons.size());
        for (BedrockButton btn : buttons) {
            Component label = comp(player, btn.getText());
            ActionButton ab = ActionButton.builder(label)
                    .action(DialogAction.customClick(
                            (response, audience) -> onAudiencePlayer(audience, p -> {
                                if (!BedrockManager.checkConditions(p, btn.getConditions())) {
                                    BedrockManager.sendDenyMessage(p, btn.getConditions());
                                    return;
                                }
                                BedrockManager.executeActions(p, btn.getActions(), plugin);
                            }),
                            CALLBACK_OPTIONS))
                    .build();
            result.add(ab);
        }
        return result;
    }

    private static DialogBase buildBase(DialogInstancesProvider inst, Player player,
                                        String title, String content,
                                        List<DialogBody> extraBodies,
                                        List<DialogInput> inputs) {
        List<DialogBody> bodies = new ArrayList<>();
        String resolved = applyPlaceholders(player, content);
        if (!resolved.isBlank()) {
            bodies.add(inst.plainMessageDialogBody(LEGACY.deserialize(resolved)));
        }
        bodies.addAll(extraBodies);

        DialogBase.Builder builder = DialogBase.builder(comp(player, title))
                .body(bodies);

        if (!inputs.isEmpty()) {
            builder.inputs(inputs);
        }

        return builder.build();
    }

    private static List<DialogInput> buildInputs(DialogInstancesProvider inst,
                                                  Player player,
                                                  List<BedrockFormInput> inputs) {
        List<DialogInput> result = new ArrayList<>();
        for (BedrockFormInput inp : inputs) {
            Component label = comp(player, inp.getLabel());
            switch (inp.getType()) {
                case INPUT:
                    result.add(inst.textBuilder(inp.getKey(), label)
                            .initial(inp.getDefaultText())
                            .maxLength(inp.getMaxLength())
                            .build());
                    break;
                case TOGGLE:
                    result.add(inst.booleanBuilder(inp.getKey(), label)
                            .initial(inp.getDefaultBool())
                            .build());
                    break;
                case SLIDER:
                    result.add(inst.numberRangeBuilder(inp.getKey(), label,
                            inp.getMin(), inp.getMax())
                            .step(inp.getStep())
                            .initial(inp.getDefaultFloat())
                            .build());
                    break;
                case STEP_SLIDER: {
                    List<SingleOptionDialogInput.OptionEntry> entries = new ArrayList<>();
                    List<String> steps = inp.getOptions();
                    for (int i = 0; i < steps.size(); i++) {
                        entries.add(inst.singleOptionEntry(
                                String.valueOf(i),
                                Component.text(steps.get(i)),
                                i == inp.getDefaultIndex()));
                    }
                    result.add(inst.singleOptionBuilder(inp.getKey(), label, entries).build());
                    break;
                }
                case DROPDOWN: {
                    List<SingleOptionDialogInput.OptionEntry> entries = new ArrayList<>();
                    List<String> opts = inp.getOptions();
                    for (int i = 0; i < opts.size(); i++) {
                        entries.add(inst.singleOptionEntry(
                                String.valueOf(i),
                                Component.text(opts.get(i)),
                                i == inp.getDefaultIndex()));
                    }
                    result.add(inst.singleOptionBuilder(inp.getKey(), label, entries).build());
                    break;
                }
                case LABEL:
                    break;
            }
        }
        return result;
    }

    private static List<DialogBody> buildLabelBodies(DialogInstancesProvider inst,
                                                      Player player,
                                                      List<BedrockFormInput> inputs) {
        List<DialogBody> result = new ArrayList<>();
        for (BedrockFormInput inp : inputs) {
            if (inp.getType() == BedrockFormInput.InputType.LABEL) {
                result.add(inst.plainMessageDialogBody(comp(player, inp.getLabel())));
            }
        }
        return result;
    }

    private static void storeInputValues(Player player, List<BedrockFormInput> inputs,
                                          DialogResponseView response) {
        Map<String, String> existing = new HashMap<>(ArgManager.getAll(player.getUniqueId()));
        for (BedrockFormInput inp : inputs) {
            if (inp.getType() == BedrockFormInput.InputType.LABEL) continue;
            existing.put(inp.getKey(), readValue(inp, response));
        }
        ArgManager.store(player.getUniqueId(), existing);
    }

    private static String readValue(BedrockFormInput inp, DialogResponseView response) {
        try {
            switch (inp.getType()) {
                case INPUT: {
                    String v = response.getText(inp.getKey());
                    return v != null ? v : "";
                }
                case TOGGLE: {
                    Boolean v = response.getBoolean(inp.getKey());
                    return v != null ? String.valueOf(v) : "false";
                }
                case SLIDER: {
                    Float v = response.getFloat(inp.getKey());
                    return v != null ? String.valueOf(v) : "0";
                }
                case STEP_SLIDER: {
                    String v = response.getText(inp.getKey());
                    if (v == null) return "";
                    try {
                        int idx = Integer.parseInt(v);
                        List<String> steps = inp.getOptions();
                        if (idx >= 0 && idx < steps.size()) return steps.get(idx);
                    } catch (NumberFormatException ignored) {}
                    return v;
                }
                case DROPDOWN: {
                    String v = response.getText(inp.getKey());
                    if (v == null) return "";
                    try {
                        int idx = Integer.parseInt(v);
                        List<String> opts = inp.getOptions();
                        if (idx >= 0 && idx < opts.size()) return opts.get(idx);
                    } catch (NumberFormatException ignored) {}
                    return v;
                }
                default:
                    return "";
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[SmartMenus] Failed to read bedrock form input '"
                    + inp.getKey() + "': " + e.getMessage());
            return "";
        }
    }

    private static void showDialog(Player player, Dialog dialog) {
        ((Audience) player).showDialog((DialogLike) dialog);
    }

    private static void onAudiencePlayer(Audience audience,
                                          java.util.function.Consumer<Player> consumer) {
        if (audience instanceof Player p) {
            consumer.accept(p);
        }
    }

    private static Component comp(Player player, String text) {
        return LEGACY.deserialize(applyPlaceholders(player, text == null ? "" : text));
    }

    private static String applyPlaceholders(Player player, String text) {
        if (text == null) return "";
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                text = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[SmartMenus] PlaceholderAPI failed for '"
                        + text + "': " + e.getMessage());
            }
        }
        return text;
    }
}
