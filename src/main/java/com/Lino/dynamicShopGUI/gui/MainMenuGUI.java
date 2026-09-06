package com.Lino.dynamicShopGUI.gui;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.config.CategoryConfigLoader;
import com.Lino.dynamicShopGUI.config.ShopConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.Lino.dynamicShopGUI.utils.MenuBuilder;
import java.util.*;

public class MainMenuGUI {

    private final DynamicShopGUI plugin;

    public MainMenuGUI(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        Map<String, CategoryConfigLoader.CategoryConfig> categories = plugin.getShopConfig().getAllCategories();
        List<Map.Entry<String, CategoryConfigLoader.CategoryConfig>> sortedCategories = new ArrayList<>(categories.entrySet());
        sortedCategories.sort(Map.Entry.comparingByKey());
        int itemsPerPage = plugin.getShopConfig().getMenuConfigManager()
                .getInt("gui.main-categories-per-page", 15);
        int totalPages = Math.max(1, (int) Math.ceil((double) sortedCategories.size() / itemsPerPage));
        int finalPage = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = MenuBuilder.create(plugin.getShopConfig().getMessage("gui.main-title"), 6).build();

        ItemStack glassFiller = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassFiller.getItemMeta();
        glassMeta.setDisplayName(" ");
        glassFiller.setItemMeta(glassMeta);

        ItemStack decorGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta decorMeta = decorGlass.getItemMeta();
        decorMeta.setDisplayName(" ");
        decorMeta.addEnchant(Enchantment.PROTECTION, 1, true);
        decorMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        decorGlass.setItemMeta(decorMeta);

        for (int i = 0; i < 54; i++) {
            inv.setItem(i, glassFiller);
        }

        inv.setItem(0, decorGlass);
        inv.setItem(1, decorGlass);
        inv.setItem(2, decorGlass);
        inv.setItem(3, decorGlass);
        inv.setItem(5, decorGlass);
        inv.setItem(6, decorGlass);
        inv.setItem(7, decorGlass);
        inv.setItem(8, decorGlass);
        inv.setItem(9, decorGlass);
        inv.setItem(17, decorGlass);
        inv.setItem(18, decorGlass);
        inv.setItem(26, decorGlass);
        inv.setItem(27, decorGlass);
        inv.setItem(35, decorGlass);
        inv.setItem(36, decorGlass);
        inv.setItem(44, decorGlass);
        inv.setItem(45, decorGlass);
        inv.setItem(46, decorGlass);
        inv.setItem(47, decorGlass);
        inv.setItem(51, decorGlass);
        inv.setItem(52, decorGlass);
        inv.setItem(53, decorGlass);

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

        int startIndex = finalPage * itemsPerPage;
        for (int i = startIndex; i < sortedCategories.size() && slotIndex < categorySlots.length; i++) {
            Map.Entry<String, CategoryConfigLoader.CategoryConfig> entry = sortedCategories.get(i);

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

        if (finalPage > 0) {
            ItemStack previous = new ItemStack(Material.ARROW);
            ItemMeta previousMeta = previous.getItemMeta();
            previousMeta.setDisplayName(plugin.getShopConfig().getMessage("gui.previous-page"));
            previous.setItemMeta(previousMeta);
            inv.setItem(47, previous);
        }
        if (finalPage < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.setDisplayName(plugin.getShopConfig().getMessage("gui.next-page"));
            next.setItemMeta(nextMeta);
            inv.setItem(51, next);
        }
        if (totalPages > 1) {
            ItemStack pageInfo = new ItemStack(Material.PAPER);
            ItemMeta pageMeta = pageInfo.getItemMeta();
            pageMeta.setDisplayName(plugin.getShopConfig().getMessage("gui.page-info",
                    "%current%", String.valueOf(finalPage + 1),
                    "%total%", String.valueOf(totalPages)));
            pageInfo.setItemMeta(pageMeta);
            inv.setItem(52, pageInfo);
        }

        for (ShopConfig.CustomButtonConfig btn : plugin.getShopConfig().getCustomButtons()) {
            int slot = btn.getSlot();

            if (slot >= 0 && slot < 54) {
                ItemStack customButton = new ItemStack(btn.getMaterial());
                ItemMeta customMeta = customButton.getItemMeta();
                if (customMeta != null) {
                    customMeta.setDisplayName(btn.getDisplayName());
                    customMeta.setLore(btn.getLore());
                    customButton.setItemMeta(customMeta);
                    inv.setItem(slot, customButton);
                }
            }
        }

        player.openInventory(inv);
    }
}