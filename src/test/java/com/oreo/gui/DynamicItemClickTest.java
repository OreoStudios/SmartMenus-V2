package com.oreo.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DynamicItemClickTest {
    private Player player(UUID id) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        return player;
    }

    @Test
    void otherPlayerInventoryClicksCannotExecuteActionsOrTransfers() {
        MenuContext context = new MenuContext(player(UUID.randomUUID()), player(UUID.randomUUID()));
        for (DynamicSource source : new DynamicSource[]{DynamicSource.PLAYER_ENDERCHEST, DynamicSource.PLAYER_INVENTORY}) {
            AtomicInteger actions = new AtomicInteger();
            var item = GuiInventoryProvider.dynamicItem(context, source, mock(ItemStack.class), e -> actions.incrementAndGet());
            item.run(mock(InventoryClickEvent.class));
            item.run(mock(InventoryClickEvent.class));
            assertEquals(0, actions.get());
        }
    }

    @Test
    void ownInventoryStillRunsConfiguredClickHandler() {
        UUID id = UUID.randomUUID();
        MenuContext context = new MenuContext(player(id), player(id));
        AtomicInteger actions = new AtomicInteger();
        GuiInventoryProvider.dynamicItem(context, DynamicSource.PLAYER_ENDERCHEST,
                mock(ItemStack.class), e -> actions.incrementAndGet()).run(mock(InventoryClickEvent.class));
        assertEquals(1, actions.get());
    }

    @Test
    void nonInventorySourcesRemainInteractiveWithTarget() {
        MenuContext context = new MenuContext(player(UUID.randomUUID()), player(UUID.randomUUID()));
        AtomicInteger actions = new AtomicInteger();
        GuiInventoryProvider.dynamicItem(context, DynamicSource.ONLINE_PLAYERS,
                mock(ItemStack.class), e -> actions.incrementAndGet()).run(mock(InventoryClickEvent.class));
        assertEquals(1, actions.get());
    }
}
