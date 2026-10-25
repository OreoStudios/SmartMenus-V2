package com.oreo;

import com.oreo.bedrock.BedrockManager;
import com.oreo.editor.EditorManager;
import com.oreo.api.SmartMenusAPI;
import com.oreo.items.ItemLevelManager;
import com.oreo.util.SmartScheduler;
import com.oreo.command.SmartMenusCommand;
import com.oreo.gui.ArgManager;
import com.oreo.gui.GuiOpener;
import com.oreo.gui.GuiOpener.OpenOptions;
import com.oreo.gui.BottomInventoryService;
import com.oreo.gui.GuiDefinition;
import com.oreo.gui.GuiInventoryProvider;
import com.oreo.gui.GuiRegistry;
import com.oreo.gui.NavigationManager;
import com.oreo.gui.PageManager;
import com.oreo.gui.PatternRegistry;
import com.oreo.items.DefaultItemProvider;
import com.oreo.items.ItemProvider;
import com.oreo.listener.BottomInventoryListener;
import com.oreo.listener.ChatInputListener;
import com.oreo.listener.InputSlotListener;
import com.oreo.listener.NPCInteractListener;
import com.oreo.listener.PlayerDeathListener;
import com.oreo.listener.PlayerQuitListener;
import com.oreo.script.SmartMenusScriptEngine;
import com.oreo.util.MessageManager;
import fr.minuskube.inv.InventoryManager;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;

public class SmartMenus extends JavaPlugin implements Listener {

    private InventoryManager inventoryManager;
    private GuiRegistry guiRegistry;
    private ItemProvider itemProvider;
    private MessageManager messageManager;
    private PatternRegistry patternRegistry;
    private SmartMenusScriptEngine scriptEngine;
    private EditorManager editorManager;
    private BedrockManager bedrockManager;
    private ItemLevelManager itemLevelManager;
    private com.oreo.util.CooldownManager cooldownManager;

    private final List<String> registeredCommands = new ArrayList<>();
    private boolean oreoHooked = false;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        int pluginId = 29355;
        Metrics metrics = new Metrics(this, pluginId);

        metrics.addCustomChart(new SingleLineChart("guis_loaded", () ->
                getGuiRegistry() != null ? getGuiRegistry().getGuiIds().size() : 0));

        metrics.addCustomChart(new SimplePie("hook_oreoessentials", () ->
                (getItemProvider() instanceof DefaultItemProvider dip && dip.hasOreoEconomy()) ? "yes" : "no"));

        printBanner();
        BottomInventoryService.init(this);
        SmartMenusAPI.init(this);
        GuiInventoryProvider.initKeys(this);

        saveDefaultConfig();
        reloadConfig();

        saveResource("lang.yml", false);

        if (getConfig().getBoolean("regenerate-default-guis", true)) {
            saveDefaultGuiFiles();
        }

        setupConverterFolders();

        messageManager = new MessageManager(this);

        cooldownManager = new com.oreo.util.CooldownManager(this);

        scriptEngine = new SmartMenusScriptEngine(this);

        inventoryManager = new InventoryManager(this);
        inventoryManager.init();

        itemProvider = new DefaultItemProvider(this);
        getLogger().info(messageManager.getMessage("loading.item_provider"));

        patternRegistry = new PatternRegistry(this);
        patternRegistry.reload();

        guiRegistry = new GuiRegistry(this);
        guiRegistry.reload();

        itemLevelManager = new ItemLevelManager(this);
        if (getConfig().getBoolean("regenerate-default-guis", true)) {
            saveResource("item_upgrades/sword_upgrade.yml", true);
        }
        itemLevelManager.reload();

        SmartMenusCommand command = new SmartMenusCommand(this);
        if (getCommand("smartmenus") != null) {
            getCommand("smartmenus").setExecutor(command);
            getCommand("smartmenus").setTabCompleter(command);
        }

        registerGuiCommands();
        getLogger().info("Registering GUI commands...");
        registerNPCListener();

        getLogger().info(messageManager.getMessage("loading.enabled"));
        getLogger().info(messageManager.getMessage("loading.oreo_currencies"));
        getLogger().info(messageManager.getMessage("loading.oreo_warps"));
        getLogger().info(messageManager.getMessage("loading.itemsadder"));
        getLogger().info(messageManager.getMessage("loading.nexo"));
        getLogger().info(messageManager.getMessage("loading.worldguard"));
        getLogger().info(messageManager.getMessage("loading.placeholderapi"));
        getLogger().info(messageManager.getMessage("loading.weather_world"));

        if (Bukkit.getPluginManager().getPlugin("ModeledNPCs") != null) {
            getLogger().info(messageManager.getMessage("loading.modelednpcs"));
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(), this);
        Bukkit.getPluginManager().registerEvents(new ChatInputListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BottomInventoryListener(this), this);
        Bukkit.getPluginManager().registerEvents(new InputSlotListener(this), this);

        editorManager = new EditorManager(this);
        Bukkit.getPluginManager().registerEvents(new com.oreo.editor.EditorListener(this, editorManager), this);
        Bukkit.getPluginManager().registerEvents(new com.oreo.editor.EditorChatListener(this, editorManager), this);

        bedrockManager = new BedrockManager(this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        SmartScheduler.runTask(this, this::hookOreoEssentialsIfPresent);
    }

    private void setupConverterFolders() {
        String[] dirs = {
            "converter/commandpanel",
            "converter/deluxemenus",
            "converter/zmenus",
            "guis/converted"
        };
        for (String dir : dirs) {
            java.io.File folder = new java.io.File(getDataFolder(), dir);
            if (!folder.exists()) {
                folder.mkdirs();
            }
        }
    }

    private void saveDefaultGuiFiles() {
        String[] defaults = {

            "guis/vault_shop/vault_shop.yml",

            "guis/xp/xp_shop.yml",
            "guis/xp/xp_points_shop.yml",

            "guis/oreoessentials/gems_shop.yml",
            "guis/oreoessentials/warp_shop.yml",
            "guis/oreoessentials/warp_location_shop.yml",

            "guis/item_trade/item_trade.yml",
            "guis/custom_model_shop/custom_model_shop.yml",

            "guis/itemsadder_shop/itemsadder_shop.yml",
            "guis/nexo_shop/nexo_shop.yml",

            "guis/vip_shop/vip_shop.yml",
            "guis/rank_upgrade/rank_upgrade.yml",

            "guis/placeholder_shop/placeholder_shop.yml",
            "guis/region_shop/region_shop.yml",
            "guis/weather_shop/weather_shop.yml",
            "guis/world_shop/world_shop.yml",

            "guis/modelednpcs/npc_shop.yml",
            "guis/modelednpcs/quest_board.yml",

            "guis/color_demo/color_demo.yml",
            "guis/ultimate_shop/ultimate_shop.yml",
            "guis/item_catalog/item_catalog.yml",
            "guis/toggle_demo/toggle_demo.yml",

            "guis/navigation_demo/main_hub.yml",

            "guis/dynamic_examples/online_players.yml",
            "guis/dynamic_examples/world_selector.yml",
            "guis/dynamic_examples/inv_viewer.yml",
            "guis/dynamic_examples/sacrifice_altar.yml",
            "guis/dynamic_examples/full_screen_demo.yml",

            "guis/feature_showcase/showcase_hub.yml",
            "guis/feature_showcase/showcase_pagination.yml",
            "guis/feature_showcase/showcase_dynamic.yml",
            "guis/feature_showcase/showcase_switch_input.yml",
            "guis/feature_showcase/showcase_advanced_conditions.yml",

            "guis/rewards/reward_menu.yml",
            "guis/rewards/chance_crate.yml",
        };
        for (String path : defaults) {

            saveResource(path, true);
        }
    }

    @Override
    public void onDisable() {

        if (cooldownManager != null) {
            cooldownManager.save();
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            GuiInventoryProvider.clearPlayerCache(id);
            NavigationManager.clearAll(id);
            PageManager.clearPlayer(id);
            ArgManager.clear(id);
            if (BottomInventoryService.getInstance() != null && BottomInventoryService.getInstance().isActive(id)) {
                BottomInventoryService.getInstance().close(p);
            }
        }

        getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");

        editorManager = null;
        inventoryManager = null;
        guiRegistry = null;
        itemProvider = null;
        messageManager = null;
        patternRegistry = null;
        scriptEngine = null;
        bedrockManager = null;
        itemLevelManager = null;
        cooldownManager = null;
        registeredCommands.clear();
        getLogger().info("Smart Menus disabled");
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            GuiInventoryProvider.handleClose(this, player);
        }
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent e) {
        if (e.getPlugin().getName().equalsIgnoreCase("OreoEssentials")) {
            hookOreoEssentialsIfPresent();
        }
    }

    private void printBanner() {
        boolean peDetected = Bukkit.getPluginManager().getPlugin("packetevents") != null;
        String[] lines;
        if (peDetected) {
            lines = new String[]{
                " ",
                "  ┌──────────────────────────────────────────────────────────┐",
                "  │  ███████╗███╗   ███╗ █████╗ ██████╗ ████████╗           │",
                "  │  ██╔════╝████╗ ████║██╔══██╗██╔══██╗╚══██╔══╝           │",
                "  │  ███████╗██╔████╔██║███████║██████╔╝   ██║              │",
                "  │  ╚════██║██║╚██╔╝██║██╔══██║██╔══██╗   ██║              │",
                "  │  ███████║██║ ╚═╝ ██║██║  ██║██║  ██║   ██║              │",
                "  │  ╚══════╝╚═╝     ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝   ╚═╝              │",
                "  │  ███╗   ███╗███████╗███╗   ██╗██╗   ██╗███████╗         │",
                "  │  ████╗ ████║██╔════╝████╗  ██║██║   ██║██╔════╝         │",
                "  │  ██╔████╔██║█████╗  ██╔██╗ ██║██║   ██║███████╗         │",
                "  │  ██║╚██╔╝██║██╔══╝  ██║╚██╗██║██║   ██║╚════██║         │",
                "  │  ██║ ╚═╝ ██║███████╗██║ ╚████║╚██████╔╝███████║         │",
                "  │  ╚═╝     ╚═╝╚══════╝╚═╝  ╚═══╝ ╚═════╝ ╚══════╝         │",
                "  │                                                          │",
                "  │   [+] PacketEvents DETECTED                              │",
                "  │       PACKET_EVENT bottom inventory mode available!      │",
                "  └──────────────────────────────────────────────────────────┘",
                " "
            };
        } else {
            lines = new String[]{
                " ",
                "  ┌──────────────────────────────────────────────────────────┐",
                "  │  ███████╗███╗   ███╗ █████╗ ██████╗ ████████╗           │",
