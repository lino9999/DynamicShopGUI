package com.Lino.dynamicShopGUI.handlers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

        if (slot == 49) {
            plugin.getGUIManager().openMainMenu(player);
            return;
        }

        if (slot == 48) {
            String category = plugin.getGUIManager().getPlayerCategory(player.getUniqueId());
            if (category != null) {
                int currentPage = plugin.getGUIManager().getPlayerPage(player.getUniqueId());
                if (currentPage > 0) {
                    plugin.getGUIManager().openCategoryMenu(player, category, currentPage - 1);
                }
            }
            return;
        }

        if (slot == 50) {
            String category = plugin.getGUIManager().getPlayerCategory(player.getUniqueId());
            if (category != null) {
                int currentPage = plugin.getGUIManager().getPlayerPage(player.getUniqueId());
                plugin.getGUIManager().openCategoryMenu(player, category, currentPage + 1);
            }
            return;
        }

        if (slot == 52) {
            return;
        }

        if (slot >= 45) {
            return;
        }

        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE ||
                clicked.getType() == Material.LIME_STAINED_GLASS_PANE) {
            return;
        }

        Material material = clicked.getType();
        boolean isBuying = clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT;

        if (isBuying && clicked.getItemMeta() != null && clicked.getItemMeta().getLore() != null) {
            for (String loreLine : clicked.getItemMeta().getLore()) {
                if (ChatColor.stripColor(loreLine).contains("Out of Stock")) {
                    player.sendMessage(plugin.getShopConfig().getMessage("errors.out-of-stock"));
                    if (plugin.getShopConfig().isSoundEnabled()) {
                        player.playSound(player.getLocation(), "entity.villager.no", 0.5f, 1.0f);
                    }
                    return;
                }
            }
        }

        plugin.getGUIManager().setPlayerSelectedItem(player.getUniqueId(), material);
        plugin.getGUIManager().openTransactionMenu(player, material, isBuying);
    }
}