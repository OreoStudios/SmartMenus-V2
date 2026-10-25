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
