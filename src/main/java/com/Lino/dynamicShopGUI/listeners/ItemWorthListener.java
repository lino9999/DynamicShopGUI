package com.Lino.dynamicShopGUI.listeners;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ItemWorthListener implements Listener {

    private final DynamicShopGUI plugin;

    public ItemWorthListener(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("item-worth.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.getGameMode() != GameMode.CREATIVE) {
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (!plugin.getConfig().getBoolean("item-worth.enabled", true)) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        GameMode newMode = event.getNewGameMode();

        if (newMode == GameMode.CREATIVE) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        for (ItemStack item : player.getInventory().getContents()) {
                            if (item != null) {
                                removeWorthFromItem(item);
                            }
                        }
                        player.updateInventory();
                    }
                }
            }.runTaskLater(plugin, 1L);
        } else if (player.getGameMode() == GameMode.CREATIVE) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && player.getGameMode() != GameMode.CREATIVE) {
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

    private void removeWorthFromItem(ItemStack item) {
        if (item == null) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return;
        }

        List<String> lore = new ArrayList<>(meta.getLore());
        Iterator<String> iterator = lore.iterator();

        while (iterator.hasNext()) {
            String line = ChatColor.stripColor(iterator.next());
            if (line.startsWith("Worth:") ||
                    line.contains("Not Sellable") ||
                    line.contains("$")) {
                iterator.remove();
            }
        }

        if (lore.isEmpty()) {
            meta.setLore(null);
        } else {
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
    }
}