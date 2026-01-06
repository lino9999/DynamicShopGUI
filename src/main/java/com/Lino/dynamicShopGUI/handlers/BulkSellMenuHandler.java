package com.Lino.dynamicShopGUI.handlers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BulkSellMenuHandler {

    private final DynamicShopGUI plugin;

    public BulkSellMenuHandler(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    public void handleClick(Player player, ItemStack clicked, int slot) {
        Material type = clicked.getType();

        Material selectedItem = plugin.getGUIManager().getPlayerSelectedItem(player.getUniqueId());

        if (selectedItem == null) {
            selectedItem = extractMaterialFromGUI(player);
            if (selectedItem != null) {
                plugin.getGUIManager().setPlayerSelectedItem(player.getUniqueId(), selectedItem);
            }
        }

        if (slot == 49 && type == Material.ARROW) {
            String category = plugin.getGUIManager().getPlayerCategory(player.getUniqueId());
            if (category != null) {
                int page = plugin.getGUIManager().getPlayerPage(player.getUniqueId());
                plugin.getGUIManager().openCategoryMenu(player, category, page);
            } else {
                plugin.getGUIManager().openMainMenu(player);
            }
            return;
        }

        if (slot == 13 || slot == 12 || slot == 14 || slot == 40 || slot >= 45) {
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
}
