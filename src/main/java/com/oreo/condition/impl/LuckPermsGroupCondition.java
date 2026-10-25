
package com.oreo.condition.impl;

import com.oreo.SmartMenus;
import com.oreo.condition.Condition;
import com.oreo.condition.ConditionType;
import com.oreo.util.ColorUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LuckPermsGroupCondition implements Condition {
    private final SmartMenus plugin;
    private final String group;
    private final boolean includeInherited;
    private final boolean negate;
    private final String customErrorMessage;

    private LuckPerms luckPerms;

    public LuckPermsGroupCondition(
            SmartMenus plugin,
            String group,
            boolean includeInherited,
            boolean negate,
            String customErrorMessage
    ) {
        this.plugin = plugin;
        this.group = group == null ? "" : group.trim();
        this.includeInherited = includeInherited;
        this.negate = negate;
        this.customErrorMessage = customErrorMessage;
        setupLuckPerms();
    }

    private void setupLuckPerms() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) return;

        RegisteredServiceProvider<LuckPerms> rsp = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (rsp != null) luckPerms = rsp.getProvider();
    }

    private boolean isInGroup(Player player) {
        if (luckPerms == null || group.isEmpty()) return false;

        User user;
        try {
            user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
        } catch (Throwable ignored) {
            user = luckPerms.getUserManager().getUser(player.getUniqueId());
        }
        if (user == null) return false;

        String target = group.toLowerCase(Locale.ROOT);

        if (user.getPrimaryGroup() != null && user.getPrimaryGroup().equalsIgnoreCase(target)) {
            return true;
        }

        if (!includeInherited) return false;

        QueryOptions queryOptions = luckPerms.getContextManager().getQueryOptions(player);

        for (Group g : user.getInheritedGroups(queryOptions)) {
            if (g.getName().equalsIgnoreCase(target)) return true;
        }

        return false;
    }

    @Override
    public boolean check(Player player) {
        boolean inGroup = isInGroup(player);
        return negate ? !inGroup : inGroup;
    }

    @Override
    public boolean take(Player player) {
        return check(player);
    }

    @Override
    public String getErrorMessage(Player player) {
        if (customErrorMessage != null && !customErrorMessage.isBlank()) {
            String message = customErrorMessage
                    .replace("{group}", group)
                    .replace("{player}", player.getName());

            if (plugin.getMessageManager().isPlaceholderAPIAvailable()) {
                try {
                    Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                    message = (String) papiClass.getMethod("setPlaceholders", Player.class, String.class)
                            .invoke(null, player, message);
                } catch (Exception ignored) {
                }
            }

            return ColorUtil.color(message);
        }

        if (luckPerms == null) {
            return plugin.getMessageManager().getMessage("conditions.luckperms_group.unavailable", player);
        }

        Map<String, String> replacements = new HashMap<>();
        replacements.put("group", group);

        return negate
                ? plugin.getMessageManager().getMessage("conditions.luckperms_group.must_not_have", player, replacements)
                : plugin.getMessageManager().getMessage("conditions.luckperms_group.missing", player, replacements);
    }

    @Override
    public ConditionType getType() {
        return ConditionType.LUCKPERMS_GROUP;
    }
}
