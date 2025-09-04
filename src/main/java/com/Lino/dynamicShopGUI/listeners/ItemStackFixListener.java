package com.Lino.dynamicShopGUI.listeners;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;

import java.util.*;

public class ItemStackFixListener implements Listener {

    private final DynamicShopGUI plugin;
    private final Set<UUID> processingInventory = new HashSet<>();

    public ItemStackFixListener(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("item-worth.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                removeWorthFromItem(item);
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.getGameMode() != GameMode.CREATIVE) {
                    for (int i = 0; i < player.getInventory().getSize(); i++) {
                        ItemStack item = player.getInventory().getItem(i);
                        if (item != null && item.getType() != Material.AIR) {
                            plugin.getItemWorthManager().updateSingleItem(item);
                        }
                    }
                    player.updateInventory();
                }
            }
        }.runTaskLater(plugin, 2L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!plugin.getConfig().getBoolean("item-worth.enabled", true)) {
            return;
        }

        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ItemStack pickedUp = event.getItem().getItemStack();
        Material pickedMaterial = pickedUp.getType();

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack invItem = player.getInventory().getItem(i);
            if (invItem != null && invItem.getType() == pickedMaterial) {
                removeWorthFromItem(invItem);
            }
        }

        removeWorthFromItem(pickedUp);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.getGameMode() != GameMode.CREATIVE) {
                    for (int i = 0; i < player.getInventory().getSize(); i++) {
                        ItemStack item = player.getInventory().getItem(i);
                        if (item != null && item.getType() == pickedMaterial) {
                            plugin.getItemWorthManager().updateSingleItem(item);
                        }
                    }
                    player.updateInventory();
                }
            }
        }.runTaskLater(plugin, 2L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.getConfig().getBoolean("item-worth.enabled", true)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        String title = event.getView().getTitle();
        if (title.contains("Shop") || title.contains("Buy") || title.contains("Sell")) {
            return;
        }

        if (event.getClick() == ClickType.NUMBER_KEY ||
                event.getClick() == ClickType.DROP ||
                event.getClick() == ClickType.CONTROL_DROP) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        boolean needsProcessing = false;

        if (clicked != null && cursor != null &&
                clicked.getType() != Material.AIR &&
                cursor.getType() != Material.AIR &&
                clicked.getType() == cursor.getType()) {
            needsProcessing = true;
        }

        if (event.isShiftClick() && clicked != null && clicked.getType() != Material.AIR) {
            needsProcessing = true;
        }

        if (needsProcessing) {
            processingInventory.add(player.getUniqueId());

            removeWorthFromInventorySlots(player);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && player.getGameMode() != GameMode.CREATIVE) {
                        processingInventory.remove(player.getUniqueId());
                        reapplyWorthToInventorySlots(player);
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && cursor.getType() != Material.AIR) {
            removeWorthFromItem(cursor);
        }

        if (processingInventory.contains(player.getUniqueId())) {
            processingInventory.remove(player.getUniqueId());

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && player.getGameMode() != GameMode.CREATIVE) {
                        reapplyWorthToInventorySlots(player);
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!plugin.getConfig().getBoolean("item-worth.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ItemStack item = event.getItemDrop().getItemStack();
        removeWorthFromItem(item);
    }

    private void removeWorthFromInventorySlots(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                removeWorthFromItem(item);
            }
        }
    }

    private void reapplyWorthToInventorySlots(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                plugin.getItemWorthManager().updateSingleItem(item);
            }
        }
        player.updateInventory();
    }

    private void removeWorthFromItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
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

    public boolean isProcessingInventory(UUID uuid) {
        return processingInventory.contains(uuid);
    }
}