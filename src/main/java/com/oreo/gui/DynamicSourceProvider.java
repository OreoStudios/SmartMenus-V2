package com.oreo.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class DynamicSourceProvider {

    private DynamicSourceProvider() {}

    public static List<Map<String, Object>> generate(DynamicSource source, Player viewer) {
        switch (source) {
            case ONLINE_PLAYERS:   return generateOnlinePlayers(viewer);
            case WORLDS:           return generateWorlds();
            case PLAYER_INVENTORY: return generatePlayerInventory(viewer);
            case PLAYER_ENDERCHEST:return generatePlayerEnderChest(viewer);
            case PERMISSION_GROUPS:return generatePermissionGroups();
            default:               return Collections.emptyList();
        }
    }

    private static List<Map<String, Object>> generateOnlinePlayers(Player viewer) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Map<String, Object> el = new LinkedHashMap<>();
            el.put("player_name",     p.getName());
            el.put("player_uuid",     p.getUniqueId().toString());
            el.put("player_world",    p.getWorld().getName());
            el.put("player_gamemode", p.getGameMode().name());
            el.put("player_health",   String.format("%.1f", p.getHealth()));
            el.put("player_max_health", String.format("%.1f", p.getMaxHealth()));
            el.put("player_level",    String.valueOf(p.getLevel()));
            el.put("player_ping",     getPing(p));
            el.put("player_is_self",  String.valueOf(p.getUniqueId().equals(viewer.getUniqueId())));

            el.put("_type",   "PLAYER");
            el.put("_player", p);
            result.add(el);
        }
        return result;
    }

    private static String getPing(Player p) {
        try {
            return String.valueOf(p.getPing());
        } catch (NoSuchMethodError | AbstractMethodError e) {
            return "0";
        }
    }

    private static List<Map<String, Object>> generateWorlds() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (World w : Bukkit.getWorlds()) {
            Map<String, Object> el = new LinkedHashMap<>();
            el.put("world_name",    w.getName());
            el.put("world_type",    w.getEnvironment().name());
            el.put("world_players", String.valueOf(w.getPlayers().size()));
            el.put("world_time",    String.valueOf(w.getTime()));
            el.put("world_seed",    String.valueOf(w.getSeed()));
            el.put("world_difficulty", w.getDifficulty().name());
            el.put("_type",  "WORLD");
            el.put("_world", w);
            result.add(el);
        }
        return result;
    }

    private static List<Map<String, Object>> generatePlayerInventory(Player player) {
        return buildInventoryElements(player.getInventory().getContents(), "ITEM", player);
    }

    private static List<Map<String, Object>> generatePlayerEnderChest(Player player) {
        return buildInventoryElements(player.getEnderChest().getContents(), "ENDERCHEST_ITEM", player);
    }

    private static List<Map<String, Object>> buildInventoryElements(
            ItemStack[] contents, String type, Player player) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() == Material.AIR) continue;

            Map<String, Object> el = new LinkedHashMap<>();
            el.put("item_material", stack.getType().name());
            el.put("item_amount",   String.valueOf(stack.getAmount()));
            el.put("item_slot",     String.valueOf(i));
            el.put("item_name",     getItemDisplayName(stack));
            el.put("_type",  type);
            el.put("_item",  stack.clone());
            el.put("_slot",  i);
            result.add(el);
        }
        return result;
    }

    private static List<Map<String, Object>> generatePermissionGroups() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return Collections.emptyList();
        }
        try {
            net.luckperms.api.LuckPerms lp = net.luckperms.api.LuckPermsProvider.get();
            List<Map<String, Object>> result = new ArrayList<>();
            for (net.luckperms.api.model.group.Group g : lp.getGroupManager().getLoadedGroups()) {
                Map<String, Object> el = new LinkedHashMap<>();
                el.put("group_name",   g.getName());
                el.put("group_weight", String.valueOf(g.getWeight().orElse(0)));
                el.put("_type", "GROUP");
                result.add(el);
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static String getItemDisplayName(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return meta.getDisplayName();
        return stack.getType().name().replace("_", " ");
    }

    public static ItemStack resolveSpecialItem(String templateMaterial, Map<String, Object> element) {
        String type = (String) element.getOrDefault("_type", "");

        if (("ITEM".equals(type) || "ENDERCHEST_ITEM".equals(type)) && element.containsKey("_item")) {
            return ((ItemStack) element.get("_item")).clone();
        }

        if ("PLAYER".equals(type) && "PLAYER_HEAD".equalsIgnoreCase(templateMaterial)
                && element.containsKey("_player")) {
            Player target = (Player) element.get("_player");
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            org.bukkit.inventory.meta.SkullMeta meta =
                    (org.bukkit.inventory.meta.SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(target);
                skull.setItemMeta(meta);
            }
            return skull;
        }

        if ("WORLD".equals(type) && "AUTO".equalsIgnoreCase(templateMaterial)
                && element.containsKey("_world")) {
            World world = (World) element.get("_world");
            return new ItemStack(worldToMaterial(world));
        }

        return null;
    }

    private static Material worldToMaterial(World world) {
        switch (world.getEnvironment()) {
            case NETHER:  return Material.NETHERRACK;
            case THE_END: return Material.END_STONE;
            default:      return Material.GRASS_BLOCK;
        }
    }
}
