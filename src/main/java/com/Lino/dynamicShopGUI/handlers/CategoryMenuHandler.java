package com.Lino.dynamicShopGUI.handlers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class CategoryMenuHandler {

    private final DynamicShopGUI plugin;

    public CategoryMenuHandler(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    public void handleClick(Player player, ItemStack clicked, ClickType clickType, String title, int slot) {
        if (slot == 46) {
            return;
        }

        if (clicked.getType() == Material.ARROW) {
            String displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

            if (displayName.contains("Back to Categories")) {
                plugin.getGUIManager().openMainMenu(player);
                return;
            } else if (displayName.contains("Previous Page")) {
                String category = plugin.getGUIManager().getPlayerCategory(player.getUniqueId());
                if (category != null) {
                    int currentPage = plugin.getGUIManager().getPlayerPage(player.getUniqueId());
                    plugin.getGUIManager().openCategoryMenu(player, category, currentPage - 1);
                }
                return;
            } else if (displayName.contains("Next Page")) {
                String category = plugin.getGUIManager().getPlayerCategory(player.getUniqueId());
                if (category != null) {
                    int currentPage = plugin.getGUIManager().getPlayerPage(player.getUniqueId());
                    plugin.getGUIManager().openCategoryMenu(player, category, currentPage + 1);
                }
                return;
            }
        }

        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE ||
                clicked.getType() == Material.LIME_STAINED_GLASS_PANE ||
                clicked.getType() == Material.PAPER) {
            return;
        }

        if (clicked.getItemMeta() != null && clicked.getItemMeta().getDisplayName() != null) {
            String displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            if (displayName.contains("Page")) {
                return;
            }
        }

        String categoryName = plugin.getGUIManager().getPlayerCategory(player.getUniqueId());
        if (categoryName != null) {
            Material categoryIcon = plugin.getShopConfig().getCategoryIcon(categoryName);
            if (clicked.getType() == categoryIcon && slot >= 45) {
                return;
            }
        }

        if (slot >= 45) {
            return;
        }

        Material material = clicked.getType();
        boolean isBuying = clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT;

        if (isBuying && clicked.getItemMeta() != null && clicked.getItemMeta().getLore() != null) {
            for (String loreLine : clicked.getItemMeta().getLore()) {
                if (ChatColor.stripColor(loreLine).contains("Out of Stock")) {
                    player.sendMessage(plugin.getShopConfig().getMessage("errors.out-of-stock"));
                    if (plugin.getShopConfig().isSoundEnabled()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                    }
                    return;
                }
            }
        }

        plugin.getGUIManager().setPlayerSelectedItem(player.getUniqueId(), material);
        plugin.getGUIManager().openTransactionMenu(player, material, isBuying);
    }
}