package com.oreo.bedrock;

import com.oreo.SmartMenus;
import com.oreo.action.Action;
import com.oreo.condition.Condition;
import com.oreo.gui.ArgManager;
import com.oreo.gui.GuiItem;
import com.oreo.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class BedrockManager {

    private static final String FLOODGATE_UUID_PREFIX = "00000000-0000-0000-";

    private final SmartMenus plugin;
    private final Logger log;

    private boolean floodgateEnabled = false;

    public BedrockManager(SmartMenus plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        detect();
    }

    private void detect() {
        if (Bukkit.getPluginManager().getPlugin("floodgate") != null) {
            try {
                Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                floodgateEnabled = true;
                log.info("[SmartMenus] Floodgate detected — using FloodgateApi for Bedrock detection.");
            } catch (ClassNotFoundException e) {
                log.warning("[SmartMenus] floodgate plugin present but API class not found; "
                        + "falling back to UUID-prefix detection.");
            }
        }

        log.info("[SmartMenus] Bedrock support active — forms sent via Paper Dialog API "
                + "(Geyser translates automatically).");
    }

    public boolean isBedrockPlayer(Player player) {
        if (floodgateEnabled) {
            try {
                return org.geysermc.floodgate.api.FloodgateApi.getInstance()
                        .isFloodgatePlayer(player.getUniqueId());
            } catch (Exception e) {
                log.warning("[SmartMenus] FloodgateApi check failed for " + player.getName() + ": " + e.getMessage());
            }
        }
        return isFloodgateUUID(player.getUniqueId());
    }

    public boolean openForBedrock(Player player, com.oreo.gui.GuiDefinition definition) {
        if (!isBedrockPlayer(player)) return false;
        BedrockFormDefinition form = definition.getBedrockDefinition();
        if (form == null) return false;

        try {
            switch (form.getType()) {
                case SIMPLE_FORM:
                    PaperDialogSender.sendSimpleForm(plugin, player, form);
                    break;
                case MODAL_FORM:
                    PaperDialogSender.sendModalForm(plugin, player, form);
                    break;
                case CUSTOM_FORM:
                    PaperDialogSender.sendCustomForm(plugin, player, form);
                    break;
                case DIALOGUE:
                    PaperDialogSender.sendDialogue(plugin, player, form);
                    break;
                default:
                    return false;
            }
            return true;
        } catch (Exception e) {
            log.warning("[SmartMenus] Failed to send Paper dialog to " + player.getName()
                    + ": " + e.getMessage());
            return false;
        }
    }

    public boolean autoConvertForBedrock(Player player,
                                          com.oreo.gui.GuiDefinition definition) {
        if (!isBedrockPlayer(player)) return false;
        if (!definition.isBedrockAutoConvert()) return false;
        if (definition.hasBedrockForm()) return false;

        BedrockFormDefinition generated = buildAutoForm(player, definition);
        if (generated == null) return false;

        try {
            PaperDialogSender.sendSimpleForm(plugin, player, generated);
            return true;
        } catch (Exception e) {
            log.warning("[SmartMenus] Auto-convert failed for " + player.getName()
                    + " / " + definition.getId() + ": " + e.getMessage());
            return false;
        }
    }

    private BedrockFormDefinition buildAutoForm(Player player,
                                                 com.oreo.gui.GuiDefinition definition) {
        List<BedrockButton> buttons = new ArrayList<>();

        List<Integer> sortedSlots = new ArrayList<>(definition.getItems().keySet());
        Collections.sort(sortedSlots);

        for (int slot : sortedSlots) {
            GuiItem item = definition.getItems().get(slot);
            if (item == null) continue;

            boolean hasActions = !item.getActionsForClick("ANY").isEmpty()
                    || !item.getClickActions().isEmpty()
                    || !item.getCommands().isEmpty();
            if (!hasActions) continue;

            GuiItem effective = item;
            for (com.oreo.condition.Condition c : item.getViewRequirements()) {
                try {
                    if (!c.check(player)) {
                        effective = item.getElseItem();
                        break;
                    }
                } catch (Exception e) {
                    log.warning("[SmartMenus] Condition check error during auto-form build: " + e.getMessage());
                }
            }
            if (effective == null) continue;

            String name = effective.getName();
            if (name == null || name.isBlank()) {

                name = formatMaterialName(effective.getMaterial());
            }

            List<String> lore = effective.getLore();
            if (!lore.isEmpty()) {
                StringBuilder sb = new StringBuilder(name);
                int limit = Math.min(lore.size(), 3);
                for (int i = 0; i < limit; i++) {
                    String line = lore.get(i);
                    if (line != null && !line.isBlank()) {
                        sb.append("\n").append(line);
                    }
                }
                name = sb.toString();
            }

            List<com.oreo.action.Action> actions = item.getActionsForClick("ANY");

            buttons.add(new BedrockButton(name, null, null,
                    item.getConditions(), actions));
        }

        if (buttons.isEmpty()) return null;

        return new BedrockFormDefinition.Builder(BedrockFormDefinition.FormType.SIMPLE_FORM)
                .title(definition.getTitle())
                .buttons(buttons)
                .npcName(definition.getTitle())
                .dialogueTag(definition.getId() + ".auto")
                .build();
    }

    private static String formatMaterialName(String material) {
        if (material == null || material.isBlank()) return "Button";

        String[] parts = material.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part.substring(1).toLowerCase());
                sb.append(' ');
            }
        }
        return sb.toString().trim();
    }

    static boolean checkConditions(Player player, List<Condition> conditions) {
        for (Condition c : conditions) {
            try {
                if (!c.check(player)) return false;
            } catch (Exception e) {
                org.bukkit.Bukkit.getLogger().warning("[SmartMenus] Bedrock condition check error: " + e.getMessage());
            }
        }
        return true;
    }

    static void sendDenyMessage(Player player, List<Condition> conditions) {
        for (Condition c : conditions) {
            try {
                if (!c.check(player)) {
                    String msg = c.getErrorMessage(player);
                    if (msg != null && !msg.isEmpty()) {
                        player.sendMessage(ColorUtil.color(msg));
                    }
                    return;
                }
            } catch (Exception e) {
                org.bukkit.Bukkit.getLogger().warning("[SmartMenus] Bedrock deny message condition error: " + e.getMessage());
            }
        }
    }

    static void executeActions(Player player, List<Action> actions, SmartMenus plugin) {
        Map<String, String> vars = ArgManager.getAll(player.getUniqueId());
        for (Action a : actions) {
            try {
                a.execute(plugin, player, vars);
            } catch (Exception e) {
                plugin.getLogger().warning("Bedrock action error: " + e.getMessage());
            }
        }
    }

    private static boolean isFloodgateUUID(UUID uuid) {
        return uuid.toString().startsWith(FLOODGATE_UUID_PREFIX);
    }

    public boolean isFloodgateEnabled() { return floodgateEnabled; }
}
