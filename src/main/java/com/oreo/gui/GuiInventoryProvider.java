package com.oreo.gui;

import com.oreo.SmartMenus;
import com.oreo.action.Action;
import com.oreo.condition.Condition;
import com.oreo.items.ItemProvider;
import com.oreo.util.ColorUtil;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GuiInventoryProvider implements InventoryProvider {

    private final GuiDefinition definition;
    private final SmartMenus plugin;
    private final ItemProvider itemProvider;
    private Economy economy = null;

    private static final Map<UUID, Map<String, Long>> updateTimers = new ConcurrentHashMap<>();

    private static final Map<UUID, Long> tickCounters = new ConcurrentHashMap<>();

    static final Map<UUID, GuiDefinition> activeGuiDefs = new ConcurrentHashMap<>();

    private static final Set<UUID> pendingFullRefresh = ConcurrentHashMap.newKeySet();

    private static final Map<UUID, Map<Integer, ItemStack>> inputSlotPlaceholders = new ConcurrentHashMap<>();

    private static NamespacedKey GUI_ITEM_KEY = null;

    public static void initKeys(SmartMenus plugin) {
        GUI_ITEM_KEY = new NamespacedKey(plugin, "gui_item");
    }

    public static boolean isGuiItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || GUI_ITEM_KEY == null) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(GUI_ITEM_KEY, PersistentDataType.BYTE);
    }

    public static boolean isGuiOpen(UUID playerId) {
        return activeGuiDefs.containsKey(playerId);
    }

    public static GuiDefinition getActiveGui(UUID playerId) {
        return activeGuiDefs.get(playerId);
    }

    public static ItemStack getInputSlotPlaceholder(UUID playerId, int slot) {
        Map<Integer, ItemStack> map = inputSlotPlaceholders.get(playerId);
        return map != null ? map.get(slot) : null;
    }

    public GuiInventoryProvider(GuiDefinition definition, SmartMenus plugin, ItemProvider itemProvider) {
        this.definition = definition;
        this.plugin = plugin;
        this.itemProvider = itemProvider;
        setupEconomy();
    }

    @Deprecated
    public GuiInventoryProvider(GuiDefinition definition) {
        this.definition = definition;
        this.plugin = (SmartMenus) Bukkit.getPluginManager().getPlugin("SmartMenus");
        this.itemProvider = plugin != null ? plugin.getItemProvider() : null;
        setupEconomy();
    }

    private void setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return;
        economy = rsp.getProvider();
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        activeGuiDefs.put(player.getUniqueId(), definition);
        Map<String, String> openVars = buildVars(player);
        for (Action action : definition.getOpenActions()) {
            try { action.execute(plugin, player, openVars); } catch (Exception e) {
                plugin.getLogger().warning("Open action error in GUI " + definition.getId() + ": " + e.getMessage());
            }
        }

        if (definition.getOpenSound() != null) {
            try {
                Sound sound = Sound.valueOf(definition.getOpenSound().toUpperCase());
                player.playSound(player.getLocation(), sound, definition.getOpenSoundVolume(), definition.getOpenSoundPitch());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown open_sound: " + definition.getOpenSound());
            }
        }

        for (GuiItem guiItem : definition.getItems().values()) {
            if (guiItem.getButtonType() == ButtonType.DYNAMIC_PAGINATION) continue;
            renderGuiItem(player, contents, guiItem, buildVars(player));
        }
        renderDynamicItems(player, contents);

        if (definition.isUseBottomInventory()) {
            BottomInventoryService.getInstance().activate(player, definition);
            renderBottomItems(player);
        }

        GuiItem fillItem = definition.getFillItem();
        if (fillItem != null) {
            int totalSlots = definition.getRows() * 9;
            for (int slot = 0; slot < totalSlots; slot++) {
                int row = slot / 9;
                int col = slot % 9;
                if (contents.get(row, col).isEmpty()) {
                    ItemStack fillStack = buildItemStack(fillItem, player);
                    if (fillStack != null) {
                        contents.set(slot, ClickableItem.empty(fillStack));
                    }
                }
            }
        }
    }

    private void renderDynamicItems(Player player, InventoryContents contents) {
        Map<String, List<GuiItem>> groups = new LinkedHashMap<>();
        for (GuiItem guiItem : definition.getItems().values()) {
            if (guiItem.getButtonType() != ButtonType.DYNAMIC_PAGINATION) continue;
            String key = guiItem.getDynamicSource().name() + "|" + guiItem.getMaterial() + "|" + guiItem.getName();
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(guiItem);
        }

        for (List<GuiItem> group : groups.values()) {
            group.sort(Comparator.comparingInt(GuiItem::getSlot));
            GuiItem template = group.get(0);
            List<Map<String, Object>> elements = DynamicSourceProvider.generate(template.getDynamicSource(), player);
            int count = Math.min(group.size(), elements.size());
            for (int i = 0; i < count; i++) {
                GuiItem slotItem = group.get(i);
                Map<String, String> vars = buildVars(player);
                Map<String, Object> element = elements.get(i);
                putElementVars(vars, element);

                ItemStack stack = buildItemStack(template, player, vars, element);
                if (stack == null) continue;

                contents.set(slotItem.getSlot(), ClickableItem.of(stack, e -> {
                    String clickKey = e.getClick().name();
                    executeClickActions(plugin, player, template, clickKey, vars);
                    if (template.isTakeItem()) {
                        removeSourceItem(player, element);
                    }
                    if (template.isGiveItem()) {
                        giveSourceItem(player, element, stack);
                    }
                }));
            }
        }
    }

    private void putElementVars(Map<String, String> vars, Map<String, Object> element) {
        for (Map.Entry<String, Object> entry : element.entrySet()) {
            if (!entry.getKey().startsWith("_") && entry.getValue() != null) {
                vars.put(entry.getKey(), entry.getValue().toString());
            }
        }
    }

    private void removeSourceItem(Player player, Map<String, Object> element) {
        Object type = element.get("_type");
        Object slot = element.get("_slot");
        if (!(slot instanceof Integer)) return;

        int index = (Integer) slot;
        if ("ITEM".equals(type)) {
            player.getInventory().setItem(index, null);
        } else if ("ENDERCHEST_ITEM".equals(type)) {
            player.getEnderChest().setItem(index, null);
        }
    }

    private void giveSourceItem(Player player, Map<String, Object> element, ItemStack displayed) {

        Object raw = element.get("_item");
        ItemStack toGive = (raw instanceof ItemStack) ? ((ItemStack) raw).clone() : displayed.clone();

        Map<Integer, ItemStack> leftOver = player.getInventory().addItem(toGive);
        if (!leftOver.isEmpty()) {

            for (ItemStack drop : leftOver.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    private void renderBottomItems(Player player) {
        BottomInventoryService service = BottomInventoryService.getInstance();
        BottomInventoryService.BottomSession session = service.getSession(player.getUniqueId());
        if (session == null) return;

        Map<String, String> vars = buildVars(player);
        for (Map.Entry<Integer, GuiItem> entry : definition.getBottomItems().entrySet()) {
            int screenSlot = entry.getKey();
            int playerSlot = BottomInventoryManager.bottomScreenToPlayerSlot(screenSlot);
            if (playerSlot < 0) continue;

            GuiItem guiItem = entry.getValue();
            GuiItem resolved = resolveItem(player, guiItem);
            ItemStack stack = resolved != null ? buildItemStack(resolved, player, vars, null) : null;
            service.renderSlot(player, playerSlot, stack);
        }

        if (session.mode == BottomInventoryMode.DEFAULT) {
            player.updateInventory();
        }
    }

    public static void executeBottomItemClick(SmartMenus plugin, Player player, GuiItem guiItem, String clickKey) {
        GuiDefinition activeDef = BottomInventoryService.getInstance().getActiveDefinition(player.getUniqueId());
        if (activeDef == null) return;
        GuiInventoryProvider temp = new GuiInventoryProvider(activeDef, plugin, plugin.getItemProvider());
        Map<String, String> vars = temp.buildVars(player);
        temp.executeClickActions(plugin, player, guiItem, clickKey, vars);
    }

    private GuiItem resolveItem(Player player, GuiItem guiItem) {
        List<Condition> viewReqs = guiItem.getViewRequirements();
        if (viewReqs.isEmpty()) return guiItem;

        for (Condition cond : viewReqs) {
            if (!cond.check(player)) {
                GuiItem elseItem = guiItem.getElseItem();
                if (elseItem == null) return null;
                return resolveItem(player, elseItem);
            }
        }
        return guiItem;
    }

    private void renderGuiItem(Player player, InventoryContents contents, GuiItem guiItem, Map<String, String> vars) {

        GuiItem resolved = resolveItem(player, guiItem);
        if (resolved == null) {
            return;
        }

        ItemStack stack = buildItemStack(resolved, player, vars, null);
        if (stack == null) {
            plugin.getLogger().warning("Failed to create item for slot " + guiItem.getSlot() + " in GUI " + definition.getId());
            return;
        }

        if (resolved.getButtonType() == ButtonType.INPUT) {
            inputSlotPlaceholders
                    .computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                    .put(guiItem.getSlot(), stack);
            contents.set(guiItem.getSlot(), ClickableItem.empty(stack));
            return;
        }

        if (resolved.getButtonType() == ButtonType.CHAT_INPUT) {
            contents.set(guiItem.getSlot(), ClickableItem.of(stack, e -> {
                player.closeInventory();
                List<Action> actions = resolved.getActionsForClick("ANY");
                if (actions.isEmpty()) actions = resolved.getActionsForClick("LEGACY");
                ChatInputManager.await(player.getUniqueId(), new ChatInputManager.PendingInput(
                        definition.getId(),
                        resolved.getChatPromptMessage(),
                        resolved.getChatInputType(),
                        resolved.getChatCancelWord(),
                        actions,
                        new HashMap<>(vars)
                ));
                player.sendMessage(ColorUtil.color(resolved.getChatPromptMessage()));
            }));
            return;
        }

        contents.set(guiItem.getSlot(), ClickableItem.of(stack, e -> {
            String clickKey = e.getClick().name();
            executeClickActions(plugin, player, resolved, clickKey, vars);
        }));
    }

    private ItemStack buildItemStack(GuiItem guiItem, Player player) {
        return buildItemStack(guiItem, player, buildVars(player), null);
    }

    private ItemStack buildItemStack(GuiItem guiItem, Player player, Map<String, String> vars, Map<String, Object> element) {
        ItemStack stack = getItemStack(guiItem, element);
        if (stack == null) return null;
        stack.setAmount(Math.max(1, guiItem.getAmount()));

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {

            if (guiItem.getName() != null && !guiItem.getName().isEmpty()) {
                meta.setDisplayName(ColorUtil.color(replacePlaceholders(guiItem.getName(), player, vars)));
            }

            List<String> lore = guiItem.getLore();
            if (!lore.isEmpty()) {
                List<String> processedLore = new ArrayList<>();
                for (String line : lore) {
                    processedLore.add(ColorUtil.color(replacePlaceholders(line, player, vars)));
                }
                meta.setLore(processedLore);
            }

            if (guiItem.isGlow()) {
                meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            for (String flag : guiItem.getItemFlags()) {
                try {
                    meta.addItemFlags(ItemFlag.valueOf(flag.toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Unknown ItemFlag: " + flag);
                }
            }

            for (Map.Entry<String, Integer> ench : guiItem.getEnchantments().entrySet()) {
                Enchantment e = Enchantment.getByKey(NamespacedKey.minecraft(ench.getKey().toLowerCase()));
                if (e != null) {
                    meta.addEnchant(e, ench.getValue(), true);
                } else {
                    plugin.getLogger().warning("Unknown enchantment: " + ench.getKey());
                }
            }

            if (GUI_ITEM_KEY != null) {
                meta.getPersistentDataContainer().set(GUI_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
            }

            stack.setItemMeta(meta);
        }

        return stack;
    }

    private void executeClickActions(SmartMenus plugin, Player player, GuiItem guiItem, String clickKey, Map<String, String> vars) {

        if (guiItem.hasConditions()) {
            for (Condition condition : guiItem.getConditions()) {
                if (!condition.check(player)) {
                    player.sendMessage(condition.getErrorMessage(player));
                    player.closeInventory();
                    return;
                }
            }
        } else {
            if (!checkLegacyRequirementsCheckOnly(player, guiItem)) return;
        }

        List<Action> actions = new ArrayList<>();
        Map<String, List<Action>> clickActions = guiItem.getClickActions();

        if (clickActions.containsKey(clickKey)) {
            actions = clickActions.get(clickKey);
        } else if (clickActions.containsKey("ANY")) {
            actions = clickActions.get("ANY");
        } else if (clickActions.containsKey("LEGACY")) {
            actions = clickActions.get("LEGACY");
        } else {

            for (String cmd : guiItem.getCommands()) {
                String resolved = cmd.replace("{player}", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
            }
        }

        Map<String, String> execVars = new HashMap<>(vars);
        execVars.put("player", player.getName());

        Map<String, String> argVars = ArgManager.getAll(player.getUniqueId());
        for (Map.Entry<String, String> e : argVars.entrySet()) {
            execVars.put("arg_" + e.getKey(), e.getValue());
        }

        for (Action action : actions) {
            try {
                action.execute(plugin, player, execVars);
            } catch (Exception ex) {
                plugin.getLogger().warning("Action failed: " + ex.getMessage());
            }
        }

        if (guiItem.isAutoUpdate()) {
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    pendingFullRefresh.add(player.getUniqueId()), 5L);
        }

        if (guiItem.hasConditions()) {
            for (Condition condition : guiItem.getConditions()) {
                if (!condition.take(player)) {
                    player.sendMessage(ColorUtil.color("&c✖ Paiement impossible. Transaction annulée."));
                    player.closeInventory();
                    return;
                }
            }
        } else {
            checkLegacyRequirementsConsumeOnly(player, guiItem);
        }

        if (guiItem.isCloseOnClick()) {
            player.closeInventory();
        }
    }

    private Map<String, String> buildVars(Player player) {
        Map<String, String> vars = new HashMap<>();
        vars.put("player", player.getName());

        Map<String, String> argVars = ArgManager.getAll(player.getUniqueId());
        for (Map.Entry<String, String> e : argVars.entrySet()) {
            vars.put("arg_" + e.getKey(), e.getValue());
        }
        return vars;
    }

    private String replacePlaceholders(String text, Player player) {
        return replacePlaceholders(text, player, buildVars(player));
    }

    private String replacePlaceholders(String text, Player player, Map<String, String> vars) {
        if (text == null) return "";
        text = text.replace("{player}", player.getName());

        for (Map.Entry<String, String> e : vars.entrySet()) {
            text = text.replace("%" + e.getKey() + "%", e.getValue() != null ? e.getValue() : "");
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                text = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
            } catch (NoClassDefFoundError | RuntimeException ignored) {

            }
        }

        return text;
    }

    private ItemStack getItemStack(GuiItem guiItem) {
        return getItemStack(guiItem, null);
    }

    private ItemStack getItemStack(GuiItem guiItem, Map<String, Object> element) {
        if (element != null) {
            ItemStack special = DynamicSourceProvider.resolveSpecialItem(guiItem.getMaterial(), element);
            if (special != null) return special;
        }

        if ("headdatabase".equalsIgnoreCase(guiItem.getItemType())) {
            ItemStack hdbItem = getHeadDatabaseItem(guiItem.getItemId());
            if (hdbItem != null) return hdbItem;

        }

        if (itemProvider != null) {
            ItemStack stack = itemProvider.getItem(
                    guiItem.getMaterial(),
                    guiItem.getItemType(),
                    guiItem.getCustomModelData()
            );
            if (stack != null) return stack;
        }

        Material material = Material.matchMaterial(guiItem.getMaterial());
        if (material == null) {
            plugin.getLogger().warning("Invalid material: " + guiItem.getMaterial());
            material = Material.STONE;
        }

        ItemStack stack = new ItemStack(material);
        if (guiItem.getCustomModelData() != null) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(guiItem.getCustomModelData());
                stack.setItemMeta(meta);
            }
        }
        return stack;
    }

    private ItemStack getHeadDatabaseItem(String itemId) {
        if (itemId == null) return null;
        try {
            if (Bukkit.getPluginManager().getPlugin("HeadDatabase") == null) return null;
            Class<?> apiClass = Class.forName("me.arcaniax.hdb.api.HeadDatabaseAPI");
            Object api = apiClass.getDeclaredConstructor().newInstance();
            return (ItemStack) apiClass.getMethod("getItemHead", String.class).invoke(api, itemId);
        } catch (Exception e) {
            plugin.getLogger().warning("HeadDatabase item not found: " + itemId + " - " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private boolean checkLegacyRequirementsCheckOnly(Player player, GuiItem guiItem) {
        if (guiItem.hasPrice() && economy != null) {
            double balance = economy.getBalance(player);
            if (balance < guiItem.getPrice()) {
                player.sendMessage(ColorUtil.color("&c✖ You don't have enough money!"));
                player.sendMessage(ColorUtil.color("&7Need: &f$" + String.format("%.2f", guiItem.getPrice())));
                player.sendMessage(ColorUtil.color("&7You have: &f$" + String.format("%.2f", balance)));
                player.closeInventory();
                return false;
            }
        }
        if (guiItem.hasRequirement()) {
            if (!player.hasPermission(guiItem.getRequirement())) {
                player.sendMessage(ColorUtil.color("&c✖ You don't have permission to purchase this item!"));
                player.sendMessage(ColorUtil.color("&7Required: &f" + guiItem.getRequirement()));
                player.closeInventory();
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean checkLegacyRequirementsConsumeOnly(Player player, GuiItem guiItem) {
        if (guiItem.hasPrice() && economy != null) {
            var response = economy.withdrawPlayer(player, guiItem.getPrice());
            if (!response.transactionSuccess()) {
                player.sendMessage(ColorUtil.color("&c✖ Payment failed: " + response.errorMessage));
                player.closeInventory();
                return false;
            }
            player.sendMessage(ColorUtil.color("&a✔ Purchased for &f$" + String.format("%.2f", guiItem.getPrice())));
        }
        return true;
    }

    public static void clearPlayerCache(UUID playerId) {
        updateTimers.remove(playerId);
        tickCounters.remove(playerId);
        activeGuiDefs.remove(playerId);
        inputSlotPlaceholders.remove(playerId);
    }

    public static void handleClose(SmartMenus plugin, Player player) {
        GuiDefinition def = activeGuiDefs.remove(player.getUniqueId());
        if (def == null || def.getCloseActions().isEmpty()) return;
        Map<String, String> vars = new java.util.HashMap<>();
        vars.put("player", player.getName());
        for (Action action : def.getCloseActions()) {
            try { action.execute(plugin, player, vars); } catch (Exception e) {
                plugin.getLogger().warning("Close action error in GUI " + def.getId() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void update(Player player, InventoryContents contents) {
        UUID playerId = player.getUniqueId();
        long currentTick = tickCounters.merge(playerId, 1L, Long::sum);
        Map<String, Long> timers = updateTimers.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());

        if (pendingFullRefresh.remove(playerId)) {
            for (GuiItem guiItem : definition.getItems().values()) {
                if (guiItem.getButtonType() == ButtonType.DYNAMIC_PAGINATION) continue;
                renderGuiItem(player, contents, guiItem, buildVars(player));
            }
            return;
        }

        for (GuiItem guiItem : definition.getItems().values()) {

            if (guiItem.isPermanent()) continue;

            GuiItem resolved = resolveItem(player, guiItem);
            if (resolved == null || !resolved.isAutoUpdate()) continue;

            String timerKey = definition.getId() + "_update_" + guiItem.getSlot();
            long lastUpdate = timers.getOrDefault(timerKey, 0L);

            if (currentTick - lastUpdate >= resolved.getUpdateInterval()) {
                timers.put(timerKey, currentTick);

                renderGuiItem(player, contents, guiItem, buildVars(player));
            }
        }

        if (definition.isUseBottomInventory() && BottomInventoryService.getInstance().isActive(playerId)) {
            BottomInventoryService.BottomSession bSession = BottomInventoryService.getInstance().getSession(playerId);
            if (bSession != null) {
                for (Map.Entry<Integer, GuiItem> entry : definition.getBottomItems().entrySet()) {
                    GuiItem guiItem = entry.getValue();
                    if (guiItem.isPermanent()) continue;
                    GuiItem resolved = resolveItem(player, guiItem);
                    if (resolved == null || !resolved.isAutoUpdate()) continue;
                    String timerKey = definition.getId() + "_bottom_" + entry.getKey();
                    long lastUpdate = timers.getOrDefault(timerKey, 0L);
                    if (currentTick - lastUpdate < resolved.getUpdateInterval()) continue;
                    timers.put(timerKey, currentTick);
                    int playerSlot = BottomInventoryManager.bottomScreenToPlayerSlot(entry.getKey());
                    if (playerSlot < 0) continue;
                    ItemStack stack = buildItemStack(resolved, player);
                    BottomInventoryService.getInstance().renderSlot(player, playerSlot, stack);
                }
            }
        }
    }
}
