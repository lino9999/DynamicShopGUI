package com.Lino.dynamicShopGUI.handlers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.gui.BulkSellMenuGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
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
        Material type = event.getCurrentItem().getType();
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        Material selectedItem = plugin.getGUIManager().getPlayerSelectedItem(player.getUniqueId());

        if (selectedItem == null) {
            selectedItem = extractMaterialFromGUI(player);
            if (selectedItem != null) {
                plugin.getGUIManager().setPlayerSelectedItem(player.getUniqueId(), selectedItem);
            }
        }

        if (slot == BulkSellMenuGUI.BACK_SLOT && type == Material.ARROW) {
            String category = plugin.getGUIManager().getPlayerCategory(player.getUniqueId());

            event.setCancelled(true);
            returnItemsToPlayer(player);
            if (category != null) {
                int page = plugin.getGUIManager().getPlayerPage(player.getUniqueId());
                plugin.getGUIManager().openCategoryMenu(player, category.split("_")[1], page);
            } else {
                player.closeInventory();
            }
            return;
        }

        if (slot == BulkSellMenuGUI.CLEAR_SLOT && type == Material.HOPPER) {
            event.setCancelled(true);
            returnItemsToPlayer(player);
            return;
        }

        if (Arrays.stream(BulkSellMenuGUI.SELL_SLOTS).noneMatch( sellSlot -> sellSlot == slot)
                && (Objects.equals(event.getClickedInventory(), event.getView().getTopInventory()))) {
            event.setCancelled(true);
            return;
        }

        if (!isAllowedItem(event.getCurrentItem(), plugin.getGUIManager().getPlayerCategory(player.getUniqueId()))) {
            event.setCancelled(true);
            if (plugin.getShopConfig().isSoundEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            }
            return;
        }

        if (type == Material.BLACK_STAINED_GLASS_PANE || type == Material.GRAY_STAINED_GLASS_PANE ||
                type == Material.IRON_BLOCK || type == Material.COAL_BLOCK ||
                type == Material.EMERALD_BLOCK || type == Material.REDSTONE_BLOCK ||
                type == Material.LAPIS_BLOCK || type == Material.LIME_STAINED_GLASS ||
                type == Material.RED_STAINED_GLASS) {
            return;
        }
    }

    private Material extractMaterialFromGUI(Player player) {
        String title = ChatColor.stripColor(player.getOpenInventory().getTitle());
        String cleanTitle = title;

        Material material = null;

        if (cleanTitle.startsWith("Buy ") || cleanTitle.startsWith("Sell ")) {
            String itemName = cleanTitle.substring(4).trim();

            String[] possibleNames = {
                    itemName.toUpperCase().replace(" ", "_"),
                    itemName.toUpperCase().replace(" ", ""),
                    itemName.replace(" ", "_").toUpperCase()
            };

            for (String possibleName : possibleNames) {
                try {
                    material = Material.valueOf(possibleName);
                    break;
                } catch (IllegalArgumentException e) {
                }
            }
        }

        if (material == null) {
            ItemStack displayItem = player.getOpenInventory().getItem(13);
            if (displayItem != null && displayItem.getType() != Material.AIR) {
                material = displayItem.getType();
            }
        }

        return material;
    }

    public void returnItemsToPlayer(Player player) {
        for (int slot : BulkSellMenuGUI.SELL_SLOTS) {
            ItemStack item = player.getOpenInventory().getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> notReturned = player.getInventory().addItem(item);
                if (!notReturned.isEmpty()) {
                    for (ItemStack leftover : notReturned.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }
                }
                player.getOpenInventory().setItem(slot, null);
            }
        }
    }

    private boolean isAllowedItem(ItemStack itemStack, String category) {
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
