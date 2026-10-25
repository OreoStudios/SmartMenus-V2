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
                "  │   [-] PacketEvents NOT detected                          │",
                "  │       Only DEFAULT bottom inventory mode available.      │",
                "  └──────────────────────────────────────────────────────────┘",
                " "
            };
        }
        for (String line : lines) {
            getLogger().info(line);
        }
    }

    private void hookOreoEssentialsIfPresent() {
        if (oreoHooked) return;

        if (Bukkit.getPluginManager().getPlugin("OreoEssentials") == null) {
            return;
        }

        oreoHooked = true;
        getLogger().info("Detected OreoEssentials. Enabling Oreo hooks...");

        if (itemProvider instanceof DefaultItemProvider) {
            ((DefaultItemProvider) itemProvider).reloadHooks();
        }

        guiRegistry.reload();
        unregisterGuiCommands();
        registeredCommands.clear();
        registerGuiCommands();
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public GuiRegistry getGuiRegistry() {
        return guiRegistry;
    }

    public ItemProvider getItemProvider() {
        return itemProvider;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public PatternRegistry getPatternRegistry() {
        return patternRegistry;
    }

    public SmartMenusScriptEngine getScriptEngine() {
        return scriptEngine;
    }

    public EditorManager getEditorManager() { return editorManager; }
    public BedrockManager getBedrockManager() { return bedrockManager; }
    public ItemLevelManager getItemLevelManager() { return itemLevelManager; }
    public com.oreo.util.CooldownManager getCooldownManager() { return cooldownManager; }

    public void reloadGuis() {
        messageManager.reload();

        if (itemProvider instanceof DefaultItemProvider) {
            ((DefaultItemProvider) itemProvider).reloadHooks();
        }

        patternRegistry.reload();

        unregisterGuiCommands();
        registeredCommands.clear();

        guiRegistry.reload();

        if (itemLevelManager != null) itemLevelManager.reload();

        registerGuiCommands();

        Map<String, String> replacements = new HashMap<>();
        replacements.put("count", String.valueOf(guiRegistry.getGuiIds().size()));
        getLogger().info(messageManager.getMessage("general.reload.success"));
        getLogger().info(messageManager.getMessage("general.reload.gui_count", replacements));
    }

    private void registerNPCListener() {
        if (Bukkit.getPluginManager().getPlugin("ModeledNPCs") == null) {
            return;
        }

        try {
            getServer().getPluginManager().registerEvents(new NPCInteractListener(this), this);

            int npcBoundGuis = 0;
            for (String guiId : guiRegistry.getGuiIds()) {
                GuiDefinition definition = guiRegistry.getGui(guiId);
                if (definition != null && definition.hasNpcBinding()) {
                    npcBoundGuis++;
                }
            }

            if (npcBoundGuis > 0) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("count", String.valueOf(npcBoundGuis));
                getLogger().info(messageManager.getMessage("loading.npc_listener", replacements));
            }
        } catch (Exception e) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("error", e.getMessage());
            getLogger().warning(messageManager.getMessage("errors.npc_listener_register_failed", replacements));
        }
    }

    private void registerGuiCommands() {
        try {
            CommandMap commandMap = getCommandMap();
            Map<String, Command> knownCommands = getKnownCommands(commandMap);

            for (String guiId : guiRegistry.getGuiIds()) {
                GuiDefinition definition = guiRegistry.getGui(guiId);
                if (definition == null || definition.getCommands().isEmpty()) continue;

                List<String> normalized = normalizeCommands(definition.getCommands());
                if (normalized.isEmpty()) continue;

                List<String> safeNames = new ArrayList<>();
                for (String name : normalized) {
                    String key = name.toLowerCase(Locale.ENGLISH);
                    Command existing = knownCommands.get(key);
                    if (existing != null && !knownCommands.containsKey("smartmenus:" + key)) {
                        getLogger().warning("Skipping command '/" + name
                                + "' for GUI '" + guiId
                                + "' — already registered by another plugin and will NOT be overridden.");
                        continue;
                    }
                    safeNames.add(name);
                }

                if (safeNames.isEmpty()) {
                    getLogger().warning("GUI '" + guiId
                            + "' has no available command names — all aliases conflict with other plugins.");
                    continue;
                }

                String primary = safeNames.get(0);
                List<String> aliases = safeNames.size() > 1
                        ? new ArrayList<>(safeNames.subList(1, safeNames.size()))
                        : Collections.emptyList();

                Command cmd = new Command(primary) {
                    @Override
                    public boolean execute(CommandSender sender, String label, String[] args) {
                        if (!(sender instanceof Player)) {
                            messageManager.send(sender, "general.player_only");
                            return true;
                        }

                        Player player = (Player) sender;

                        if (!player.hasPermission("smartmenus.command." + guiId)
                                && !player.hasPermission("ogui.command." + guiId)) {
                            messageManager.send(player, "general.no_permission");
                            return true;
                        }

                        GuiOpener.open(SmartMenus.this, player, definition, OpenOptions.checked());
                        return true;
                    }
                };

                cmd.setAliases(new ArrayList<>(aliases));
                cmd.setDescription("Opens the " + definition.getTitle() + " menu");
                cmd.setPermission("smartmenus.command." + guiId);

                forceUnregisterNames(knownCommands, primary, aliases);

                cmd.register(commandMap);

                knownCommands.put(primary.toLowerCase(Locale.ENGLISH), cmd);
                knownCommands.put(("smartmenus:" + primary).toLowerCase(Locale.ENGLISH), cmd);
                registeredCommands.add(primary);

                for (String a : aliases) {
                    knownCommands.put(a.toLowerCase(Locale.ENGLISH), cmd);
                    knownCommands.put(("smartmenus:" + a).toLowerCase(Locale.ENGLISH), cmd);
                    registeredCommands.add(a);
                }

                Map<String, String> replacements = new HashMap<>();
                replacements.put("command", primary + (aliases.isEmpty() ? "" : " (aliases: " + String.join(", ", aliases) + ")"));
                replacements.put("gui", guiId);
                getLogger().info(messageManager.getMessage("loading.command_registered", replacements));
            }

        } catch (Exception e) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("error", e.getMessage());
            getLogger().log(Level.SEVERE, messageManager.getMessage("errors.command_register_failed", replacements), e);
        }
    }

    private void unregisterGuiCommands() {
        try {
            CommandMap commandMap = getCommandMap();
            Map<String, Command> knownCommands = getKnownCommands(commandMap);

            for (String cmdName : registeredCommands) {
                String key = cmdName.toLowerCase(Locale.ENGLISH);
                knownCommands.remove(key);
                knownCommands.remove(("smartmenus:" + key).toLowerCase(Locale.ENGLISH));

                knownCommands.entrySet().removeIf(e ->
                        e.getKey() != null &&
                                (e.getKey().equalsIgnoreCase(cmdName) ||
                                        e.getKey().equalsIgnoreCase("smartmenus:" + cmdName)));
            }

        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to unregister GUI commands: " + e.getMessage(), e);
        }
    }

    private CommandMap getCommandMap() throws Exception {
        Class<?> clazz = getServer().getClass();
        while (clazz != null) {
            try {
                Field commandMapField = clazz.getDeclaredField("commandMap");
                commandMapField.setAccessible(true);
                return (CommandMap) commandMapField.get(getServer());
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new Exception("Could not find commandMap field in server class hierarchy");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Command> getKnownCommands(CommandMap commandMap) throws Exception {
        Class<?> clazz = commandMap.getClass();
        while (clazz != null) {
            try {
                Field knownCommandsField = clazz.getDeclaredField("knownCommands");
                knownCommandsField.setAccessible(true);
                return (Map<String, Command>) knownCommandsField.get(commandMap);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new Exception("Could not find knownCommands field in CommandMap hierarchy");
    }

    private List<String> normalizeCommands(List<String> raw) {
        List<String> out = new ArrayList<>();
        for (String s : raw) {
            if (s == null) continue;
            String v = s.trim();
            if (v.isEmpty()) continue;
            if (v.startsWith("/")) v = v.substring(1);
            v = v.toLowerCase(Locale.ENGLISH);
            v = v.replaceAll("[^\\p{L}0-9_:\\-]", "");
            if (!v.isEmpty()) out.add(v);
        }
        LinkedHashSet<String> set = new LinkedHashSet<>(out);
        return new ArrayList<>(set);
    }

    private void forceUnregisterNames(Map<String, Command> knownCommands, String primary, List<String> aliases) {
        List<String> all = new ArrayList<>();
        all.add(primary);
        all.addAll(aliases);

        for (String name : all) {
            String key = name.toLowerCase(Locale.ENGLISH);

            knownCommands.remove(key);
            knownCommands.remove(("smartmenus:" + key).toLowerCase(Locale.ENGLISH));

            knownCommands.entrySet().removeIf(e ->
                    e.getKey() != null &&
                            (e.getKey().equalsIgnoreCase(name) ||
                                    e.getKey().equalsIgnoreCase("smartmenus:" + name)));
        }
    }
}
