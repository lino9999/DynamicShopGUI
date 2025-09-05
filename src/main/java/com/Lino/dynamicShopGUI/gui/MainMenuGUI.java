package com.Lino.dynamicShopGUI.gui;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.config.CategoryConfigLoader;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class MainMenuGUI {

    private final DynamicShopGUI plugin;

    public MainMenuGUI(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Map<String, CategoryConfigLoader.CategoryConfig> categories = plugin.getShopConfig().getAllCategories();

        Inventory inv = Bukkit.createInventory(null, 54, plugin.getShopConfig().getMessage("gui.main-title"));

        ItemStack glassFiller = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassFiller.getItemMeta();
        glassMeta.setDisplayName(" ");
        glassFiller.setItemMeta(glassMeta);

        ItemStack decorGlass = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta decorMeta = decorGlass.getItemMeta();
        decorMeta.setDisplayName(" ");
        decorGlass.setItemMeta(decorMeta);

        for (int i = 0; i < 54; i++) {
            inv.setItem(i, glassFiller);
        }

        inv.setItem(0, decorGlass);
        inv.setItem(8, decorGlass);
        inv.setItem(45, decorGlass);
        inv.setItem(53, decorGlass);

        inv.setItem(9, decorGlass);
        inv.setItem(17, decorGlass);
        inv.setItem(36, decorGlass);
        inv.setItem(44, decorGlass);

        ItemStack centerDecor = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta centerMeta = centerDecor.getItemMeta();
        centerMeta.setDisplayName(plugin.getShopConfig().getMessage("gui.main-header"));
        List<String> centerLore = new ArrayList<>();
        centerLore.add("");
        centerLore.add(plugin.getShopConfig().getMessage("gui.welcome"));
        centerLore.add(plugin.getShopConfig().getMessage("gui.price-info"));
        centerLore.add("");
        centerLore.add(plugin.getShopConfig().getMessage("gui.categories-label", "%amount%", String.valueOf(categories.size())));
        centerLore.add(plugin.getShopConfig().getMessage("gui.tax-enabled-label", "%status%", plugin.getShopConfig().isTaxEnabled() ? "Yes" : "No"));
        centerMeta.setLore(centerLore);
        centerDecor.setItemMeta(centerMeta);
        inv.setItem(4, centerDecor);

        int[] categorySlots = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33, 38, 39, 40, 41, 42};
        int slotIndex = 0;

        List<Map.Entry<String, CategoryConfigLoader.CategoryConfig>> sortedCategories = new ArrayList<>(categories.entrySet());
        sortedCategories.sort(Map.Entry.comparingByKey());

        for (Map.Entry<String, CategoryConfigLoader.CategoryConfig> entry : sortedCategories) {
            if (slotIndex >= categorySlots.length) break;

            CategoryConfigLoader.CategoryConfig category = entry.getValue();

            ItemStack categoryItem = new ItemStack(category.getIcon());
            ItemMeta meta = categoryItem.getItemMeta();
            meta.setDisplayName(plugin.getShopConfig().getMessage("gui.item-name", "%item%", category.getDisplayName()));

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(plugin.getShopConfig().getMessage("gui.click-to-browse"));
            lore.add("");
            lore.add(plugin.getShopConfig().getMessage("gui.items-label", "%amount%", String.valueOf(category.getItems().size())));
            lore.add(plugin.getShopConfig().getMessage("gui.tax-rate-label", "%rate%", String.valueOf(category.getTaxRate())));
            lore.add("");
            lore.add(plugin.getShopConfig().getMessage("gui.click-to-open"));

            meta.setLore(lore);
            categoryItem.setItemMeta(meta);

            inv.setItem(categorySlots[slotIndex], categoryItem);
            slotIndex++;
        }

        double balance = plugin.getEconomy().getBalance(player);
        ItemStack playerInfo = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta playerMeta = playerInfo.getItemMeta();
        playerMeta.setDisplayName(plugin.getShopConfig().getMessage("gui.player-name", "%name%", player.getName()));
        List<String> playerLore = new ArrayList<>();
        playerLore.add("");
        playerLore.add(plugin.getShopConfig().getMessage("gui.balance-display", "%balance%", String.format("%.2f", balance)));
        playerLore.add("");
        playerLore.add(plugin.getShopConfig().getMessage("gui.happy-shopping"));
        playerMeta.setLore(playerLore);
        playerInfo.setItemMeta(playerMeta);
        inv.setItem(49, playerInfo);

        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.setDisplayName(plugin.getShopConfig().getMessage("gui.close"));
        closeButton.setItemMeta(closeMeta);
        inv.setItem(50, closeButton);

        ItemStack infoBook = new ItemStack(Material.BOOK);
        ItemMeta bookMeta = infoBook.getItemMeta();
        bookMeta.setDisplayName(plugin.getShopConfig().getMessage("gui.shop-guide-title"));
        List<String> bookLore = new ArrayList<>();
        bookLore.add("");
        bookLore.add(plugin.getShopConfig().getMessage("gui.guide-buy"));
        bookLore.add(plugin.getShopConfig().getMessage("gui.guide-sell"));
        bookLore.add(plugin.getShopConfig().getMessage("gui.guide-dynamic"));
        bookLore.add(plugin.getShopConfig().getMessage("gui.guide-low-stock"));
        bookLore.add(plugin.getShopConfig().getMessage("gui.guide-high-stock"));
        bookMeta.setLore(bookLore);
        infoBook.setItemMeta(bookMeta);
        inv.setItem(48, infoBook);

        player.openInventory(inv);
    }
}