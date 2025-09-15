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
    private final int[] CATEGORY_SLOTS = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33, 38, 39, 40, 41, 42};

    public MainMenuHandler(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    public void handleClick(Player player, ItemStack clicked, int slot) {
        if (clicked.getItemMeta() == null || clicked.getItemMeta().getDisplayName() == null) return;

        if (slot == 50 && clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (plugin.getShopConfig().isCustomButtonEnabled() && slot == plugin.getShopConfig().getCustomButtonSlot()) {
            String command = plugin.getShopConfig().getCustomButtonCommand();
            if (command != null && !command.isEmpty()) {
                player.closeInventory();
                plugin.getServer().dispatchCommand(player, command);
            }
            return;
        }

        if (slot == 48 || slot == 49 || slot == 4) {
            return;
        }

        if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE ||
                clicked.getType() == Material.GREEN_STAINED_GLASS_PANE) {
            return;
        }

        boolean isCategorySlot = false;
        for (int categorySlot : CATEGORY_SLOTS) {
            if (slot == categorySlot) {
                isCategorySlot = true;
                break;
            }
        }

        if (isCategorySlot) {
            String clickedName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            Map<String, CategoryConfigLoader.CategoryConfig> categories = plugin.getShopConfig().getAllCategories();

            for (Map.Entry<String, CategoryConfigLoader.CategoryConfig> entry : categories.entrySet()) {
                if (entry.getValue().getDisplayName().equalsIgnoreCase(clickedName)) {
                    plugin.getGUIManager().openCategoryMenu(player, entry.getKey(), 0);
                    return;
                }
            }
        }
    }
}