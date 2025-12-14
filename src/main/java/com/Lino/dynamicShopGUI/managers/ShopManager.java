package com.Lino.dynamicShopGUI.managers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.models.ShopItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class ShopManager {

    private final DynamicShopGUI plugin;
    private final double PRICE_INCREASE_FACTOR;
    private final double PRICE_DECREASE_FACTOR;
    private final double MIN_PRICE_MULTIPLIER;
    private final double MAX_PRICE_MULTIPLIER;

    public ShopManager(DynamicShopGUI plugin) {
        this.plugin = plugin;
        this.PRICE_INCREASE_FACTOR = plugin.getShopConfig().getPriceIncreaseFactor();
        this.PRICE_DECREASE_FACTOR = plugin.getShopConfig().getPriceDecreaseFactor();
        this.MIN_PRICE_MULTIPLIER = plugin.getShopConfig().getMinPriceMultiplier();
        this.MAX_PRICE_MULTIPLIER = plugin.getShopConfig().getMaxPriceMultiplier();
    }

    public void sellHandItem(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            player.sendMessage(plugin.getShopConfig().getMessage("transaction.sell-no-item-hand"));
            return;
        }

        sellItem(player, hand.getType(), hand.getAmount());
    }

    public void sellAllItems(Player player) {
        CompletableFuture.runAsync(() -> {
            Map<Material, Integer> inventoryContents = new HashMap<>();
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    inventoryContents.put(item.getType(), inventoryContents.getOrDefault(item.getType(), 0) + item.getAmount());
                }
            }

            if (inventoryContents.isEmpty()) {
                player.sendMessage(plugin.getShopConfig().getMessage("transaction.sell-nothing"));
                return;
            }

            double totalNetEarnings = 0;
            double totalTax = 0;
            int totalItemsSold = 0;
            List<ShopItem> itemsToUpdate = new ArrayList<>();
            Map<Material, Integer> amountsToRemove = new HashMap<>();

            for (Map.Entry<Material, Integer> entry : inventoryContents.entrySet()) {
                Material material = entry.getKey();
                int amount = entry.getValue();

                ShopItem item = plugin.getDatabaseManager().getShopItem(material).join();
                if (item == null) continue;

                if (item.getStock() >= item.getMaxStock()) continue;

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

                    // FIX: Aggiunto * 0.7 per usare il prezzo di vendita corretto
                    double batchEarnings = tempPrice * currentBatch * 0.7;
                    double batchTax = plugin.getShopConfig().calculateTax(material, item.getCategory(), batchEarnings);

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

                totalNetEarnings += (itemTotalEarnings - itemTotalTax);
                totalTax += itemTotalTax;
                totalItemsSold += canAccept;

                item.setStock(item.getStock() + canAccept);
                item.incrementTransactionsSell();
                item.setCurrentPrice(tempPrice);

                double priceChangePercent = ((tempPrice - oldPrice) / oldPrice) * 100;
                item.setPriceChangePercent(priceChangePercent);

                itemsToUpdate.add(item);
                amountsToRemove.put(material, canAccept);

                plugin.getDatabaseManager().logTransaction(player.getUniqueId().toString(),
                        material, "SELL_ALL", canAccept, itemTotalEarnings / canAccept, itemTotalEarnings - itemTotalTax);
            }

            if (totalItemsSold > 0) {
                double finalEarnings = totalNetEarnings;
                double finalTax = totalTax;
                int finalAmount = totalItemsSold;

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    for (Map.Entry<Material, Integer> remove : amountsToRemove.entrySet()) {
                        removeItem(player, remove.getKey(), remove.getValue());
                    }

                    plugin.getEconomy().depositPlayer(player, finalEarnings);

                    player.sendMessage(plugin.getShopConfig().getMessage("transaction.sell-all-success",
                            "%amount%", String.valueOf(finalAmount),
                            "%price%", String.format("%.2f", finalEarnings),
                            "%tax%", String.format("%.2f", finalTax)));

                    if (plugin.getShopConfig().isSoundEnabled()) {
                        player.playSound(player.getLocation(), "entity.experience_orb.pickup", 0.7f, 1.0f);
                    }
                });

                for (ShopItem updatedItem : itemsToUpdate) {
                    plugin.getDatabaseManager().updateShopItem(updatedItem);
                }

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getItemWorthManager().clearCache();
                });

            } else {
                player.sendMessage(plugin.getShopConfig().getMessage("transaction.sell-nothing"));
            }
        });
    }

    public CompletableFuture<TransactionResult> buyItem(Player player, Material material, int amount) {
        return plugin.getDatabaseManager().getShopItem(material).thenCompose(item -> {
            if (item == null) {
                return CompletableFuture.completedFuture(
                        new TransactionResult(false, "Item not found in shop"));
            }

            if (item.getStock() < amount) {
                return CompletableFuture.completedFuture(new TransactionResult(false,
                        plugin.getShopConfig().getMessage("errors.out-of-stock")));
            }

            double totalCost = 0;
            int tempStock = item.getStock();
            double tempPrice = item.getCurrentPrice();

            for (int i = 0; i < amount; i++) {
                totalCost += tempPrice;
                tempStock--;

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

            if (!plugin.getEconomy().has(player, totalCost)) {
                return CompletableFuture.completedFuture(new TransactionResult(false,
                        plugin.getShopConfig().getMessage("errors.insufficient-funds",
                                "%price%", String.format("%.2f", totalCost))));
            }

            ItemStack itemStack = new ItemStack(material, amount);
            if (!canFitItems(player, itemStack)) {
                return CompletableFuture.completedFuture(new TransactionResult(false,
                        plugin.getShopConfig().getMessage("errors.inventory-full")));
            }

            boolean withdrawSuccess = plugin.getEconomy().withdrawPlayer(player, totalCost).transactionSuccess();
            if (!withdrawSuccess) {
                return CompletableFuture.completedFuture(new TransactionResult(false,
                        "Failed to process payment"));
            }

            player.getInventory().addItem(itemStack);

            int oldStock = item.getStock();
            item.setStock(item.getStock() - amount);
            item.incrementTransactionsBuy();

            double oldPrice = item.getCurrentPrice();
            double newPrice = calculateNewPrice(item);
            item.setCurrentPrice(newPrice);

            double priceChangePercent = ((newPrice - oldPrice) / oldPrice) * 100;
            item.setPriceChangePercent(priceChangePercent);

            plugin.getDatabaseManager().updateShopItem(item);
            plugin.getDatabaseManager().logTransaction(player.getUniqueId().toString(),
                    material, "BUY", amount, totalCost / amount, totalCost);

            plugin.getItemWorthManager().clearCache();

            if (oldStock > 0 && item.getStock() == 0) {
                checkOutOfStockAlert(item);
            }

            double triggerThreshold = plugin.getShopConfig().getRestockTriggerThreshold();
            if (plugin.getShopConfig().isRestockEnabled() && item.getStock() < item.getMaxStock() * triggerThreshold) {
                plugin.getRestockManager().startRestockTimer(material);
            }

            return CompletableFuture.completedFuture(new TransactionResult(true,
                    plugin.getShopConfig().getMessage("transaction.buy-success",
                            "%amount%", String.valueOf(amount),
                            "%item%", formatMaterialName(material),
                            "%price%", String.format("%.2f", totalCost))));
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return new TransactionResult(false, "An error occurred during the transaction");
        });
    }

    public CompletableFuture<TransactionResult> sellItem(Player player, Material material, int amount) {
        return plugin.getDatabaseManager().getShopItem(material).thenCompose(item -> {
            if (item == null) {
                return CompletableFuture.completedFuture(
                        new TransactionResult(false, "This item cannot be sold"));
            }

            if (item.getStock() >= item.getMaxStock()) {
                return CompletableFuture.completedFuture(new TransactionResult(false,
                        plugin.getShopConfig().getMessage("errors.shop-full")));
            }

            if (!hasItem(player, material, amount)) {
                return CompletableFuture.completedFuture(new TransactionResult(false,
                        plugin.getShopConfig().getMessage("errors.insufficient-items")));
            }

            int canAccept = Math.min(amount, item.getMaxStock() - item.getStock());

            removeItem(player, material, canAccept);

            double totalEarnings = 0;
            double totalTax = 0;
            int tempStock = item.getStock();
            double tempPrice = item.getCurrentPrice();
            double oldPrice = tempPrice;

            int batchSize = Math.max(1, canAccept / 10);
            int processed = 0;

            while (processed < canAccept) {
                int currentBatch = Math.min(batchSize, canAccept - processed);

                double batchEarnings = tempPrice * currentBatch * 0.7;
                double batchTax = plugin.getShopConfig().calculateTax(material, item.getCategory(), batchEarnings);

                totalEarnings += batchEarnings;
                totalTax += batchTax;

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

            double netEarnings = totalEarnings - totalTax;

            plugin.getEconomy().depositPlayer(player, netEarnings);

            item.setStock(item.getStock() + canAccept);
            item.incrementTransactionsSell();

            double newPrice = calculateNewPrice(item);
            item.setCurrentPrice(newPrice);

            double priceChangePercent = ((newPrice - oldPrice) / oldPrice) * 100;
            item.setPriceChangePercent(priceChangePercent);

            plugin.getDatabaseManager().updateShopItem(item);
            plugin.getDatabaseManager().logTransaction(player.getUniqueId().toString(),
                    material, "SELL", canAccept, totalEarnings / canAccept, netEarnings);

            plugin.getItemWorthManager().clearCache();

            String message;
            if (totalTax > 0) {
                message = plugin.getShopConfig().getMessage("transaction.sell-success-with-tax",
                        "%amount%", String.valueOf(canAccept),
                        "%item%", formatMaterialName(material),
                        "%price%", String.format("%.2f", netEarnings),
                        "%tax%", String.format("%.2f", totalTax));
            } else {
                message = plugin.getShopConfig().getMessage("transaction.sell-success",
                        "%amount%", String.valueOf(canAccept),
                        "%item%", formatMaterialName(material),
                        "%price%", String.format("%.2f", netEarnings));
            }

            return CompletableFuture.completedFuture(new TransactionResult(true, message));
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return new TransactionResult(false, "An error occurred during the transaction");
        });
    }

    private boolean canFitItems(Player player, ItemStack items) {
        ItemStack[] contents = player.getInventory().getStorageContents().clone();

        int remaining = items.getAmount();
        int maxStackSize = items.getMaxStackSize();

        for (int i = 0; i < contents.length; i++) {
            if (remaining <= 0) break;

            if (contents[i] == null) {
                remaining -= maxStackSize;
            } else if (contents[i].getType() == items.getType() &&
                    contents[i].getAmount() < maxStackSize) {
                remaining -= (maxStackSize - contents[i].getAmount());
            }
        }

        return remaining <= 0;
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

    private boolean hasItem(Player player, Material material, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material && item.getEnchantments().isEmpty()) {
                count += item.getAmount();
                if (count >= amount) return true;
            }
        }
        return false;
    }

    private void removeItem(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material && item.getEnchantments().isEmpty()) {
                int stackAmount = item.getAmount();
                if (stackAmount <= remaining) {
                    player.getInventory().setItem(i, null);
                    remaining -= stackAmount;
                } else {
                    item.setAmount(stackAmount - remaining);
                    remaining = 0;
                }
            }
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

    private void checkOutOfStockAlert(ShopItem item) {
        if (!plugin.getShopConfig().isOutOfStockAlertEnabled()) return;

        plugin.getDiscordManager().sendOutOfStockAlert(item);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            String itemName = formatMaterialName(item.getMaterial());
            String message = plugin.getShopConfig().getMessage("out-of-stock.alert",
                    "%item%", itemName,
                    "%price%", String.format("%.2f", item.getCurrentPrice()));

            Sound alertSound = null;
            String soundName = plugin.getShopConfig().getOutOfStockSound();
            if (!soundName.equalsIgnoreCase("none")) {
                try {
                    alertSound = Sound.valueOf(soundName);
                } catch (IllegalArgumentException ignored) {}
            }

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.sendMessage(plugin.getShopConfig().getPrefix() + message);

                if (alertSound != null) {
                    onlinePlayer.playSound(onlinePlayer.getLocation(), alertSound,
                            plugin.getShopConfig().getOutOfStockSoundVolume(),
                            plugin.getShopConfig().getOutOfStockSoundPitch());
                }

                if (plugin.getShopConfig().showOutOfStockTitle()) {
                    String title = plugin.getShopConfig().getMessage("out-of-stock.title");
                    String subtitle = plugin.getShopConfig().getMessage("out-of-stock.subtitle",
                            "%item%", itemName);

                    int duration = plugin.getShopConfig().getOutOfStockTitleDuration();
                    onlinePlayer.sendTitle(title, subtitle, 10, duration, 20);
                }
            }
        });
    }

    public static class TransactionResult {
        private final boolean success;
        private final String message;

        public TransactionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}