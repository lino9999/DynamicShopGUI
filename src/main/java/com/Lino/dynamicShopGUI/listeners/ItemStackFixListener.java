package com.Lino.dynamicShopGUI.listeners;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
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

        if (event.getClick() == ClickType.DOUBLE_CLICK && cursor != null && cursor.getType() != Material.AIR) {
            needsProcessing = true;
        }

        if (event.isShiftClick() && clicked != null && clicked.getType() != Material.AIR) {
            needsProcessing = true;
        }

        if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
            needsProcessing = true;
        }

        if (needsProcessing) {
            processingInventory.add(player.getUniqueId());

            cleanInventoryForStacking(player);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        processingInventory.remove(player.getUniqueId());
                        reapplyWorthToInventory(player);
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
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

        processingInventory.add(player.getUniqueId());

        cleanInventoryForStacking(player);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    processingInventory.remove(player.getUniqueId());
                    reapplyWorthToInventory(player);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler(priority = EventPriority.HIGH)
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

        ItemStack item = event.getItem().getItemStack();
        removeWorthFromItem(item);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    for (ItemStack invItem : player.getInventory().getContents()) {
                        if (invItem != null && invItem.getType() == item.getType()) {
                            plugin.getItemWorthManager().updateSingleItem(invItem);
                        }
                    }
                    player.updateInventory();
                }
            }
        }.runTaskLater(plugin, 3L);
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

    private void cleanInventoryForStacking(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                removeWorthFromItem(item);
            }
        }

        ItemStack cursor = player.getOpenInventory().getCursor();
        if (cursor != null && cursor.getType() != Material.AIR) {
            removeWorthFromItem(cursor);
        }
    }

    private void reapplyWorthToInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                plugin.getItemWorthManager().updateSingleItem(item);
            }
        }

        ItemStack cursor = player.getOpenInventory().getCursor();
        if (cursor != null && cursor.getType() != Material.AIR) {
            plugin.getItemWorthManager().updateSingleItem(cursor);
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