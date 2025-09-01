package com.Lino.dynamicShopGUI.managers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.config.CategoryConfigLoader;
import com.Lino.dynamicShopGUI.models.ShopItem;
import com.Lino.dynamicShopGUI.utils.GradientColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class GUIManager {

    private final DynamicShopGUI plugin;
    private final Map<UUID, String> playerCategory = new HashMap<>();
    private final Map<UUID, Material> playerSelectedItem = new HashMap<>();
    private final Map<UUID, Integer> playerPage = new HashMap<>();

    public GUIManager(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Map<String, CategoryConfigLoader.CategoryConfig> categories = plugin.getShopConfig().getAllCategories();

        Inventory inv = Bukkit.createInventory(null, 54, GradientColor.apply("<gradient:#00ff00:#00ffff>Dynamic Shop</gradient>"));

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
        centerMeta.setDisplayName(GradientColor.apply("<gradient:#ffd700:#ffaa00>DYNAMIC SHOP</gradient>"));
        List<String> centerLore = new ArrayList<>();
        centerLore.add("");
        centerLore.add(GradientColor.apply("&7Welcome to the Dynamic Shop!"));
        centerLore.add(GradientColor.apply("&7Prices change based on supply & demand"));
        centerLore.add("");
        centerLore.add(GradientColor.apply("<gradient:#ffff00:#ff8800>Categories: " + categories.size() + "</gradient>"));
        centerLore.add(GradientColor.apply("<gradient:#ffff00:#ff8800>Tax Enabled: " + (plugin.getShopConfig().isTaxEnabled() ? "Yes" : "No") + "</gradient>"));
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
            meta.setDisplayName(GradientColor.apply("<gradient:#ffff00:#ff8800>" + category.getDisplayName() + "</gradient>"));

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(GradientColor.apply("&7Click to browse items"));
            lore.add("");
            lore.add(GradientColor.apply("&8Items: &7" + category.getItems().size()));
            lore.add(GradientColor.apply("&8Tax Rate: &7" + category.getTaxRate() + "%"));
            lore.add("");
            lore.add(GradientColor.apply("<gradient:#00ff00:#00ffff>» Click to open</gradient>"));

            meta.setLore(lore);
            categoryItem.setItemMeta(meta);

            inv.setItem(categorySlots[slotIndex], categoryItem);
            slotIndex++;
        }

        double balance = plugin.getEconomy().getBalance(player);
        ItemStack playerInfo = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta playerMeta = playerInfo.getItemMeta();
        playerMeta.setDisplayName(GradientColor.apply("<gradient:#00ffff:#0088ff>" + player.getName() + "</gradient>"));
        List<String> playerLore = new ArrayList<>();
        playerLore.add("");
        playerLore.add(GradientColor.applyWithVariables("<gradient:#00ff00:#88ff00>Balance: $%balance%</gradient>",
                "%balance%", String.format("%.2f", balance)));
        playerLore.add("");
        playerLore.add(GradientColor.apply("&8Happy shopping!"));
        playerMeta.setLore(playerLore);
        playerInfo.setItemMeta(playerMeta);
        inv.setItem(49, playerInfo);

        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.setDisplayName(GradientColor.apply("<gradient:#ff0000:#ff8800>Close</gradient>"));
        closeButton.setItemMeta(closeMeta);
        inv.setItem(50, closeButton);

        ItemStack infoBook = new ItemStack(Material.BOOK);
        ItemMeta bookMeta = infoBook.getItemMeta();
        bookMeta.setDisplayName(GradientColor.apply("<gradient:#ff00ff:#8800ff>Shop Guide</gradient>"));
        List<String> bookLore = new ArrayList<>();
        bookLore.add("");
        bookLore.add(GradientColor.apply("&7• Left click to buy items"));
        bookLore.add(GradientColor.apply("&7• Right click to sell items"));
        bookLore.add(GradientColor.apply("&7• Prices change dynamically"));
        bookLore.add(GradientColor.apply("&7• Low stock = Higher prices"));
        bookLore.add(GradientColor.apply("&7• High stock = Lower prices"));
        bookMeta.setLore(bookLore);
        infoBook.setItemMeta(bookMeta);
        inv.setItem(48, infoBook);

        player.openInventory(inv);
    }

    public void openCategoryMenu(Player player, String category, int page) {
        playerCategory.put(player.getUniqueId(), category);
        playerPage.put(player.getUniqueId(), page);

        String displayName = plugin.getShopConfig().getCategoryDisplayName(category.toLowerCase());
        String title = GradientColor.apply("<gradient:#00ff00:#00ffff>Shop - " + displayName + "</gradient>");
        Inventory inv = Bukkit.createInventory(null, 54, title);

        ItemStack glassFiller = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassFiller.getItemMeta();
        glassMeta.setDisplayName(" ");
        glassFiller.setItemMeta(glassMeta);

        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glassFiller);
        }

        ItemStack decorGlass = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta decorMeta = decorGlass.getItemMeta();
        decorMeta.setDisplayName(" ");
        decorGlass.setItemMeta(decorMeta);

        inv.setItem(45, decorGlass);
        inv.setItem(53, decorGlass);

        plugin.getDatabaseManager().getItemsByCategory(category).thenAccept(items -> {
            List<Map.Entry<Material, ShopItem>> sortedItems = new ArrayList<>(items.entrySet());
            sortedItems.sort(Map.Entry.comparingByKey());

            int itemsPerPage = plugin.getShopConfig().getItemsPerPage();
            int totalPages = (int) Math.ceil((double) sortedItems.size() / itemsPerPage);
            final int finalPage = Math.max(0, Math.min(page, totalPages - 1));

            int startIndex = finalPage * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, sortedItems.size());

            int slot = 0;
            for (int i = startIndex; i < endIndex && slot < 45; i++) {
                Map.Entry<Material, ShopItem> entry = sortedItems.get(i);
                ShopItem shopItem = entry.getValue();

                if (shopItem.getStock() == 0 && plugin.getShopConfig().isRestockEnabled()) {
                    if (!plugin.getRestockManager().isRestocking(shopItem.getMaterial())) {
                        plugin.getRestockManager().startRestockTimer(shopItem.getMaterial());
                    }
                }

                ItemStack displayItem = createShopItemDisplay(shopItem);
                inv.setItem(slot++, displayItem);
            }

            if (finalPage > 0) {
                ItemStack prevPage = new ItemStack(Material.ARROW);
                ItemMeta prevMeta = prevPage.getItemMeta();
                prevMeta.setDisplayName(GradientColor.apply("<gradient:#ffff00:#ff8800>« Previous Page</gradient>"));
                List<String> prevLore = new ArrayList<>();
                prevLore.add(GradientColor.apply("&7Page " + finalPage + "/" + totalPages));
                prevMeta.setLore(prevLore);
                prevPage.setItemMeta(prevMeta);
                inv.setItem(48, prevPage);
            }

            ItemStack backButton = new ItemStack(Material.ARROW);
            ItemMeta backMeta = backButton.getItemMeta();
            backMeta.setDisplayName(GradientColor.apply("<gradient:#ff0000:#ff8800>Back to Categories</gradient>"));
            backButton.setItemMeta(backMeta);
            inv.setItem(49, backButton);

            if (finalPage < totalPages - 1) {
                ItemStack nextPage = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = nextPage.getItemMeta();
                nextMeta.setDisplayName(GradientColor.apply("<gradient:#ffff00:#ff8800>Next Page »</gradient>"));
                List<String> nextLore = new ArrayList<>();
                nextLore.add(GradientColor.apply("&7Page " + (finalPage + 2) + "/" + totalPages));
                nextMeta.setLore(nextLore);
                nextPage.setItemMeta(nextMeta);
                inv.setItem(50, nextPage);
            }

            if (totalPages > 1) {
                ItemStack pageInfo = new ItemStack(Material.PAPER);
                ItemMeta pageMeta = pageInfo.getItemMeta();
                pageMeta.setDisplayName(GradientColor.apply("<gradient:#ffd700:#ffaa00>Page " + (finalPage + 1) + "/" + totalPages + "</gradient>"));
                List<String> pageLore = new ArrayList<>();
                pageLore.add(GradientColor.apply("&7Total items: " + sortedItems.size()));
                pageLore.add(GradientColor.apply("&7Items per page: " + itemsPerPage));
                pageMeta.setLore(pageLore);
                pageInfo.setItemMeta(pageMeta);
                inv.setItem(52, pageInfo);
            }

            double taxRate = plugin.getShopConfig().getTaxRate(category) * 100;
            ItemStack categoryInfo = new ItemStack(plugin.getShopConfig().getCategoryIcon(category));
            ItemMeta categoryMeta = categoryInfo.getItemMeta();
            categoryMeta.setDisplayName(GradientColor.apply("<gradient:#00ffff:#0088ff>" + displayName + "</gradient>"));
            List<String> categoryLore = new ArrayList<>();
            categoryLore.add("");
            categoryLore.add(GradientColor.applyWithVariables("&7Category Tax: <gradient:#ffff00:#ff8800>%tax%%</gradient>",
                    "%tax%", String.format("%.1f", taxRate)));
            categoryLore.add(GradientColor.applyWithVariables("&7Total Items: <gradient:#ffff00:#ff8800>%items%</gradient>",
                    "%items%", String.valueOf(sortedItems.size())));
            categoryMeta.setLore(categoryLore);
            categoryInfo.setItemMeta(categoryMeta);
            inv.setItem(46, categoryInfo);

            Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inv));
        });
    }

    public void openTransactionMenu(Player player, Material material, boolean isBuying) {
        playerSelectedItem.put(player.getUniqueId(), material);

        plugin.getDatabaseManager().getShopItem(material).thenAccept(item -> {
            if (item == null) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(GradientColor.apply("<gradient:#ff0000:#ff8800>This item is not available in the shop!</gradient>"));
                });
                return;
            }

            String title = isBuying ?
                    GradientColor.apply("<gradient:#00ff00:#88ff00>Buy " + formatMaterialName(material) + "</gradient>") :
                    GradientColor.apply("<gradient:#ff0000:#ff8800>Sell " + formatMaterialName(material) + "</gradient>");

            Inventory inv = Bukkit.createInventory(null, 54, title);

            ItemStack glassFiller = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta glassMeta = glassFiller.getItemMeta();
            glassMeta.setDisplayName(" ");
            glassFiller.setItemMeta(glassMeta);

            for (int i = 0; i < 54; i++) {
                inv.setItem(i, glassFiller);
            }

            ItemStack decorGlass = new ItemStack(isBuying ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
            ItemMeta decorMeta = decorGlass.getItemMeta();
            decorMeta.setDisplayName(" ");
            decorGlass.setItemMeta(decorMeta);

            inv.setItem(0, decorGlass);
            inv.setItem(1, decorGlass);
            inv.setItem(7, decorGlass);
            inv.setItem(8, decorGlass);
            inv.setItem(9, decorGlass);
            inv.setItem(17, decorGlass);
            inv.setItem(45, decorGlass);
            inv.setItem(46, decorGlass);
            inv.setItem(52, decorGlass);
            inv.setItem(53, decorGlass);

            for (int i = 10; i <= 16; i++) {
                inv.setItem(i, decorGlass);
            }

            if (item.getStock() == 0 && plugin.getShopConfig().isRestockEnabled()) {
                if (!plugin.getRestockManager().isRestocking(material)) {
                    plugin.getRestockManager().startRestockTimer(material);
                }
            }

            ItemStack displayItem = new ItemStack(material);
            ItemMeta displayMeta = displayItem.getItemMeta();
            displayMeta.setDisplayName(GradientColor.apply("<gradient:#ffff00:#ff8800>" + formatMaterialName(material) + "</gradient>"));
            List<String> displayLore = new ArrayList<>();

            if (plugin.getShopConfig().showStock()) {
                displayLore.add(GradientColor.applyWithVariables("&7Stock: <gradient:#00ff00:#00ffff>%stock%/%max%</gradient>",
                        "%stock%", String.valueOf(item.getStock()),
                        "%max%", String.valueOf(item.getMaxStock())));
            }

            displayLore.add(GradientColor.applyWithVariables("&7Buy Price: <gradient:#00ff00:#88ff00>$%price%</gradient>",
                    "%price%", String.format("%.2f", item.getBuyPrice())));
            displayLore.add(GradientColor.applyWithVariables("&7Sell Price: <gradient:#ff0000:#ff8800>$%price%</gradient>",
                    "%price%", String.format("%.2f", item.getSellPrice())));

            if (!isBuying && plugin.getShopConfig().showTaxInfo() && plugin.getShopConfig().isTaxEnabled()) {
                if (!plugin.getShopConfig().isTaxExempt(material)) {
                    double taxRate = plugin.getShopConfig().getTaxRate(item.getCategory()) * 100;
                    displayLore.add(GradientColor.applyWithVariables("&8Tax Rate: <gradient:#ff0000:#ff8800>%tax%%</gradient>",
                            "%tax%", String.format("%.1f", taxRate)));
                } else {
                    displayLore.add(GradientColor.apply("&8Tax: <gradient:#00ff00:#88ff00>Exempt</gradient>"));
                }
            }

            if (plugin.getShopConfig().showPriceTrends()) {
                displayLore.add("");
                displayLore.add(formatPriceChange(item.getPriceChangePercent()));
            }

            if (item.getStock() == 0 && plugin.getRestockManager().isRestocking(material)) {
                String countdown = plugin.getRestockManager().getRestockCountdown(material);
                if (countdown != null) {
                    displayLore.add("");
                    displayLore.add(GradientColor.apply("<gradient:#ff0000:#ff8800>Out of Stock!</gradient>"));
                    displayLore.add(GradientColor.applyWithVariables("<gradient:#ffff00:#ff8800>Restocking in: %time%</gradient>",
                            "%time%", countdown));
                }
            }

            displayMeta.setLore(displayLore);
            displayItem.setItemMeta(displayMeta);
            inv.setItem(13, displayItem);

            if (isBuying) {
                setAmountButton(inv, 29, Material.LIME_STAINED_GLASS_PANE, 1, true, item);
                setAmountButton(inv, 30, Material.LIME_STAINED_GLASS_PANE, 10, true, item);
                setAmountButton(inv, 31, Material.LIME_STAINED_GLASS_PANE, 32, true, item);
                setAmountButton(inv, 32, Material.LIME_STAINED_GLASS_PANE, 64, true, item);
                setAmountButton(inv, 33, Material.LIME_STAINED_GLASS_PANE, 128, true, item);
            } else {
                setAmountButton(inv, 29, Material.RED_STAINED_GLASS_PANE, 1, false, item);
                setAmountButton(inv, 30, Material.RED_STAINED_GLASS_PANE, 10, false, item);
                setAmountButton(inv, 31, Material.RED_STAINED_GLASS_PANE, 32, false, item);
                setAmountButton(inv, 32, Material.RED_STAINED_GLASS_PANE, 64, false, item);
                setAmountButton(inv, 33, Material.RED_STAINED_GLASS_PANE, -1, false, item);
            }

            ItemStack toggleButton = new ItemStack(Material.HOPPER);
            ItemMeta toggleMeta = toggleButton.getItemMeta();
            toggleMeta.setDisplayName(isBuying ?
                    GradientColor.apply("<gradient:#ffd700:#ffaa00>» Switch to Sell</gradient>") :
                    GradientColor.apply("<gradient:#ffd700:#ffaa00>» Switch to Buy</gradient>"));
            List<String> toggleLore = new ArrayList<>();
            toggleLore.add("");
            toggleLore.add(GradientColor.apply("&7Click to " + (isBuying ? "sell" : "buy") + " instead"));
            toggleMeta.setLore(toggleLore);
            toggleButton.setItemMeta(toggleMeta);
            inv.setItem(22, toggleButton);

            ItemStack backButton = new ItemStack(Material.ARROW);
            ItemMeta backMeta = backButton.getItemMeta();
            backMeta.setDisplayName(GradientColor.apply("<gradient:#ff0000:#ff8800>« Back</gradient>"));
            backButton.setItemMeta(backMeta);
            inv.setItem(49, backButton);

            double balance = plugin.getEconomy().getBalance(player);
            ItemStack balanceInfo = new ItemStack(Material.EMERALD);
            ItemMeta balanceMeta = balanceInfo.getItemMeta();
            balanceMeta.setDisplayName(GradientColor.apply("<gradient:#00ff00:#88ff00>Your Balance</gradient>"));
            List<String> balanceLore = new ArrayList<>();
            balanceLore.add("");
            balanceLore.add(GradientColor.applyWithVariables("&7Current: <gradient:#00ff00:#88ff00>$%balance%</gradient>",
                    "%balance%", String.format("%.2f", balance)));
            balanceMeta.setLore(balanceLore);
            balanceInfo.setItemMeta(balanceMeta);
            inv.setItem(4, balanceInfo);

            Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inv));
        });
    }

    private ItemStack createShopItemDisplay(ShopItem item) {
        ItemStack display = new ItemStack(item.getMaterial());
        ItemMeta meta = display.getItemMeta();

        meta.setDisplayName(GradientColor.apply("<gradient:#ffff00:#ff8800>" + formatMaterialName(item.getMaterial()) + "</gradient>"));

        List<String> lore = new ArrayList<>();

        if (plugin.getShopConfig().showStock()) {
            lore.add(GradientColor.applyWithVariables("&7Stock: <gradient:#00ff00:#00ffff>%stock%/%max%</gradient>",
                    "%stock%", String.valueOf(item.getStock()),
                    "%max%", String.valueOf(item.getMaxStock())));
        }

        lore.add("");
        lore.add(GradientColor.applyWithVariables("&7Buy Price: <gradient:#00ff00:#88ff00>$%price%</gradient>",
                "%price%", String.format("%.2f", item.getBuyPrice())));

        double sellPrice = item.getSellPrice();
        if (plugin.getShopConfig().showTaxInfo() && plugin.getShopConfig().isTaxEnabled()) {
            double tax = plugin.getShopConfig().calculateTax(item.getMaterial(), item.getCategory(), sellPrice);
            double netPrice = sellPrice - tax;

            if (tax > 0) {
                lore.add(GradientColor.applyWithVariables("&7Sell Price: <gradient:#ff0000:#ff8800>$%price%</gradient> &8(Net: $%net%)",
                        "%price%", String.format("%.2f", sellPrice),
                        "%net%", String.format("%.2f", netPrice)));
            } else {
                lore.add(GradientColor.applyWithVariables("&7Sell Price: <gradient:#ff0000:#ff8800>$%price%</gradient> <gradient:#00ff00:#88ff00>(Tax Exempt)</gradient>",
                        "%price%", String.format("%.2f", sellPrice)));
            }
        } else {
            lore.add(GradientColor.applyWithVariables("&7Sell Price: <gradient:#ff0000:#ff8800>$%price%</gradient>",
                    "%price%", String.format("%.2f", sellPrice)));
        }

        if (plugin.getShopConfig().showPriceTrends()) {
            lore.add("");
            lore.add(formatPriceChange(item.getPriceChangePercent()));
        }

        if (item.getStock() == 0 && plugin.getRestockManager().isRestocking(item.getMaterial())) {
            String countdown = plugin.getRestockManager().getRestockCountdown(item.getMaterial());
            if (countdown != null) {
                lore.add("");
                lore.add(GradientColor.apply("<gradient:#ff0000:#ff8800>Out of Stock for Purchase!</gradient>"));
                lore.add(GradientColor.applyWithVariables("<gradient:#ffff00:#ff8800>Restocking in: %time%</gradient>",
                        "%time%", countdown));
                lore.add("");
                lore.add(GradientColor.apply("&7You can still sell this item to the shop"));
                lore.add(GradientColor.apply("<gradient:#ffff00:#ff8800>Right Click to Sell</gradient>"));
            }
        } else if (item.getStock() == 0) {
            lore.add("");
            lore.add(GradientColor.apply("<gradient:#ff0000:#ff8800>Out of Stock for Purchase!</gradient>"));
            lore.add(GradientColor.apply("&7You can still sell this item to the shop"));
            lore.add(GradientColor.apply("<gradient:#ffff00:#ff8800>Right Click to Sell</gradient>"));
        } else {
            lore.add("");
            lore.add(GradientColor.apply("<gradient:#00ff00:#88ff00>Left Click to Buy</gradient>"));
            lore.add(GradientColor.apply("<gradient:#ffff00:#ff8800>Right Click to Sell</gradient>"));
        }

        meta.setLore(lore);
        display.setItemMeta(meta);

        return display;
    }

    private void setAmountButton(Inventory inv, int slot, Material material, int amount, boolean isBuying, ShopItem item) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();

        String action = isBuying ? "Buy" : "Sell";
        String amountText = amount == -1 ? "All" : String.valueOf(amount);

        meta.setDisplayName(GradientColor.apply("<gradient:#00ff00:#88ff00>" + action + " " + amountText + "</gradient>"));

        List<String> lore = new ArrayList<>();
        if (amount == -1) {
            lore.add(GradientColor.apply("&7Sell all items in inventory"));
        } else if (isBuying) {
            double totalCost = item.getBuyPrice() * amount;
            lore.add(GradientColor.applyWithVariables("&7Total cost: <gradient:#00ff00:#88ff00>$%cost%</gradient>",
                    "%cost%", String.format("%.2f", totalCost)));
        } else {
            double totalEarn = item.getSellPrice() * amount;
            double tax = plugin.getShopConfig().calculateTax(item.getMaterial(), item.getCategory(), totalEarn);
            double netEarn = totalEarn - tax;

            lore.add(GradientColor.applyWithVariables("&7Total earnings: <gradient:#00ff00:#88ff00>$%earn%</gradient>",
                    "%earn%", String.format("%.2f", totalEarn)));
            if (tax > 0) {
                lore.add(GradientColor.applyWithVariables("&8Tax: <gradient:#ff0000:#ff8800>-$%tax%</gradient>",
                        "%tax%", String.format("%.2f", tax)));
                lore.add(GradientColor.applyWithVariables("&7Net earnings: <gradient:#00ff00:#88ff00>$%net%</gradient>",
                        "%net%", String.format("%.2f", netEarn)));
            }
        }

        meta.setLore(lore);
        button.setItemMeta(meta);
        inv.setItem(slot, button);
    }

    private void fillBorders(Inventory inv) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        int size = inv.getSize();
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
            if (inv.getItem(size - 9 + i) == null) inv.setItem(size - 9 + i, filler);
        }

        for (int i = 0; i < size; i += 9) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
            if (inv.getItem(i + 8) == null) inv.setItem(i + 8, filler);
        }
    }

    private void fillBottomBorder(Inventory inv) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        for (int i = 45; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }
    }

    private String formatPriceChange(double percent) {
        if (percent > 0) {
            return GradientColor.applyWithVariables("<gradient:#00ff00:#88ff00>▲ %percent%% Price Increase</gradient>",
                    "%percent%", String.format("%.1f", percent));
        } else if (percent < 0) {
            return GradientColor.applyWithVariables("<gradient:#ff0000:#ff8800>▼ %percent%% Price Decrease</gradient>",
                    "%percent%", String.format("%.1f", Math.abs(percent)));
        } else {
            return GradientColor.apply("&7● Price Stable");
        }
    }

    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        StringBuilder formatted = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : name.toCharArray()) {
            if (c == ' ') {
                formatted.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                formatted.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                formatted.append(c);
            }
        }

        return formatted.toString();
    }

    public String getPlayerCategory(UUID uuid) {
        return playerCategory.get(uuid);
    }

    public Material getPlayerSelectedItem(UUID uuid) {
        return playerSelectedItem.get(uuid);
    }

    public void setPlayerSelectedItem(UUID uuid, Material material) {
        playerSelectedItem.put(uuid, material);
    }

    public int getPlayerPage(UUID uuid) {
        return playerPage.getOrDefault(uuid, 0);
    }

    public void clearPlayerData(UUID uuid) {
        playerCategory.remove(uuid);
        playerSelectedItem.remove(uuid);
        playerPage.remove(uuid);
    }
}