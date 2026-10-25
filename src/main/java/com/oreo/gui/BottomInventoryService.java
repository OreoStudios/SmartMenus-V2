package com.oreo.gui;

import com.oreo.SmartMenus;
import com.oreo.gui.bottom.DefaultBottomInventoryRenderer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BottomInventoryService {

    private static BottomInventoryService INSTANCE;

    private final SmartMenus plugin;
    private final BottomInventoryRenderer defaultRenderer;
    private final BottomInventoryRenderer packetRenderer; 
    private final Set<String> warnedGuiIds = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BottomSession> sessions = new ConcurrentHashMap<>();

    public static void init(SmartMenus plugin) {
        INSTANCE = new BottomInventoryService(plugin);
    }

    public static BottomInventoryService getInstance() {
        return INSTANCE;
    }

    private BottomInventoryService(SmartMenus plugin) {
        this.plugin = plugin;
        this.defaultRenderer = new DefaultBottomInventoryRenderer();

        BottomInventoryRenderer pe = null;
        if (Bukkit.getPluginManager().getPlugin("packetevents") != null) {
            try {
                pe = (BottomInventoryRenderer) Class
                        .forName("com.oreo.gui.bottom.PacketEventsBottomInventoryRenderer")
                        .getDeclaredConstructor()
                        .newInstance();
                plugin.getLogger().info("PacketEvents detected — packet inventory rendering available.");
            } catch (Throwable t) {
                plugin.getLogger().warning("PacketEvents found but renderer failed to initialize: " + t.getMessage());
            }
        } else {
            plugin.getLogger().info("PacketEvents not detected — packet inventory rendering unavailable.");
        }
        this.packetRenderer = pe;
    }

    public boolean isPacketEventsAvailable() {
        return packetRenderer != null;
    }

    public void activate(Player player, GuiDefinition definition) {
        UUID id = player.getUniqueId();

        BottomSession prev = sessions.get(id);
        if (prev != null && prev.mode == BottomInventoryMode.DEFAULT) {
            BottomInventoryManager.restoreDefault(player);
        }

        BottomInventoryMode mode = resolveMode(definition);
        BottomInventoryRenderer renderer = mode == BottomInventoryMode.PACKET_EVENT
                ? packetRenderer : defaultRenderer;

        Set<Integer> virtualPlayerSlots = new LinkedHashSet<>();
        for (int screenSlot : definition.getBottomItems().keySet()) {
            int playerSlot = BottomInventoryManager.bottomScreenToPlayerSlot(screenSlot);
            if (playerSlot >= 0) virtualPlayerSlots.add(playerSlot);
        }

        BottomSession session = new BottomSession(definition, mode, renderer, virtualPlayerSlots);
        sessions.put(id, session);

        if (mode == BottomInventoryMode.DEFAULT) {
            BottomInventoryManager.activateDefault(player, definition);
        }

        renderer.open(player, definition);
    }

    public void renderSlot(Player player, int playerSlot, ItemStack item) {
        UUID id = player.getUniqueId();
        BottomSession session = sessions.get(id);
        if (session == null) return;
        session.updateDisplay(playerSlot, item);
        session.renderer.renderSlot(player, playerSlot, item);
    }

    public void refreshVirtualSlots(Player player) {
        UUID id = player.getUniqueId();
        BottomSession session = sessions.get(id);
        if (session == null || session.mode != BottomInventoryMode.PACKET_EVENT) return;
        for (Map.Entry<Integer, ItemStack> e : session.currentDisplay.entrySet()) {
            session.renderer.renderSlot(player, e.getKey(), e.getValue());
        }
    }

    public void close(Player player) {
        UUID id = player.getUniqueId();
        BottomSession session = sessions.remove(id);
        if (session == null) return;

        if (session.mode == BottomInventoryMode.DEFAULT) {
            BottomInventoryManager.clearActiveDefinition(id);
        }

        session.renderer.close(player, session.virtualPlayerSlots);
    }

    public void clearSession(UUID id) {
        BottomSession session = sessions.remove(id);
        if (session != null && session.mode == BottomInventoryMode.DEFAULT) {
            BottomInventoryManager.clearWithoutRestore(id);
        }
    }

    public boolean isActive(UUID id) {
        return sessions.containsKey(id);
    }

    public GuiDefinition getActiveDefinition(UUID id) {
        BottomSession s = sessions.get(id);
        return s != null ? s.definition : null;
    }

    public BottomSession getSession(UUID id) {
        return sessions.get(id);
    }

    private BottomInventoryMode resolveMode(GuiDefinition definition) {
        BottomInventoryMode mode = definition.getBottomInventoryMode();
        if (mode == BottomInventoryMode.PACKET_EVENT && packetRenderer == null) {
            if (warnedGuiIds.add(definition.getId())) {
                plugin.getLogger().warning("GUI '" + definition.getId()
                        + "' requested PACKET_EVENT bottom inventory mode, but PacketEvents is not installed."
                        + " Falling back to DEFAULT.");
            }
            return BottomInventoryMode.DEFAULT;
        }
        return mode;
    }

    public static final class BottomSession {
        public final GuiDefinition definition;
        public final BottomInventoryMode mode;
        public final BottomInventoryRenderer renderer;
        public final Set<Integer> virtualPlayerSlots;
        final Map<Integer, ItemStack> currentDisplay = new ConcurrentHashMap<>();

        BottomSession(GuiDefinition definition, BottomInventoryMode mode,
                      BottomInventoryRenderer renderer, Set<Integer> virtualPlayerSlots) {
            this.definition = definition;
            this.mode = mode;
            this.renderer = renderer;
            this.virtualPlayerSlots = Collections.unmodifiableSet(new LinkedHashSet<>(virtualPlayerSlots));
        }

        void updateDisplay(int playerSlot, ItemStack item) {
            if (item != null) {
                currentDisplay.put(playerSlot, item);
            } else {
                currentDisplay.remove(playerSlot);
            }
        }
    }
}
