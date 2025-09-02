package com.Lino.dynamicShopGUI.listeners;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class ItemWorthListener implements Listener {

    private final DynamicShopGUI plugin;

    public ItemWorthListener(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!plugin.getConfig().getBoolean("item-worth.enabled", true)) {
            return;
        }

        if (event.getEntity() instanceof Player) {
            ItemStack item = event.getItem().getItemStack();
            plugin.getItemWorthManager().updateSingleItem(item);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("item-worth.enabled", true)) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                Player player = event.getPlayer();
                if (player.isOnline()) {
                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item != null) {
                            plugin.getItemWorthManager().updateSingleItem(item);
                        }
                    }
                    player.updateInventory();
                }
            }
        }.runTaskLater(plugin, 20L);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!plugin.getConfig().getBoolean("item-worth.enabled", true)) {
            return;
        }

        String title = event.getView().getTitle();
        if (title.contains("Shop") || title.contains("Buy") || title.contains("Sell")) {
            return;
        }

        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        for (ItemStack item : player.getInventory().getContents()) {
                            if (item != null) {
                                plugin.getItemWorthManager().updateSingleItem(item);
                            }
                        }
                        player.updateInventory();
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }
}