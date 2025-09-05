package com.Lino.dynamicShopGUI.gui;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.managers.GUIManager;
import com.Lino.dynamicShopGUI.models.ShopItem;
import com.Lino.dynamicShopGUI.utils.GUIUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class TransactionMenuGUI {

    private final DynamicShopGUI plugin;
    private final GUIManager guiManager;

    public TransactionMenuGUI(DynamicShopGUI plugin, GUIManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    public void open(Player player, Material material, boolean isBuying) {
        plugin.getDatabaseManager().getShopItem(material).thenAccept(item -> {
            if (item == null) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(plugin.getShopConfig().getMessage("errors.item-not-found"));
                });
                return;
            }

            String title = isBuying ?
                    plugin.getShopConfig().getMessage("gui.buy-title", "%item%", GUIUtils.formatMaterialName(material)) :
                    plugin.getShopConfig().getMessage("gui.sell-title", "%item%", GUIUtils.formatMaterialName(material));

            Inventory inv = Bukkit.createInventory(null, 54, title);

            setupBorders(inv, isBuying);

            if (item.getStock() == 0 && plugin.getShopConfig().isRestockEnabled()) {
                if (!plugin.getRestockManager().isRestocking(material)) {
                    plugin.getRestockManager().startRestockTimer(material);
                }
            }

            ItemStack displayItem = createDisplayItem(material, item, isBuying);
            inv.setItem(13, displayItem);

            ItemStack glasmorphGlass = new ItemStack(isBuying ? Material.LIME_STAINED_GLASS : Material.RED_STAINED_GLASS);
            ItemMeta glasmorphMeta = glasmorphGlass.getItemMeta();
            glasmorphMeta.setDisplayName(" ");
            glasmorphGlass.setItemMeta(glasmorphMeta);

            inv.setItem(12, glasmorphGlass);
            inv.setItem(14, glasmorphGlass);

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
                    plugin.getShopConfig().getMessage("gui.switch-to-sell") :
                    plugin.getShopConfig().getMessage("gui.switch-to-buy"));
            List<String> toggleLore = new ArrayList<>();
            toggleLore.add("");
            toggleLore.add(plugin.getShopConfig().getMessage("gui.click-to-switch", "%action%", isBuying ? "sell" : "buy"));
            toggleMeta.setLore(toggleLore);
            toggleButton.setItemMeta(toggleMeta);
            inv.setItem(22, toggleButton);

            ItemStack backButton = new ItemStack(Material.ARROW);
            ItemMeta backMeta = backButton.getItemMeta();
            backMeta.setDisplayName(plugin.getShopConfig().getMessage("gui.back"));
            backButton.setItemMeta(backMeta);
            inv.setItem(49, backButton);

            double balance = plugin.getEconomy().getBalance(player);
            ItemStack balanceInfo = new ItemStack(Material.EMERALD);
            ItemMeta balanceMeta = balanceInfo.getItemMeta();
            balanceMeta.setDisplayName(plugin.getShopConfig().getMessage("gui.balance-label"));
            List<String> balanceLore = new ArrayList<>();
            balanceLore.add("");
            balanceLore.add(plugin.getShopConfig().getMessage("gui.balance-current", "%balance%", String.format("%.2f", balance)));
            balanceMeta.setLore(balanceLore);
            balanceInfo.setItemMeta(balanceMeta);
            inv.setItem(40, balanceInfo);

            Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inv));
        });
    }

    private void setupBorders(Inventory inv, boolean isBuying) {
        ItemStack glassFiller = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassFiller.getItemMeta();
        glassMeta.setDisplayName(" ");
        glassFiller.setItemMeta(glassMeta);

        for (int i = 0; i < 54; i++) {
            inv.setItem(i, glassFiller);
        }

        ItemStack ironBorder = new ItemStack(Material.IRON_BLOCK);
        ItemMeta ironMeta = ironBorder.getItemMeta();
        ironMeta.setDisplayName(" ");
        ironBorder.setItemMeta(ironMeta);

        ItemStack coalBorder = new ItemStack(Material.COAL_BLOCK);
        ItemMeta coalMeta = coalBorder.getItemMeta();
        coalMeta.setDisplayName(" ");
        coalBorder.setItemMeta(coalMeta);

        ItemStack redstoneBorder = new ItemStack(isBuying ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
        ItemMeta redstoneMeta = redstoneBorder.getItemMeta();
        redstoneMeta.setDisplayName(" ");
        redstoneBorder.setItemMeta(redstoneMeta);

        ItemStack lapisBorder = new ItemStack(Material.LAPIS_BLOCK);
        ItemMeta lapisMeta = lapisBorder.getItemMeta();
        lapisMeta.setDisplayName(" ");
        lapisBorder.setItemMeta(lapisMeta);

        inv.setItem(0, redstoneBorder);
        inv.setItem(1, ironBorder);
        inv.setItem(2, coalBorder);
        inv.setItem(3, ironBorder);
        inv.setItem(4, lapisBorder);
        inv.setItem(5, ironBorder);
        inv.setItem(6, coalBorder);
        inv.setItem(7, ironBorder);
        inv.setItem(8, redstoneBorder);

        inv.setItem(45, redstoneBorder);
        inv.setItem(46, ironBorder);
        inv.setItem(47, coalBorder);
        inv.setItem(48, glassFiller);
        inv.setItem(49, glassFiller);
        inv.setItem(50, glassFiller);
        inv.setItem(51, coalBorder);
        inv.setItem(52, ironBorder);
        inv.setItem(53, redstoneBorder);

        inv.setItem(9, coalBorder);
        inv.setItem(17, coalBorder);
        inv.setItem(18, ironBorder);
        inv.setItem(26, ironBorder);
        inv.setItem(27, coalBorder);
        inv.setItem(35, coalBorder);
        inv.setItem(36, ironBorder);
        inv.setItem(44, ironBorder);

        ItemStack borderGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderGlass.getItemMeta();
        borderMeta.setDisplayName(" ");
        borderGlass.setItemMeta(borderMeta);

        for (int i = 10; i <= 16; i++) {
            inv.setItem(i, borderGlass);
        }

        inv.setItem(19, borderGlass);
        inv.setItem(20, borderGlass);
        inv.setItem(21, borderGlass);
        inv.setItem(23, borderGlass);
        inv.setItem(24, borderGlass);
        inv.setItem(25, borderGlass);
    }

    private ItemStack createDisplayItem(Material material, ShopItem item, boolean isBuying) {
        ItemStack displayItem = new ItemStack(material);
        ItemMeta displayMeta = displayItem.getItemMeta();
        displayMeta.setDisplayName(plugin.getShopConfig().getMessage("gui.item-name", "%item%",
                GUIUtils.formatMaterialName(material)));
        List<String> displayLore = new ArrayList<>();

        if (plugin.getShopConfig().showStock()) {
            displayLore.add(plugin.getShopConfig().getMessage("gui.stock-display",
                    "%stock%", String.valueOf(item.getStock()),
                    "%max%", String.valueOf(item.getMaxStock())));
        }

        displayLore.add(plugin.getShopConfig().getMessage("gui.buy-price",
                "%price%", String.format("%.2f", item.getBuyPrice())));
        displayLore.add(plugin.getShopConfig().getMessage("gui.sell-price",
                "%price%", String.format("%.2f", item.getSellPrice())));

        if (!isBuying && plugin.getShopConfig().showTaxInfo() && plugin.getShopConfig().isTaxEnabled()) {
            if (!plugin.getShopConfig().isTaxExempt(material)) {
                double taxRate = plugin.getShopConfig().getTaxRate(item.getCategory()) * 100;
                displayLore.add(plugin.getShopConfig().getMessage("gui.tax-rate",
                        "%rate%", String.format("%.1f", taxRate)));
            } else {
                displayLore.add(plugin.getShopConfig().getMessage("gui.tax-exempt"));
            }
        }

        if (plugin.getShopConfig().showPriceTrends()) {
            displayLore.add("");
            displayLore.add(GUIUtils.formatPriceChange(plugin, item.getPriceChangePercent()));
        }

        if (item.getStock() == 0 && plugin.getRestockManager().isRestocking(material)) {
            String countdown = plugin.getRestockManager().getRestockCountdown(material);
            if (countdown != null) {
                displayLore.add("");
                displayLore.add(plugin.getShopConfig().getMessage("restock.out-of-stock"));
                displayLore.add(plugin.getShopConfig().getMessage("restock.countdown", "%time%", countdown));
            }
        }

        displayMeta.setLore(displayLore);
        displayItem.setItemMeta(displayMeta);
        return displayItem;
    }

    private void setAmountButton(Inventory inv, int slot, Material material, int amount, boolean isBuying, ShopItem item) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();

        String amountText = amount == -1 ? "All" : String.valueOf(amount);

        meta.setDisplayName(plugin.getShopConfig().getMessage(isBuying ? "gui.buy-amount" : "gui.sell-amount",
                "%amount%", amountText));

        List<String> lore = new ArrayList<>();
        if (amount == -1) {
            lore.add(plugin.getShopConfig().getMessage("gui.sell-all"));
        } else if (isBuying) {
            double totalCost = item.getBuyPrice() * amount;
            lore.add(plugin.getShopConfig().getMessage("gui.total-cost", "%cost%", String.format("%.2f", totalCost)));
        } else {
            double totalEarn = item.getSellPrice() * amount;
            double tax = plugin.getShopConfig().calculateTax(item.getMaterial(), item.getCategory(), totalEarn);
            double netEarn = totalEarn - tax;

            lore.add(plugin.getShopConfig().getMessage("gui.total-earnings", "%earn%", String.format("%.2f", totalEarn)));
            if (tax > 0) {
                lore.add(plugin.getShopConfig().getMessage("gui.tax-deduction", "%tax%", String.format("%.2f", tax)));
                lore.add(plugin.getShopConfig().getMessage("gui.net-earnings", "%net%", String.format("%.2f", netEarn)));
            }
        }

        meta.setLore(lore);
        button.setItemMeta(meta);
        inv.setItem(slot, button);
    }
}