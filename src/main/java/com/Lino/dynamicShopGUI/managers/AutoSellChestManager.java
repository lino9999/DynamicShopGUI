package com.Lino.dynamicShopGUI.managers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.models.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AutoSellChestManager {

    private final DynamicShopGUI plugin;
    private final Map<Location, UUID> autoSellChests = new ConcurrentHashMap<>();
    private final Map<Location, BukkitTask> activeTimers = new HashMap<>();

    private final double MIN_PRICE_MULTIPLIER;
    private final double MAX_PRICE_MULTIPLIER;

    public AutoSellChestManager(DynamicShopGUI plugin) {
        this.plugin = plugin;
        this.MIN_PRICE_MULTIPLIER = plugin.getShopConfig().getMinPriceMultiplier();
        this.MAX_PRICE_MULTIPLIER = plugin.getShopConfig().getMaxPriceMultiplier();
        loadChests();
    }

    public void addChest(Location location, UUID owner) {
        autoSellChests.put(location, owner);
        plugin.getDatabaseManager().saveAutoSellChest(location, owner);
    }

    public void removeChest(Location location) {
        autoSellChests.remove(location);
        plugin.getDatabaseManager().removeAutoSellChest(location);
        if (activeTimers.containsKey(location)) {
            activeTimers.get(location).cancel();
            activeTimers.remove(location);
        }
    }

    public boolean isAutoSellChest(Location location) {
        return autoSellChests.containsKey(location);
    }

    public UUID getChestOwner(Location location) {
        return autoSellChests.get(location);
    }

    public void startSellTimer(Location location) {
        if (activeTimers.containsKey(location)) {
            return;
        }

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                sellContents(location);
                activeTimers.remove(location);
            }
        }.runTaskLater(plugin, 5 * 60 * 20L);

        activeTimers.put(location, task);
    }

    private void sellContents(Location location) {
        if (!isAutoSellChest(location)) {
            return;
        }

        Block block = location.getBlock();
        if (!(block.getState() instanceof Chest)) {
            removeChest(location);
            return;
        }

        Chest chest = (Chest) block.getState();
        ItemStack[] contents = chest.getInventory().getContents();
        UUID ownerUUID = getChestOwner(location);

        if (ownerUUID == null) {
            return;
        }
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);

        double totalSold = 0.0;
        boolean itemsSold = false;

        for (ItemStack itemStack : contents) {
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                continue;
            }

            ShopItem item = plugin.getDatabaseManager().getShopItem(itemStack.getType()).join();
            if (item == null) continue;

            if (item.getStock() >= item.getMaxStock()) continue;

            int amount = itemStack.getAmount();
            int canAccept = Math.min(amount, item.getMaxStock() - item.getStock());

            if (canAccept <= 0) continue;

            double itemTotalEarnings = 0;
            double itemTotalTax = 0;
            int tempStock = item.getStock();
            double tempPrice = item.getCurrentPrice();
            double oldPrice = tempPrice;

            int batchSize = Math.max(1, canAccept / 10);
            int processed = 0;

            while (processed < canAccept) {
                int currentBatch = Math.min(batchSize, canAccept - processed);

                double batchEarnings = tempPrice * currentBatch * 0.7;
                double batchTax = plugin.getShopConfig().calculateTax(itemStack.getType(), item.getCategory(), batchEarnings);

                itemTotalEarnings += batchEarnings;
                itemTotalTax += batchTax;

                tempStock += currentBatch;
                processed += currentBatch;

                ShopItem tempItem = new ShopItem(
                        item.getMaterial(),
                        item.getCategory(),
                        item.getBasePrice(),
                        tempPrice,
                        tempStock,
                        item.getMinStock(),
                        item.getMaxStock(),
                        item.getTransactionsBuy(),
                        item.getTransactionsSell(),
                        0
                );
                tempPrice = calculateNewPrice(tempItem);
            }

            double netEarnings = itemTotalEarnings - itemTotalTax;

            if (netEarnings > 0) {
                plugin.getEconomy().depositPlayer(owner, netEarnings);
                totalSold += netEarnings;

                item.setStock(item.getStock() + canAccept);
                item.setCurrentPrice(tempPrice);

                double priceChangePercent = ((tempPrice - oldPrice) / oldPrice) * 100;
                item.setPriceChangePercent(priceChangePercent);

                plugin.getDatabaseManager().updateShopItem(item);
                plugin.getDatabaseManager().logTransaction(ownerUUID.toString(),
                        itemStack.getType(), "AUTOSELL", canAccept, itemTotalEarnings / canAccept, netEarnings);

                if (canAccept >= amount) {
                    chest.getInventory().remove(itemStack);
                } else {
                    itemStack.setAmount(amount - canAccept);
                }

                itemsSold = true;
            }
        }

        if (itemsSold) {
            plugin.getItemWorthManager().clearCache();
        }

        if (owner.isOnline() && totalSold > 0) {
            owner.getPlayer().sendMessage(plugin.getShopConfig().getMessage("autosell.success", "%value%", String.format("%.2f", totalSold)));
        }
    }

    private double calculateNewPrice(ShopItem item) {
        double stockRatio = (double) item.getStock() / item.getMaxStock();
        int maxStock = item.getMaxStock();

        double basePriceFactor;
        double smoothingFactor = Math.min(1.0, 100.0 / maxStock);

        if (stockRatio < 0.01) {
            basePriceFactor = 2.5;
        } else if (stockRatio < 0.05) {
            double t = (stockRatio - 0.01) / 0.04;
            basePriceFactor = 2.5 - (t * 0.3);
        } else if (stockRatio < 0.1) {
            double t = (stockRatio - 0.05) / 0.05;
            basePriceFactor = 2.2 - (t * 0.2);
        } else if (stockRatio < 0.3) {
            double t = (stockRatio - 0.1) / 0.2;
            basePriceFactor = 2.0 - (t * 0.5);
        } else if (stockRatio < 0.5) {
            double t = (stockRatio - 0.3) / 0.2;
            basePriceFactor = 1.5 - (t * 0.3);
        } else if (stockRatio < 0.7) {
            double t = (stockRatio - 0.5) / 0.2;
            basePriceFactor = 1.2 - (t * 0.1);
        } else if (stockRatio < 0.9) {
            double t = (stockRatio - 0.7) / 0.2;
            basePriceFactor = 1.1 - (t * 0.05);
        } else if (stockRatio < 0.95) {
            double t = (stockRatio - 0.9) / 0.05;
            basePriceFactor = 1.05 - (t * 0.03);
        } else if (stockRatio < 0.99) {
            double t = (stockRatio - 0.95) / 0.04;
            basePriceFactor = 1.02 - (t * 0.015);
        } else {
            double t = (stockRatio - 0.99) / 0.01;
            basePriceFactor = 1.005 - (t * 0.005);
        }

        double priceFactor = basePriceFactor;

        if (maxStock > 1000) {
            double scaleFactor = Math.log10(maxStock / 1000.0) + 1.0;
            double adjustment = (basePriceFactor - 1.0) / scaleFactor;
            priceFactor = 1.0 + adjustment;
        }

        int recentActivity = item.getTransactionsBuy() + item.getTransactionsSell();
        if (recentActivity > 0) {
            double buyRatio = (double) item.getTransactionsBuy() / recentActivity;
            double momentumFactor = 1.0 + ((buyRatio - 0.5) * 0.05 * smoothingFactor);
            priceFactor *= momentumFactor;
        }

        double newPrice = item.getBasePrice() * priceFactor;

        double minPrice = item.getBasePrice() * MIN_PRICE_MULTIPLIER;
        double maxPrice = item.getBasePrice() * MAX_PRICE_MULTIPLIER;

        return Math.max(minPrice, Math.min(maxPrice, newPrice));
    }

    private void loadChests() {
        autoSellChests.putAll(plugin.getDatabaseManager().loadAutoSellChests().join());
    }

    public void shutdown() {
        activeTimers.values().forEach(BukkitTask::cancel);
        activeTimers.clear();
    }
}