package com.oreo.gui.bottom;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.oreo.gui.BottomInventoryMode;
import com.oreo.gui.BottomInventoryRenderer;
import com.oreo.gui.GuiDefinition;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class PacketEventsBottomInventoryRenderer implements BottomInventoryRenderer {

    @Override
    public void open(Player player, GuiDefinition definition) {
        
    }

    @Override
    public void renderSlot(Player player, int playerSlot, ItemStack item) {
        com.github.retrooper.packetevents.protocol.item.ItemStack peItem =
                item != null
                        ? SpigotConversionUtil.fromBukkitItemStack(item)
                        : com.github.retrooper.packetevents.protocol.item.ItemStack.EMPTY;
        WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(0, 0, playerSlot, peItem);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    @Override
    public void close(Player player, Set<Integer> virtualPlayerSlots) {
        for (int playerSlot : virtualPlayerSlots) {
            ItemStack real = player.getInventory().getItem(playerSlot);
            com.github.retrooper.packetevents.protocol.item.ItemStack peItem =
                    real != null
                            ? SpigotConversionUtil.fromBukkitItemStack(real)
                            : com.github.retrooper.packetevents.protocol.item.ItemStack.EMPTY;
            WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(0, 0, playerSlot, peItem);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        }
    }

    @Override
    public BottomInventoryMode getMode() {
        return BottomInventoryMode.PACKET_EVENT;
    }
}
