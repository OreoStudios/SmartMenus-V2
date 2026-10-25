package com.oreo.items;

import com.oreo.SmartMenus;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;

public class DefaultItemProvider implements ItemProvider {

    private final SmartMenus plugin;

    private Object currencyService;
    private Object warpService;
    private Object economyService;

    public DefaultItemProvider(SmartMenus plugin) {
        this.plugin = plugin;
        reloadHooks();
    }

    public void reloadHooks() {
        Plugin oreo = Bukkit.getPluginManager().getPlugin("OreoEssentials");
        if (oreo == null || !oreo.isEnabled()) {
            currencyService = null;
            warpService = null;
            economyService = null;
            plugin.getLogger().info("OreoEssentials not found (or not enabled). Hooks cleared.");
            return;
        }

        /*
         * Do NOT discover OreoEssentials getters via Class#getMethod/getMethods here.
         *
         * OreoEssentials has optional Vault types in its public method signatures. On a
         * server without Vault, asking the JVM to enumerate/resolve those methods can throw
         * NoClassDefFoundError for net.milkbowl.vault.economy.Economy before we ever reach
         * getCurrencyService() or getWarpService().
         *
         * First prefer Bukkit's service registry. If those services are not registered,
         * read the known OreoEssentials service fields directly. Looking up one field does
         * not force the JVM to resolve every public method signature on the plugin class.
         */
        Object cs = tryBukkitService(
                oreo,
                "fr.elias.oreoEssentials.modules.currency.CurrencyService"
        );
        Object ws = tryBukkitService(
                oreo,
                "fr.elias.oreoEssentials.modules.warps.WarpService"
        );

        if (cs == null) cs = tryField(oreo, "currencyService");
        if (ws == null) ws = tryField(oreo, "warpService");

        // Economy is optional. Resolve Vault only through the Vault plugin's classloader
        // and only when Vault is actually enabled. Missing Vault must be a normal state,
        // not an OreoEssentials hook failure.
        Object es = tryVaultEconomy();

        currencyService = cs;
        warpService = ws;
        economyService = es;

        plugin.getLogger().info("OreoEssentials detected. Hooks: "
                + "Currency=" + (currencyService != null)
                + ", Warps=" + (warpService != null)
                + ", Economy=" + (economyService != null));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object tryBukkitService(Plugin owner, String className) {
        try {
            ClassLoader loader = owner.getClass().getClassLoader();
            Class serviceClass = Class.forName(className, false, loader);
            return Bukkit.getServicesManager().load(serviceClass);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object tryField(Object target, String fieldName) {
        Class<?> type = target.getClass();

        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }

        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object tryVaultEconomy() {
        Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");
        if (vault == null || !vault.isEnabled()) {
            return null;
        }

        try {
            ClassLoader loader = vault.getClass().getClassLoader();
            Class economyClass = Class.forName(
                    "net.milkbowl.vault.economy.Economy",
                    false,
                    loader
            );
            return Bukkit.getServicesManager().load(economyClass);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public boolean hasOreoCurrency() { return currencyService != null; }
    public boolean hasOreoWarps() { return warpService != null; }
    public boolean hasOreoEconomy() { return economyService != null; }

    public Object getCurrencyService() { return currencyService; }
    public Object getWarpService() { return warpService; }
    public Object getEconomyService() { return economyService; }

    @Override
    public ItemStack getItem(String material, String itemType, Integer customModelData) {
        if (itemType == null || itemType.equalsIgnoreCase("vanilla")) {
            return getVanillaItem(material, customModelData);
        } else if (itemType.equalsIgnoreCase("itemsadder")) {
            return getItemsAdderItem(material);
        } else if (itemType.equalsIgnoreCase("nexo")) {
            return getNexoItem(material);
        }

        plugin.getLogger().warning("Unknown item type: " + itemType + ", falling back to vanilla");
        return getVanillaItem(material, customModelData);
    }

    @Override
    public boolean isAvailable(String itemType) {
        if (itemType == null || itemType.equalsIgnoreCase("vanilla")) {
            return true;
        } else if (itemType.equalsIgnoreCase("itemsadder")) {
            return Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;
        } else if (itemType.equalsIgnoreCase("nexo")) {
            return Bukkit.getPluginManager().getPlugin("Nexo") != null;
        }
        return false;
    }

    private ItemStack getVanillaItem(String materialName, Integer customModelData) {
        Material mat = Material.matchMaterial(materialName);
        if (mat == null) {
            plugin.getLogger().warning("Invalid material: " + materialName);
            return new ItemStack(Material.STONE);
        }

        ItemStack item = new ItemStack(mat);

        if (customModelData != null && customModelData > 0) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(customModelData);
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    private ItemStack getItemsAdderItem(String itemId) {
        if (!isAvailable("itemsadder")) {
            plugin.getLogger().warning("ItemsAdder is not available!");
            return new ItemStack(Material.STONE);
        }

        try {
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Object customStack = customStackClass.getMethod("getInstance", String.class).invoke(null, itemId);

            if (customStack == null) {
                plugin.getLogger().warning("ItemsAdder item not found: " + itemId);
                return new ItemStack(Material.STONE);
            }

            return (ItemStack) customStackClass.getMethod("getItemStack").invoke(customStack);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get ItemsAdder item: " + itemId + " - " + e.getMessage());
            return new ItemStack(Material.STONE);
        }
    }

    private ItemStack getNexoItem(String itemId) {
        if (!isAvailable("nexo")) {
            plugin.getLogger().warning("Nexo is not available!");
            return new ItemStack(Material.STONE);
        }

        try {
            Class<?> nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems");
            Object itemBuilder = nexoItemsClass.getMethod("itemFromId", String.class).invoke(null, itemId);

            if (itemBuilder == null) {
                plugin.getLogger().warning("Nexo item not found: " + itemId);
                return new ItemStack(Material.STONE);
            }

            return (ItemStack) itemBuilder.getClass().getMethod("build").invoke(itemBuilder);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get Nexo item: " + itemId + " - " + e.getMessage());
            return new ItemStack(Material.STONE);
        }
    }
}
