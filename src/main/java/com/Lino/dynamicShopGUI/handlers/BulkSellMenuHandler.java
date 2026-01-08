package com.Lino.dynamicShopGUI.handlers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.gui.BulkSellMenuGUI;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

public class BulkSellMenuHandler {

    private final DynamicShopGUI plugin;

    public BulkSellMenuHandler(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    public void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack itemStack = event.getCurrentItem();
        Material type = itemStack != null ? itemStack.getType() : Material.AIR;
        Inventory clickedInventory = event.getClickedInventory();
        int slot = event.getSlot();

        if (clickedInventory == null) { return; }

//        if (itemStack == null || itemStack.getType() == Material.AIR) {
//            if (Objects.equals(clickedInventory, event.getView().getTopInventory())) {
//                if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
//                    if (!isAllowedItem(event.getCursor(), plugin.getGUIManager().getPlayerCategory(player.getUniqueId()))) {
//                        event.setCancelled(true);
//                        if (plugin.getShopConfig().isSoundEnabled()) {
//                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 0.5f, 1.0f);
//                        }
//                        return;
//                    }
//                }
//            } else if (event.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY)) {
//                plugin.getLogger().info("Shift-click item: " + clickedInventory.getItem(event.getSlot()));
//                if (!isAllowedItem(clickedInventory.getItem(event.getSlot()),
//                        plugin.getGUIManager().getPlayerCategory(player.getUniqueId()))) {
//                    event.setCancelled(true);
//                    if (plugin.getShopConfig().isSoundEnabled()) {
//                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 0.5f, 1.0f);
//                    }
//                    return;
//                }
//            }
//            returnItemsToPlayer(player, false);
//        }

        if (slot == BulkSellMenuGUI.BACK_SLOT && clickedInventory == event.getView().getTopInventory()) {
            String category = plugin.getGUIManager().getPlayerCategory(player.getUniqueId());

            event.setCancelled(true);
            returnItemsToPlayer(player, true);
            if (category != null) {
                int page = plugin.getGUIManager().getPlayerPage(player.getUniqueId());
                plugin.getGUIManager().openCategoryMenu(player, category.split("_")[1], page);
            } else {
                player.closeInventory();
            }
            return;
        }

        if (slot == BulkSellMenuGUI.CLEAR_SLOT && clickedInventory == event.getView().getTopInventory()) {
            event.setCancelled(true);
            returnItemsToPlayer(player, true);
            return;
        }

        if (slot == BulkSellMenuGUI.SELL_SLOT && clickedInventory == event.getView().getTopInventory()) {
            event.setCancelled(true);
            return;
        }

        if (Arrays.stream(BulkSellMenuGUI.SELL_SLOTS).noneMatch( sellSlot -> sellSlot == slot)
                && (Objects.equals(clickedInventory, event.getView().getTopInventory()))) {
            event.setCancelled(true);
            return;
        }

        if (clickedInventory == event.getView().getTopInventory() && (
                type == Material.BLACK_STAINED_GLASS_PANE || type == Material.GRAY_STAINED_GLASS_PANE ||
                type == Material.IRON_BLOCK || type == Material.COAL_BLOCK ||
                type == Material.EMERALD_BLOCK || type == Material.REDSTONE_BLOCK ||
                type == Material.LAPIS_BLOCK || type == Material.LIME_STAINED_GLASS ||
                type == Material.RED_STAINED_GLASS)) {
            event.setCancelled(true);
            return;
        }

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                returnItemsToPlayer(player, false);
            }
        }.runTaskLater(plugin, 1L);
    }

    public void handleDrag(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack itemStack = event.getOldCursor();

        plugin.getLogger().info("Held item: " + itemStack);
        plugin.getLogger().info("Item allowed: " +
                isAllowedItem(itemStack, plugin.getGUIManager().getPlayerCategory(player.getUniqueId())));
        plugin.getLogger().info("Inventory match: " +
                Objects.equals(event.getInventory(), event.getView().getTopInventory()));

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                returnItemsToPlayer(player, false);
            }
        }.runTaskLater(plugin, 1L);
    }

    public void returnItemsToPlayer(Player player, boolean allItems) {
        for (int slot : BulkSellMenuGUI.SELL_SLOTS) {
            ItemStack item = player.getOpenInventory().getItem(slot);

            if ((item != null && item.getType() != Material.AIR)
                    && (allItems || !isAllowedItem(item, plugin.getGUIManager().getPlayerCategory(player.getUniqueId())))) {

                HashMap<Integer, ItemStack> notReturned = player.getInventory().addItem(item);
                if (plugin.getShopConfig().isSoundEnabled() && !allItems) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 0.5f, 1.0f);
                }

                if (!notReturned.isEmpty()) {
                    for (ItemStack leftover : notReturned.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }
                }
                player.getOpenInventory().setItem(slot, null);
            }
        }
    }

    public boolean isAllowedItem(ItemStack itemStack, String category) {
        if (category.contains("fish")) {
            ItemMeta fishMeta = itemStack.getItemMeta();
            if (fishMeta != null) {
                boolean isStarcatcher = fishMeta.getAsComponentString().contains("starcatcher");
                if (isStarcatcher) { return true; }
            }
        }
        Set<String> shopItems = plugin.getShopConfig().getShopItems().get(category).keySet();
        return shopItems.contains(itemStack.getType().toString());
    }
}
