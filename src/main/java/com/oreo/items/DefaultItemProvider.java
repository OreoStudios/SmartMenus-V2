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
