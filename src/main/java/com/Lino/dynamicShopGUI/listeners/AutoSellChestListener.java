package com.Lino.dynamicShopGUI.listeners;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class AutoSellChestListener implements Listener {

    private final DynamicShopGUI plugin;

    public AutoSellChestListener(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() != Material.CHEST || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore.contains(ChatColor.DARK_GRAY + "autosell-chest")) {
                plugin.getAutoSellChestManager().addChest(event.getBlock().getLocation(), event.getPlayer().getUniqueId());
                event.getPlayer().sendMessage(plugin.getShopConfig().getMessage("autosell.placed"));
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        if (plugin.getAutoSellChestManager().isAutoSellChest(location)) {
            plugin.getAutoSellChestManager().removeChest(location);
            event.getPlayer().sendMessage(plugin.getShopConfig().getMessage("autosell.broken"));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getLocation() == null) {
            return;
        }
        Block block = event.getInventory().getLocation().getBlock();
        if (block.getType() == Material.CHEST && plugin.getAutoSellChestManager().isAutoSellChest(block.getLocation())) {
            plugin.getAutoSellChestManager().startSellTimer(block.getLocation());
        }
    }
}
