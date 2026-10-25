package com.oreo.gui;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuContextTest {

    @Test
    void viewerOnlyContextOwnsDataItself() {
        Player viewer = Mockito.mock(Player.class);
        MenuContext ctx = new MenuContext(viewer);
        assertSame(viewer, ctx.viewer());
        assertNull(ctx.target());
        assertFalse(ctx.hasTarget());
        assertSame(viewer, ctx.dataOwner(), "with no target the viewer is the data owner");
    }

    @Test
    void targetContextResolvesDataOwnerToTarget() {
        Player viewer = Mockito.mock(Player.class);
        Player target = Mockito.mock(Player.class);
        MenuContext ctx = new MenuContext(viewer, target);
        assertTrue(ctx.hasTarget());
        assertSame(target, ctx.dataOwner());
        assertSame(viewer, ctx.viewer());
    }

    @Test
    void argumentsAreDefensivelyCopiedAndImmutable() {
        Player viewer = Mockito.mock(Player.class);
        java.util.Map<String, String> args = new java.util.HashMap<>();
        args.put("k", "v");
        MenuContext ctx = new MenuContext(viewer, null, args);
        args.put("k2", "v2"); // must not leak into the context
        assertEquals(1, ctx.arguments().size());
        assertEquals("v", ctx.arguments().get("k"));
    }
}
