package com.Lino.dynamicShopGUI.managers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.config.CategoryConfigLoader;
import com.Lino.dynamicShopGUI.models.ShopItem;
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

        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "Dynamic Shop");

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
        centerMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "DYNAMIC SHOP");
        List<String> centerLore = new ArrayList<>();
        centerLore.add("");
        centerLore.add(ChatColor.GRAY + "Welcome to the Dynamic Shop!");
        centerLore.add(ChatColor.GRAY + "Prices change based on supply & demand");
        centerLore.add("");
        centerLore.add(ChatColor.YELLOW + "Categories: " + ChatColor.WHITE + categories.size());
        centerLore.add(ChatColor.YELLOW + "Tax Enabled: " + ChatColor.WHITE + (plugin.getShopConfig().isTaxEnabled() ? "Yes" : "No"));
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
            meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + category.getDisplayName());

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Click to browse items");
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "Items: " + ChatColor.GRAY + category.getItems().size());
            lore.add(ChatColor.DARK_GRAY + "Tax Rate: " + ChatColor.GRAY + category.getTaxRate() + "%");
            lore.add("");
            lore.add(ChatColor.YELLOW + "» Click to open");

            meta.setLore(lore);
            categoryItem.setItemMeta(meta);

            inv.setItem(categorySlots[slotIndex], categoryItem);
            slotIndex++;
        }

        ItemStack playerInfo = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta playerMeta = playerInfo.getItemMeta();
        playerMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + player.getName());
        List<String> playerLore = new ArrayList<>();
        playerLore.add("");
        playerLore.add(ChatColor.GRAY + "Balance: " + ChatColor.GREEN + "$" + String.format("%.2f", plugin.getEconomy().getBalance(player)));
        playerLore.add("");
        playerLore.add(ChatColor.DARK_GRAY + "Happy shopping!");
        playerMeta.setLore(playerLore);
        playerInfo.setItemMeta(playerMeta);
        inv.setItem(49, playerInfo);

        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Close");
        closeButton.setItemMeta(closeMeta);
        inv.setItem(50, closeButton);

        ItemStack infoBook = new ItemStack(Material.BOOK);
        ItemMeta bookMeta = infoBook.getItemMeta();
        bookMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Shop Guide");
        List<String> bookLore = new ArrayList<>();
        bookLore.add("");
        bookLore.add(ChatColor.GRAY + "• Left click to buy items");
        bookLore.add(ChatColor.GRAY + "• Right click to sell items");
        bookLore.add(ChatColor.GRAY + "• Prices change dynamically");
        bookLore.add(ChatColor.GRAY + "• Low stock = Higher prices");
        bookLore.add(ChatColor.GRAY + "• High stock = Lower prices");
        bookMeta.setLore(bookLore);
        infoBook.setItemMeta(bookMeta);
        inv.setItem(48, infoBook);

        player.openInventory(inv);
    }

    public void openCategoryMenu(Player player, String category, int page) {
        playerCategory.put(player.getUniqueId(), category);
        playerPage.put(player.getUniqueId(), page);

        String displayName = plugin.getShopConfig().getCategoryDisplayName(category.toLowerCase());
        String title = ChatColor.DARK_GREEN + "Shop - " + displayName;
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
                prevMeta.setDisplayName(ChatColor.YELLOW + "« Previous Page");
                List<String> prevLore = new ArrayList<>();
                prevLore.add(ChatColor.GRAY + "Page " + finalPage + "/" + totalPages);
                prevMeta.setLore(prevLore);
                prevPage.setItemMeta(prevMeta);
                inv.setItem(48, prevPage);
            }

            ItemStack backButton = new ItemStack(Material.ARROW);
            ItemMeta backMeta = backButton.getItemMeta();
            backMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Back to Categories");
            backButton.setItemMeta(backMeta);
            inv.setItem(49, backButton);

            if (finalPage < totalPages - 1) {
                ItemStack nextPage = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = nextPage.getItemMeta();
                nextMeta.setDisplayName(ChatColor.YELLOW + "Next Page »");
                List<String> nextLore = new ArrayList<>();
                nextLore.add(ChatColor.GRAY + "Page " + (finalPage + 2) + "/" + totalPages);
                nextMeta.setLore(nextLore);
                nextPage.setItemMeta(nextMeta);
                inv.setItem(50, nextPage);
            }

            if (totalPages > 1) {
                ItemStack pageInfo = new ItemStack(Material.PAPER);
                ItemMeta pageMeta = pageInfo.getItemMeta();
                pageMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Page " + (finalPage + 1) + "/" + totalPages);
                List<String> pageLore = new ArrayList<>();
                pageLore.add(ChatColor.GRAY + "Total items: " + sortedItems.size());
                pageLore.add(ChatColor.GRAY + "Items per page: " + itemsPerPage);
                pageMeta.setLore(pageLore);
                pageInfo.setItemMeta(pageMeta);
                inv.setItem(52, pageInfo);
            }

            ItemStack categoryInfo = new ItemStack(plugin.getShopConfig().getCategoryIcon(category));
            ItemMeta categoryMeta = categoryInfo.getItemMeta();
            categoryMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + displayName);
            List<String> categoryLore = new ArrayList<>();
            categoryLore.add("");
            categoryLore.add(ChatColor.GRAY + "Category Tax: " + ChatColor.YELLOW + String.format("%.1f", plugin.getShopConfig().getTaxRate(category) * 100) + "%");
            categoryLore.add(ChatColor.GRAY + "Total Items: " + ChatColor.YELLOW + sortedItems.size());
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
                    player.sendMessage(ChatColor.RED + "This item is not available in the shop!");
                });
                return;
            }

            String title = isBuying ?
                    ChatColor.DARK_GREEN + "Buy " + formatMaterialName(material) :
                    ChatColor.DARK_RED + "Sell " + formatMaterialName(material);

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
            displayMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + formatMaterialName(material));
            List<String> displayLore = new ArrayList<>();

            if (plugin.getShopConfig().showStock()) {
                displayLore.add(ChatColor.GRAY + "Stock: " + ChatColor.WHITE + item.getStock() + "/" + item.getMaxStock());
            }

            displayLore.add(ChatColor.GRAY + "Buy Price: " + ChatColor.GREEN + "$" + String.format("%.2f", item.getBuyPrice()));
            displayLore.add(ChatColor.GRAY + "Sell Price: " + ChatColor.RED + "$" + String.format("%.2f", item.getSellPrice()));

            if (!isBuying && plugin.getShopConfig().showTaxInfo() && plugin.getShopConfig().isTaxEnabled()) {
                if (!plugin.getShopConfig().isTaxExempt(material)) {
                    double taxRate = plugin.getShopConfig().getTaxRate(item.getCategory()) * 100;
                    displayLore.add(ChatColor.DARK_GRAY + "Tax Rate: " + ChatColor.GRAY + String.format("%.1f%%", taxRate));
                } else {
                    displayLore.add(ChatColor.DARK_GRAY + "Tax: " + ChatColor.GREEN + "Exempt");
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
                    displayLore.add(ChatColor.RED + "Out of Stock!");
                    displayLore.add(ChatColor.YELLOW + "Restocking in: " + ChatColor.GOLD + countdown);
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
            toggleMeta.setDisplayName(isBuying ? ChatColor.GOLD + "" + ChatColor.BOLD + "» Switch to Sell" : ChatColor.GOLD + "" + ChatColor.BOLD + "» Switch to Buy");
            List<String> toggleLore = new ArrayList<>();
            toggleLore.add("");
            toggleLore.add(ChatColor.GRAY + "Click to " + (isBuying ? "sell" : "buy") + " instead");
            toggleMeta.setLore(toggleLore);
            toggleButton.setItemMeta(toggleMeta);
            inv.setItem(22, toggleButton);

            ItemStack backButton = new ItemStack(Material.ARROW);
            ItemMeta backMeta = backButton.getItemMeta();
            backMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "« Back");
            backButton.setItemMeta(backMeta);
            inv.setItem(49, backButton);

            ItemStack balanceInfo = new ItemStack(Material.EMERALD);
            ItemMeta balanceMeta = balanceInfo.getItemMeta();
            balanceMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Your Balance");
            List<String> balanceLore = new ArrayList<>();
            balanceLore.add("");
            balanceLore.add(ChatColor.GRAY + "Current: " + ChatColor.GREEN + "$" + String.format("%.2f", plugin.getEconomy().getBalance(player)));
            balanceMeta.setLore(balanceLore);
            balanceInfo.setItemMeta(balanceMeta);
            inv.setItem(4, balanceInfo);

            Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inv));
        });
    }

    private ItemStack createShopItemDisplay(ShopItem item) {
        ItemStack display = new ItemStack(item.getMaterial());
        ItemMeta meta = display.getItemMeta();

        meta.setDisplayName(ChatColor.YELLOW + formatMaterialName(item.getMaterial()));

        List<String> lore = new ArrayList<>();

        if (plugin.getShopConfig().showStock()) {
            lore.add(ChatColor.GRAY + "Stock: " + ChatColor.WHITE + item.getStock() + "/" + item.getMaxStock());
        }

        lore.add("");
        lore.add(ChatColor.GRAY + "Buy Price: " + ChatColor.GREEN + "$" + String.format("%.2f", item.getBuyPrice()));

        double sellPrice = item.getSellPrice();
        if (plugin.getShopConfig().showTaxInfo() && plugin.getShopConfig().isTaxEnabled()) {
            double tax = plugin.getShopConfig().calculateTax(item.getMaterial(), item.getCategory(), sellPrice);
            double netPrice = sellPrice - tax;

            if (tax > 0) {
                lore.add(ChatColor.GRAY + "Sell Price: " + ChatColor.RED + "$" + String.format("%.2f", sellPrice) +
                        ChatColor.DARK_GRAY + " (Net: $" + String.format("%.2f", netPrice) + ")");
            } else {
                lore.add(ChatColor.GRAY + "Sell Price: " + ChatColor.RED + "$" + String.format("%.2f", sellPrice) +
                        ChatColor.GREEN + " (Tax Exempt)");
            }
        } else {
            lore.add(ChatColor.GRAY + "Sell Price: " + ChatColor.RED + "$" + String.format("%.2f", sellPrice));
        }

        if (plugin.getShopConfig().showPriceTrends()) {
            lore.add("");
            lore.add(formatPriceChange(item.getPriceChangePercent()));
        }

        if (item.getStock() == 0 && plugin.getRestockManager().isRestocking(item.getMaterial())) {
            String countdown = plugin.getRestockManager().getRestockCountdown(item.getMaterial());
            if (countdown != null) {
                lore.add("");
                lore.add(ChatColor.RED + "Out of Stock for Purchase!");
                lore.add(ChatColor.YELLOW + "Restocking in: " + ChatColor.GOLD + countdown);
                lore.add("");
                lore.add(ChatColor.GRAY + "You can still sell this item to the shop");
                lore.add(ChatColor.YELLOW + "Right Click to Sell");
            }
        } else if (item.getStock() == 0) {
            lore.add("");
            lore.add(ChatColor.RED + "Out of Stock for Purchase!");
            lore.add(ChatColor.GRAY + "You can still sell this item to the shop");
            lore.add(ChatColor.YELLOW + "Right Click to Sell");
        } else {
            lore.add("");
            lore.add(ChatColor.YELLOW + "Left Click to Buy");
            lore.add(ChatColor.YELLOW + "Right Click to Sell");
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

        meta.setDisplayName(ChatColor.GREEN + action + " " + amountText);

        List<String> lore = new ArrayList<>();
        if (amount == -1) {
            lore.add(ChatColor.GRAY + "Sell all items in inventory");
        } else if (isBuying) {
            double totalCost = item.getBuyPrice() * amount;
            lore.add(ChatColor.GRAY + "Total cost: " + ChatColor.GREEN + "$" + String.format("%.2f", totalCost));
        } else {
            double totalEarn = item.getSellPrice() * amount;
            double tax = plugin.getShopConfig().calculateTax(item.getMaterial(), item.getCategory(), totalEarn);
            double netEarn = totalEarn - tax;

            lore.add(ChatColor.GRAY + "Total earnings: " + ChatColor.GREEN + "$" + String.format("%.2f", totalEarn));
            if (tax > 0) {
                lore.add(ChatColor.DARK_GRAY + "Tax: " + ChatColor.RED + "-$" + String.format("%.2f", tax));
                lore.add(ChatColor.GRAY + "Net earnings: " + ChatColor.GREEN + "$" + String.format("%.2f", netEarn));
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
            return ChatColor.GREEN + "▲ " + String.format("%.1f%%", percent) + " Price Increase";
        } else if (percent < 0) {
            return ChatColor.RED + "▼ " + String.format("%.1f%%", Math.abs(percent)) + " Price Decrease";
        } else {
            return ChatColor.GRAY + "● Price Stable";
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