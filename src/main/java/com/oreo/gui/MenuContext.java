package com.oreo.gui;

import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Carries the distinction between the player who has a menu <b>open</b> (the {@code viewer}) and the
 * player whose data the menu <b>displays</b> (the {@code target}).
 *
 * <p>Historically SmartMenus assumed {@code viewer == data owner}. That assumption still holds for
 * every normal menu — when no target is supplied, {@link #dataOwner()} returns the viewer, so
 * existing behaviour is unchanged. When a target <i>is</i> supplied (e.g.
 * {@code /enderchestview <player>}), data sources such as {@link DynamicSource#PLAYER_ENDERCHEST}
 * resolve against the target instead.</p>
 *
 * <p>A context travels with the open menu as a SmartInvs
 * {@link fr.minuskube.inv.content.InventoryContents} property under {@link #PROPERTY_KEY}, so the
 * provider can read it on every {@code init}/{@code update} without any per-player static state.</p>
 */
public final class MenuContext {

    /** Property key under which the context is stored on a SmartInvs {@code InventoryContents}. */
    public static final String PROPERTY_KEY = "smartmenus:context";

    private final Player viewer;
    private final Player target;
    private final Map<String, String> arguments;

    public MenuContext(Player viewer) {
        this(viewer, null, null);
    }

    public MenuContext(Player viewer, Player target) {
        this(viewer, target, null);
    }

    public MenuContext(Player viewer, Player target, Map<String, String> arguments) {
        this.viewer = viewer;
        this.target = target;
        this.arguments = arguments != null ? new HashMap<>(arguments) : new HashMap<>();
    }

    /** @return the player who has the menu open */
    public Player viewer() {
        return viewer;
    }

    /** @return the explicit target player, or {@code null} if the menu targets the viewer */
    public Player target() {
        return target;
    }

    public boolean hasTarget() {
        return target != null;
    }

    /**
     * @return the player whose data should be displayed: the {@link #target()} if present, otherwise
     *         the {@link #viewer()}. Data sources should read from this player.
     */
    public Player dataOwner() {
        return target != null ? target : viewer;
    }

    public Map<String, String> arguments() {
        return Collections.unmodifiableMap(arguments);
    }
}
