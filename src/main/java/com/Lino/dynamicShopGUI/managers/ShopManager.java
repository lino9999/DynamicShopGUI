package com.Lino.dynamicShopGUI.managers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.models.ShopItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.concurrent.CompletableFuture;

public class ShopManager {

    private final DynamicShopGUI plugin;
    private final double PRICE_INCREASE_FACTOR;
    private final double PRICE_DECREASE_FACTOR;
    private final double MIN_PRICE_MULTIPLIER;
    private final double MAX_PRICE_MULTIPLIER;

    public ShopManager(DynamicShopGUI plugin) {
        this.plugin = plugin;
        this.PRICE_INCREASE_FACTOR = plugin.getConfig().getDouble("price-factors.increase", 0.05);
        this.PRICE_DECREASE_FACTOR = plugin.getConfig().getDouble("price-factors.decrease", 0.03);
        this.MIN_PRICE_MULTIPLIER = plugin.getConfig().getDouble("price-limits.min-multiplier", 0.1);
        this.MAX_PRICE_MULTIPLIER = plugin.getConfig().getDouble("price-limits.max-multiplier", 10.0);
    }

    public CompletableFuture<TransactionResult> buyItem(Player player, Material material, int amount) {
        return plugin.getDatabaseManager().getShopItem(material).thenCompose(item -> {
            if (item == null) {
                return CompletableFuture.completedFuture(new TransactionResult(false, "Item not found in shop"));
            }

            if (item.getStock() < amount) {
                return CompletableFuture.completedFuture(new TransactionResult(false,
                        String.format("Not enough stock! Available: %d", item.getStock())));
            }

            double totalCost = item.getBuyPrice() * amount;

            if (!plugin.getEconomy().has(player, totalCost)) {
                return CompletableFuture.completedFuture(new TransactionResult(false,
                        String.format("Insufficient funds! You need $%.2f", totalCost)));
            }

            plugin.getEconomy().withdrawPlayer(player, totalCost);

            ItemStack itemStack = new ItemStack(material, amount);
            if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
            } else {
                player.getInventory().addItem(itemStack);
            }

            item.setStock(item.getStock() - amount);
            item.incrementTransactionsBuy();

            double oldPrice = item.getCurrentPrice();
            double newPrice = calculateNewPrice(item, true);
            item.setCurrentPrice(newPrice);

            double priceChangePercent = ((newPrice - oldPrice) / oldPrice) * 100;
            item.setPriceChangePercent(priceChangePercent);

            plugin.getDatabaseManager().updateShopItem(item);
            plugin.getDatabaseManager().logTransaction(player.getUniqueId().toString(),
                    material, "BUY", amount, item.getBuyPrice(), totalCost);

            return CompletableFuture.completedFuture(new TransactionResult(true,
                    String.format("Successfully purchased %dx %s for $%.2f",
                            amount, formatMaterialName(material), totalCost)));
        });
    }

    public CompletableFuture<TransactionResult> sellItem(Player player, Material material, int amount) {
        return plugin.getDatabaseManager().getShopItem(material).thenCompose(item -> {
            if (item == null) {
                return CompletableFuture.completedFuture(new TransactionResult(false, "This item cannot be sold"));
            }

            if (item.getStock() >= item.getMaxStock()) {
                return CompletableFuture.completedFuture(new TransactionResult(false,
                        "Shop is full of this item! Cannot buy more"));
            }

            if (!hasItem(player, material, amount)) {
                return CompletableFuture.completedFuture(new TransactionResult(false,
                        "You don't have enough of this item"));
            }

            int canAccept = Math.min(amount, item.getMaxStock() - item.getStock());
            removeItem(player, material, canAccept);

            double totalEarnings = item.getSellPrice() * canAccept;
            plugin.getEconomy().depositPlayer(player, totalEarnings);

            item.setStock(item.getStock() + canAccept);
            item.incrementTransactionsSell();

            double oldPrice = item.getCurrentPrice();
            double newPrice = calculateNewPrice(item, false);
            item.setCurrentPrice(newPrice);

            double priceChangePercent = ((newPrice - oldPrice) / oldPrice) * 100;
            item.setPriceChangePercent(priceChangePercent);

            plugin.getDatabaseManager().updateShopItem(item);
            plugin.getDatabaseManager().logTransaction(player.getUniqueId().toString(),
                    material, "SELL", canAccept, item.getSellPrice(), totalEarnings);

            String message = canAccept < amount ?
                    String.format("Shop could only accept %d items. Sold for $%.2f", canAccept, totalEarnings) :
                    String.format("Successfully sold %dx %s for $%.2f", canAccept, formatMaterialName(material), totalEarnings);

            return CompletableFuture.completedFuture(new TransactionResult(true, message));
        });
    }

    private double calculateNewPrice(ShopItem item, boolean isBuying) {
        double stockRatio = (double) item.getStock() / item.getMaxStock();
        double demandFactor = (double) item.getTransactionsBuy() / Math.max(1, item.getTransactionsSell());

        double priceFactor = 1.0;

        if (stockRatio < 0.2) {
            priceFactor = 2.0 - (stockRatio * 5);
        } else if (stockRatio < 0.5) {
            priceFactor = 1.2;
        } else if (stockRatio > 0.8) {
            priceFactor = 0.8;
        }

        if (demandFactor > 2.0) {
            priceFactor *= 1.2;
        } else if (demandFactor < 0.5) {
            priceFactor *= 0.8;
        }

        if (isBuying) {
            priceFactor *= (1 + PRICE_INCREASE_FACTOR);
        } else {
            priceFactor *= (1 - PRICE_DECREASE_FACTOR);
        }

        double newPrice = item.getBasePrice() * priceFactor;

        double minPrice = item.getBasePrice() * MIN_PRICE_MULTIPLIER;
        double maxPrice = item.getBasePrice() * MAX_PRICE_MULTIPLIER;

        return Math.max(minPrice, Math.min(maxPrice, newPrice));
    }

    private boolean hasItem(Player player, Material material, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
                if (count >= amount) return true;
            }
        }
        return false;
    }

    private void removeItem(Player player, Material material, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                int stackAmount = item.getAmount();
                if (stackAmount > remaining) {
                    item.setAmount(stackAmount - remaining);
                    break;
                } else {
                    player.getInventory().remove(item);
                    remaining -= stackAmount;
                    if (remaining <= 0) break;
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