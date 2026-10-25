package com.oreo.items;

import com.oreo.SmartMenus;
import com.oreo.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;

public class ItemLevelManager {

    public static final String KEY_ID    = "item_upgrade_id";
    public static final String KEY_LEVEL = "item_upgrade_level";

    private final SmartMenus plugin;
    private final NamespacedKey keyId;
    private final NamespacedKey keyLevel;

    private final Map<String, TreeMap<Integer, ItemStack>> registry = new LinkedHashMap<>();

    public ItemLevelManager(SmartMenus plugin) {
        this.plugin = plugin;
        this.keyId    = new NamespacedKey(plugin, KEY_ID);
        this.keyLevel = new NamespacedKey(plugin, KEY_LEVEL);
    }

    public void reload() {
        registry.clear();
        File dir = new File(plugin.getDataFolder(), "item_upgrades");
        if (!dir.exists()) {
            dir.mkdirs();
            return;
        }
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            for (String itemId : cfg.getKeys(false)) {
                ConfigurationSection itemSec = cfg.getConfigurationSection(itemId);
                if (itemSec == null) continue;
                ConfigurationSection levelsSec = itemSec.getConfigurationSection("levels");
                if (levelsSec == null) continue;
                TreeMap<Integer, ItemStack> levels = new TreeMap<>();
                for (String lvlStr : levelsSec.getKeys(false)) {
                    try {
                        int lvl = Integer.parseInt(lvlStr);
                        ConfigurationSection lvlSec = levelsSec.getConfigurationSection(lvlStr);
                        if (lvlSec == null) continue;
                        ItemStack built = buildItem(itemId, lvl, lvlSec);
                        if (built != null) levels.put(lvl, built);
                    } catch (NumberFormatException ignored) {}
                }
                if (!levels.isEmpty()) registry.put(itemId, levels);
            }
        }
        plugin.getLogger().info("Loaded " + registry.size() + " item upgrade(s).");
    }

    private ItemStack buildItem(String itemId, int level, ConfigurationSection sec) {
        String matName = sec.getString("material", "STONE");
        Material mat;
        try {
            mat = Material.valueOf(matName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("ItemUpgrade '" + itemId + "' level " + level
                    + ": unknown material '" + matName + "' — skipped.");
            return null;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = sec.getString("name");
        if (name != null) meta.setDisplayName(ColorUtil.color(name));

        List<String> lore = sec.getStringList("lore");
        if (!lore.isEmpty()) {
            List<String> colored = new ArrayList<>();
            for (String line : lore) colored.add(ColorUtil.color(line));
            meta.setLore(colored);
        }

        if (sec.contains("custom_model_data")) meta.setCustomModelData(sec.getInt("custom_model_data"));

        boolean glow = sec.getBoolean("glow", false);
        ConfigurationSection enchSec = sec.getConfigurationSection("enchantments");
        if (enchSec != null) {
            for (String key : enchSec.getKeys(false)) {
                Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(key.toLowerCase()));
                if (ench == null) ench = Enchantment.getByName(key.toUpperCase());
                if (ench != null) meta.addEnchant(ench, enchSec.getInt(key), true);
            }
        } else if (glow) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        for (String flagName : sec.getStringList("item_flags")) {
            try { meta.addItemFlags(ItemFlag.valueOf(flagName.toUpperCase())); }
            catch (IllegalArgumentException ignored) {}
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyId, PersistentDataType.STRING, itemId);
        pdc.set(keyLevel, PersistentDataType.INTEGER, level);

        item.setItemMeta(meta);
        return item;
    }

    public boolean give(Player player, String itemId, int level) {
        ItemStack item = getItem(itemId, level);
        if (item == null) return false;
        player.getInventory().addItem(item.clone());
        return true;
    }

    public boolean set(Player player, String itemId, int level) {
        ItemStack newItem = getItem(itemId, level);
        if (newItem == null) return false;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (itemId.equals(getItemId(contents[i]))) {
                player.getInventory().setItem(i, newItem.clone());
                return true;
            }
        }
        player.getInventory().addItem(newItem.clone());
        return false;
    }

    public ItemStack getItem(String itemId, int level) {
        TreeMap<Integer, ItemStack> levels = registry.get(itemId);
        return levels == null ? null : levels.get(level);
    }

    public String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(keyId, PersistentDataType.STRING);
    }

    public int getItemLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return -1;
        Integer lvl = item.getItemMeta().getPersistentDataContainer().get(keyLevel, PersistentDataType.INTEGER);
        return lvl != null ? lvl : -1;
    }

    public int getMaxLevel(String itemId) {
        TreeMap<Integer, ItemStack> levels = registry.get(itemId);
        return (levels == null || levels.isEmpty()) ? -1 : levels.lastKey();
    }

    public int getMinLevel(String itemId) {
        TreeMap<Integer, ItemStack> levels = registry.get(itemId);
        return (levels == null || levels.isEmpty()) ? -1 : levels.firstKey();
    }

    public Set<String> getItemIds() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    public boolean hasItem(String itemId) {
        return registry.containsKey(itemId);
    }

    public Set<Integer> getLevels(String itemId) {
        TreeMap<Integer, ItemStack> levels = registry.get(itemId);
        return levels == null ? Collections.emptySet() : Collections.unmodifiableSet(levels.keySet());
    }
}
