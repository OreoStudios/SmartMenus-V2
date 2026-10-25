package com.oreo.gui;

import com.oreo.SmartMenus;
import com.oreo.bedrock.BedrockManager;
import com.oreo.condition.Condition;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public final class GuiOpener {

    private GuiOpener() {
    }

    /** How a GUI open should behave regarding requirement/cooldown checks and Bedrock handling. */
    public record OpenOptions(boolean checkRequirements, boolean checkCooldown, boolean allowBedrockAutoConvert) {

        /** Player-initiated open: enforce requirements and cooldowns, allow Bedrock auto-conversion. */
        public static OpenOptions checked() {
            return new OpenOptions(true, true, true);
        }

        /** Programmatic open (action/command/back): skip requirement and cooldown checks. */
        public static OpenOptions unchecked() {
            return new OpenOptions(false, false, true);
        }

        /** Like {@link #unchecked()} but without the Bedrock auto-conversion fallback. */
        public static OpenOptions uncheckedNoBedrockConvert() {
            return new OpenOptions(false, false, false);
        }
    }

    public static boolean open(SmartMenus plugin, Player player, GuiDefinition def, OpenOptions options) {
        return open(plugin, player, def, options, null);
    }

    /**
     * Opens a menu for {@code player} (the viewer) optionally displaying {@code target}'s data.
     *
     * <p>When {@code target} is non-null and differs from the viewer, a {@link MenuContext} is passed
     * to the menu so data sources such as {@code PLAYER_ENDERCHEST} resolve against the target.</p>
     */
    public static boolean open(SmartMenus plugin, Player player, GuiDefinition def, OpenOptions options, Player target) {
        if (def == null) return false;

        if (options.checkRequirements()) {
            for (Condition req : def.getOpenRequirements()) {
                if (!req.check(player)) {
                    player.sendMessage(req.getErrorMessage(player));
                    return false;
                }
            }
        }

        if (options.checkCooldown() && plugin.getCooldownManager() != null
                && !plugin.getCooldownManager().tryUse(player, def.getOpenCooldown())) {
            return false;
        }

        BedrockManager bm = plugin.getBedrockManager();
        if (bm != null) {
            if (bm.openForBedrock(player, def)) return true;
            if (options.allowBedrockAutoConvert() && bm.autoConvertForBedrock(player, def)) return true;
        }

        Map<String, Object> properties = new HashMap<>();
        // Always record a context so the provider can distinguish viewer/target uniformly.
        properties.put(MenuContext.PROPERTY_KEY,
                new MenuContext(player, (target != null && !target.equals(player)) ? target : null));
        def.createInventory(plugin.getInventoryManager(), plugin, player).open(player, properties);

        // Record the open in the navigation history so the BACK action can return here.
        // Skip consecutive duplicates (e.g. a refresh re-opening the same menu).
        String id = def.getId();
        if (id != null && !id.equals(NavigationManager.peekCurrent(player.getUniqueId()))) {
            NavigationManager.push(player.getUniqueId(), id);
        }
        return true;
    }
}
