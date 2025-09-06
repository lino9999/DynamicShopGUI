package com.Lino.dynamicShopGUI.handlers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.config.CategoryConfigLoader;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.Map;

public class MainMenuHandler {

    private final DynamicShopGUI plugin;

    public MainMenuHandler(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    public void handleClick(Player player, ItemStack clicked, int slot) {
        if (clicked.getItemMeta() == null || clicked.getItemMeta().getDisplayName() == null) return;

        String clickedName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.BOOK || clicked.getType() == Material.PLAYER_HEAD ||
                clicked.getType() == Material.EMERALD_BLOCK || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE ||
                clicked.getType() == Material.GREEN_STAINED_GLASS_PANE) {
            return;
        }

        Map<String, CategoryConfigLoader.CategoryConfig> categories = plugin.getShopConfig().getAllCategories();

        for (Map.Entry<String, CategoryConfigLoader.CategoryConfig> entry : categories.entrySet()) {
            if (entry.getValue().getDisplayName().equalsIgnoreCase(clickedName)) {
                plugin.getGUIManager().openCategoryMenu(player, entry.getKey(), 0);
                return;
            }
        }
    }
}