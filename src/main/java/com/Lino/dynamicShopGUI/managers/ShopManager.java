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
        this.PRICE_INCREASE_FACTOR = plugin.getShopConfig().getPriceIncreaseFactor();
        this.PRICE_DECREASE_FACTOR = plugin.getShopConfig().getPriceDecreaseFactor();
        this.MIN_PRICE_MULTIPLIER = plugin.getShopConfig().getMinPriceMultiplier();
        this.MAX_PRICE_MULTIPLIER = plugin.getShopConfig().getMaxPriceMultiplier();
    }

    public CompletableFuture<TransactionResult> buyItem(Player player, Material material, int amount) {
        plugin.getLogger().info("ShopManager: Starting buy transaction for " + player.getName() +
                ": " + amount + "x " + material);

        return plugin.getDatabaseManager().getShopItem(material).thenCompose(item -> {
            if (item == null) {
                plugin.getLogger().warning("ShopManager: Item not found in database: " + material);
                return CompletableFuture.completedFuture(
                        new TransactionResult(false, "Item not found in shop"));
            }

            plugin.getLogger().info("ShopManager: Item found - Stock: " + item.getStock() +
                    ", Price: " + item.getBuyPrice());

            if (item.getStock() < amount) {
                plugin.getLogger().info("ShopManager: Insufficient stock. Available: " + item.getStock() +
                        ", Requested: " + amount);
                return CompletableFuture.completedFuture(new TransactionResult(false,
                        String.format("Not enough stock! Available: %d", item.getStock())));
            }

            double totalCost = item.getBuyPrice() * amount;
            plugin.getLogger().info("ShopManager: Total cost: " + totalCost);

            if (!plugin.getEconomy().has(player, totalCost)) {
                plugin.getLogger().info("ShopManager: Player has insufficient funds. Required: " + totalCost +
                        ", Available: " + plugin.getEconomy().getBalance(player));
                return CompletableFuture.completedFuture(new TransactionResult(false,
                        String.format("Insufficient funds! You need $%.2f", totalCost)));
            }

            // Withdraw money
            boolean withdrawSuccess = plugin.getEconomy().withdrawPlayer(player, totalCost).transactionSuccess();
            if (!withdrawSuccess) {
                plugin.getLogger().warning("ShopManager: Failed to withdraw money from player");
                return CompletableFuture.completedFuture(new TransactionResult(false,
                        "Failed to process payment"));
            }

            // Give items to player
            ItemStack itemStack = new ItemStack(material, amount);
            if (player.getInventory().firstEmpty() == -1) {
                // Check if inventory can fit the items
                if (!canFitItems(player, itemStack)) {
                    // Refund the money
                    plugin.getEconomy().depositPlayer(player, totalCost);
                    return CompletableFuture.completedFuture(new TransactionResult(false,
                            "Inventory is full! Transaction cancelled and refunded."));
                }
                player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
            } else {
                player.getInventory().addItem(itemStack);
            }

            // Update item data
            item.setStock(item.getStock() - amount);
            item.incrementTransactionsBuy();

            double oldPrice = item.getCurrentPrice();
            double newPrice = calculateNewPrice(item, true);
            item.setCurrentPrice(newPrice);

            double priceChangePercent = ((newPrice - oldPrice) / oldPrice) * 100;
            item.setPriceChangePercent(priceChangePercent);

            plugin.getLogger().info("ShopManager: Updated item - New stock: " + item.getStock() +
                    ", New price: " + newPrice);

            // Update database
            plugin.getDatabaseManager().updateShopItem(item);
            plugin.getDatabaseManager().logTransaction(player.getUniqueId().toString(),
                    material, "BUY", amount, item.getBuyPrice(), totalCost);

            return CompletableFuture.completedFuture(new TransactionResult(true,
                    String.format("Successfully purchased %dx %s for $%.2f",
                            amount, formatMaterialName(material), totalCost)));
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("ShopManager: Error in buyItem: " + throwable.getMessage());
            throwable.printStackTrace();
            return new TransactionResult(false, "An error occurred during the transaction");
        });
    }

    public CompletableFuture<TransactionResult> sellItem(Player player, Material material, int amount) {
        plugin.getLogger().info("ShopManager: Starting sell transaction for " + player.getName() +
                ": " + amount + "x " + material);

        return plugin.getDatabaseManager().getShopItem(material).thenCompose(item -> {
            if (item == null) {
                plugin.getLogger().warning("ShopManager: Item not found in database: " + material);
                return CompletableFuture.completedFuture(
                        new TransactionResult(false, "This item cannot be sold"));
            }

            plugin.getLogger().info("ShopManager: Item found - Stock: " + item.getStock() +
                    "/" + item.getMaxStock() + ", Sell price: " + item.getSellPrice());

            if (item.getStock() >= item.getMaxStock()) {
                plugin.getLogger().info("ShopManager: Shop is full for this item");
                return CompletableFuture.completedFuture(new TransactionResult(false,
                        "Shop is full of this item! Cannot buy more"));
            }

            if (!hasItem(player, material, amount)) {
                plugin.getLogger().info("ShopManager: Player doesn't have enough items");
                return CompletableFuture.completedFuture(new TransactionResult(false,
                        "You don't have enough of this item"));
            }

            int canAccept = Math.min(amount, item.getMaxStock() - item.getStock());
            plugin.getLogger().info("ShopManager: Can accept " + canAccept + " items");

            removeItem(player, material, canAccept);

            double totalEarnings = item.getSellPrice() * canAccept;
            plugin.getEconomy().depositPlayer(player, totalEarnings);

            // Update item data
            item.setStock(item.getStock() + canAccept);
            item.incrementTransactionsSell();

            double oldPrice = item.getCurrentPrice();
            double newPrice = calculateNewPrice(item, false);
            item.setCurrentPrice(newPrice);

            double priceChangePercent = ((newPrice - oldPrice) / oldPrice) * 100;
            item.setPriceChangePercent(priceChangePercent);

            plugin.getLogger().info("ShopManager: Updated item - New stock: " + item.getStock() +
                    ", New price: " + newPrice + ", Earnings: " + totalEarnings);

            // Update database
            plugin.getDatabaseManager().updateShopItem(item);
            plugin.getDatabaseManager().logTransaction(player.getUniqueId().toString(),
                    material, "SELL", canAccept, item.getSellPrice(), totalEarnings);

            String message = canAccept < amount ?
                    String.format("Shop could only accept %d items. Sold for $%.2f", canAccept, totalEarnings) :
                    String.format("Successfully sold %dx %s for $%.2f", canAccept, formatMaterialName(material), totalEarnings);

            return CompletableFuture.completedFuture(new TransactionResult(true, message));
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("ShopManager: Error in sellItem: " + throwable.getMessage());
            throwable.printStackTrace();
            return new TransactionResult(false, "An error occurred during the transaction");
        });
    }

    private boolean canFitItems(Player player, ItemStack items) {
        // Create a temporary copy of the inventory to test
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

    private double calculateNewPrice(ShopItem item, boolean isBuying) {
        double stockRatio = (double) item.getStock() / item.getMaxStock();

        double priceFactor = 1.0;

        if (stockRatio == 0) {
            priceFactor = 3.0;
        } else if (stockRatio < 0.1) {
            priceFactor = 2.0;
        } else if (stockRatio < 0.3) {
            priceFactor = 1.5;
        } else if (stockRatio < 0.5) {
            priceFactor = 1.2;
        } else if (stockRatio > 0.7) {
            priceFactor = 0.8;
        } else if (stockRatio > 0.9) {
            priceFactor = 0.6;
        }

        if (isBuying && item.getStock() > 0) {
            priceFactor *= (1 + PRICE_INCREASE_FACTOR);
        } else if (!isBuying) {
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
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
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