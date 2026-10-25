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
