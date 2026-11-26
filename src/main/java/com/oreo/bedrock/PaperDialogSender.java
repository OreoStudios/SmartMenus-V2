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

public final class PaperDialogSender {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private static final ClickCallback.Options CALLBACK_OPTIONS =
            ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).build();

    public static void sendNative(SmartMenus plugin, Player player, com.oreo.gui.GuiDefinition definition) {
        NativeDialogDefinition config = definition.getNativeDialog();
        DialogInstancesProvider inst = DialogInstancesProvider.instance();
        List<DialogBody> bodies = new ArrayList<>();
        for (NativeDialogDefinition.Body entry : config.body()) {
            if (entry.material() != null) {
                var item = DialogBody.item(new org.bukkit.inventory.ItemStack(entry.material()));
                if (!entry.text().isBlank()) item.description(DialogBody.plainMessage(nativeComponent(player, entry.text())));
                bodies.add(item.build());
            } else bodies.add(DialogBody.plainMessage(nativeComponent(player, entry.text())));
        }
        List<DialogInput> inputs = new ArrayList<>();
        for (BedrockFormInput input : config.inputs()) {
            Component label = nativeComponent(player, input.getLabel());
            switch (input.getType()) {
                case INPUT -> {
                    var builder = inst.textBuilder(input.getKey(), label)
                            .initial(applyPlaceholders(player, input.getDefaultText())).maxLength(input.getMaxLength());
                    if (config.multilineKeys().contains(input.getKey()))
                        builder.multiline(io.papermc.paper.registry.data.dialog.input.TextDialogInput.MultilineOptions.create(5, 80));
                    inputs.add(builder.build());
                }
                case TOGGLE -> inputs.add(inst.booleanBuilder(input.getKey(), label).initial(input.getDefaultBool()).build());
                case SLIDER -> inputs.add(inst.numberRangeBuilder(input.getKey(), label, input.getMin(), input.getMax())
                        .step(input.getStep()).initial(input.getDefaultFloat()).build());
                case DROPDOWN, STEP_SLIDER -> {
                    List<SingleOptionDialogInput.OptionEntry> entries = new ArrayList<>();
                    for (int i = 0; i < input.getOptions().size(); i++)
                        entries.add(inst.singleOptionEntry(String.valueOf(i), nativeComponent(player, input.getOptions().get(i)), i == input.getDefaultIndex()));
                    inputs.add(inst.singleOptionBuilder(input.getKey(), label, entries).build());
                }
                default -> throw new IllegalArgumentException("Unsupported native dialog input");
            }
        }
        var baseBuilder = DialogBase.builder(nativeComponent(player, config.title()))
                .canCloseWithEscape(config.closeOnEscape()).body(bodies).inputs(inputs);
        if (config.externalTitle() != null) baseBuilder.externalTitle(nativeComponent(player, config.externalTitle()));
        DialogBase base = baseBuilder.build();
        var used = new java.util.concurrent.atomic.AtomicBoolean();
        var sessionArgs = new HashMap<>(ArgManager.getAll(player.getUniqueId()));
        List<ActionButton> buttons = new ArrayList<>();
        for (BedrockButton button : config.buttons()) {
            buttons.add(ActionButton.builder(nativeComponent(player, button.getText()))
                    .action(DialogAction.customClick((response, audience) -> onAudiencePlayer(audience, p -> {
                        if (!p.getUniqueId().equals(player.getUniqueId()) || !used.compareAndSet(false, true)) return;
                        Map<String, String> values = new HashMap<>(sessionArgs);
                        try {
                            for (BedrockFormInput input : config.inputs()) values.put(input.getKey(), validatedValue(input, response));
                        } catch (RuntimeException e) {
                            p.sendMessage("Invalid dialog response.");
                            return;
                        }
                        com.oreo.util.SmartScheduler.runTaskForPlayer(plugin, p, () -> {
                            if (!p.isOnline() || plugin.getGuiRegistry().getGui(definition.getId()) != definition
                                    || !definition.getId().equals(com.oreo.gui.NavigationManager.peekCurrent(p.getUniqueId()))) return;
                            try {
                                for (var requirement : definition.getOpenRequirements()) {
                                    if (!requirement.check(p)) { p.sendMessage(requirement.getErrorMessage(p)); return; }
                                }
                                for (var condition : button.getConditions()) {
                                    if (!condition.check(p)) { p.sendMessage(condition.getErrorMessage(p)); return; }
                                }
                                ArgManager.store(p.getUniqueId(), values);
                                BedrockManager.executeActions(p, button.getActions(), plugin);
                            } catch (RuntimeException e) {
                                plugin.getLogger().warning("Native dialog action failed: " + e.getMessage());
                            }
                        });
                    }), ClickCallback.Options.builder().uses(1).build())).build());
        }
        Dialog dialog = Dialog.create(factory -> {
            var builder = factory.empty();
            builder.base(base);
            if (config.confirmation()) builder.type(inst.confirmation(buttons.get(0), buttons.get(1)));
            else builder.type(inst.multiAction(buttons).columns(config.columns()).build());
        });
        player.closeInventory();
        showDialog(player, dialog);
    }

    private static Component nativeComponent(Player player, String value) {
        String text = applyPlaceholders(player, value == null ? "" : value.replace("{player}", player.getName()));
        if (text.contains("<")) return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(text);
        return com.oreo.util.ColorUtil.colorComponent(text);
    }

    private static String validatedValue(BedrockFormInput input, DialogResponseView response) {
        return switch (input.getType()) {
            case INPUT -> {
                String value = response.getText(input.getKey());
                if (value == null || value.length() > input.getMaxLength() || value.indexOf(0) >= 0)
                    throw new IllegalArgumentException("Invalid text");
                yield value;
            }
            case TOGGLE -> {
                Boolean value = response.getBoolean(input.getKey());
                if (value == null) throw new IllegalArgumentException("Missing boolean");
                yield value.toString();
            }
            case SLIDER -> {
                Float value = response.getFloat(input.getKey());
                if (value == null || !Float.isFinite(value) || value < input.getMin() || value > input.getMax())
                    throw new IllegalArgumentException("Invalid number");
                double steps = (value - input.getMin()) / input.getStep();
                if (Math.abs(steps - Math.rint(steps)) > 0.001 && Math.abs(value - input.getMax()) > 0.0001)
                    throw new IllegalArgumentException("Invalid step");
                yield value.toString();
            }
            case DROPDOWN, STEP_SLIDER -> {
                int index = Integer.parseInt(response.getText(input.getKey()));
                if (index < 0 || index >= input.getOptions().size()) throw new IllegalArgumentException("Invalid option");
                yield input.getOptions().get(index);
            }
            default -> throw new IllegalArgumentException("Unsupported input");
        };
    }

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
