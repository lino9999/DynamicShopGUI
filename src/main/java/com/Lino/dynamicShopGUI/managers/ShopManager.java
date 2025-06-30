package com.Lino.dynamicShopGUI.managers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.models.ShopItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;

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
        return plugin.getDatabaseManager().getShopItem(material).thenCompose(item -> {
            if (item == null) {
                return CompletableFuture.completedFuture(
                        new TransactionResult(false, "Item not found in shop"));
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

            // Withdraw money
            boolean withdrawSuccess = plugin.getEconomy().withdrawPlayer(player, totalCost).transactionSuccess();
            if (!withdrawSuccess) {
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

            // Update item data - FIRST update stock
            item.setStock(item.getStock() - amount);
            item.incrementTransactionsBuy();

            // THEN calculate new price based on new stock level
            double oldPrice = item.getCurrentPrice();
            double newPrice = calculateNewPrice(item);
            item.setCurrentPrice(newPrice);

            double priceChangePercent = ((newPrice - oldPrice) / oldPrice) * 100;
            item.setPriceChangePercent(priceChangePercent);

            // Update database
            plugin.getDatabaseManager().updateShopItem(item);
            plugin.getDatabaseManager().logTransaction(player.getUniqueId().toString(),
                    material, "BUY", amount, item.getBuyPrice(), totalCost);

            // Check for price alert
            checkPriceAlert(item, oldPrice, newPrice);

            return CompletableFuture.completedFuture(new TransactionResult(true,
                    String.format("Successfully purchased %dx %s for $%.2f",
                            amount, formatMaterialName(material), totalCost)));
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
                        "Shop is full of this item! Cannot buy more"));
            }

            if (!hasItem(player, material, amount)) {
                return CompletableFuture.completedFuture(new TransactionResult(false,
                        "You don't have enough of this item"));
            }

            int canAccept = Math.min(amount, item.getMaxStock() - item.getStock());

            removeItem(player, material, canAccept);

            double totalEarnings = item.getSellPrice() * canAccept;

            // Calculate and apply tax
            double taxAmount = plugin.getShopConfig().calculateTax(material, item.getCategory(), totalEarnings);
            double netEarnings = totalEarnings - taxAmount;

            plugin.getEconomy().depositPlayer(player, netEarnings);

            // Update item data - FIRST update stock
            item.setStock(item.getStock() + canAccept);
            item.incrementTransactionsSell();

            // THEN calculate new price based on new stock level
            double oldPrice = item.getCurrentPrice();
            double newPrice = calculateNewPrice(item);
            item.setCurrentPrice(newPrice);

            double priceChangePercent = ((newPrice - oldPrice) / oldPrice) * 100;
            item.setPriceChangePercent(priceChangePercent);

            // Update database - log the actual amount player received (after tax)
            plugin.getDatabaseManager().updateShopItem(item);
            plugin.getDatabaseManager().logTransaction(player.getUniqueId().toString(),
                    material, "SELL", canAccept, item.getSellPrice(), netEarnings);

            // Check for price alert
            checkPriceAlert(item, oldPrice, newPrice);

            // Create appropriate message based on tax
            String message;
            if (taxAmount > 0) {
                message = canAccept < amount ?
                        String.format("Shop could only accept %d items. Sold for $%.2f (Tax: $%.2f)",
                                canAccept, netEarnings, taxAmount) :
                        String.format("Successfully sold %dx %s for $%.2f (Tax: $%.2f)",
                                canAccept, formatMaterialName(material), netEarnings, taxAmount);
            } else {
                message = canAccept < amount ?
                        String.format("Shop could only accept %d items. Sold for $%.2f", canAccept, netEarnings) :
                        String.format("Successfully sold %dx %s for $%.2f",
                                canAccept, formatMaterialName(material), netEarnings);
            }

            return CompletableFuture.completedFuture(new TransactionResult(true, message));
        }).exceptionally(throwable -> {
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

    private double calculateNewPrice(ShopItem item) {
        double stockRatio = (double) item.getStock() / item.getMaxStock();

        // Base price multiplier based on stock level
        // Low stock = high prices, high stock = low prices
        double priceFactor;

        if (stockRatio <= 0.05) {           // 0-5% stock
            priceFactor = 2.5;
        } else if (stockRatio <= 0.1) {    // 5-10% stock
            priceFactor = 2.0;
        } else if (stockRatio <= 0.2) {    // 10-20% stock
            priceFactor = 1.7;
        } else if (stockRatio <= 0.3) {    // 20-30% stock
            priceFactor = 1.5;
        } else if (stockRatio <= 0.4) {    // 30-40% stock
            priceFactor = 1.3;
        } else if (stockRatio <= 0.5) {    // 40-50% stock
            priceFactor = 1.15;
        } else if (stockRatio <= 0.6) {    // 50-60% stock
            priceFactor = 1.0;
        } else if (stockRatio <= 0.7) {    // 60-70% stock
            priceFactor = 0.9;
        } else if (stockRatio <= 0.8) {    // 70-80% stock
            priceFactor = 0.8;
        } else if (stockRatio <= 0.9) {    // 80-90% stock
            priceFactor = 0.7;
        } else {                            // 90-100% stock
            priceFactor = 0.6;
        }

        // Apply transaction momentum (slight additional change based on recent activity)
        int recentActivity = item.getTransactionsBuy() + item.getTransactionsSell();
        if (recentActivity > 0) {
            double buyRatio = (double) item.getTransactionsBuy() / recentActivity;
            // If more people are buying than selling, increase price slightly
            double momentumFactor = 1.0 + ((buyRatio - 0.5) * 0.1);
            priceFactor *= momentumFactor;
        }

        double newPrice = item.getBasePrice() * priceFactor;

        // Apply min/max limits
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

    private void checkPriceAlert(ShopItem item, double oldPrice, double newPrice) {
        if (!plugin.getShopConfig().isPriceAlertsEnabled()) return;

        double priceChangePercent = ((newPrice - oldPrice) / oldPrice) * 100;

        boolean shouldAlert = false;
        boolean isIncrease = priceChangePercent > 0;

        if (priceChangePercent >= plugin.getShopConfig().getPriceIncreaseThreshold()) {
            shouldAlert = true;
        } else if (priceChangePercent <= plugin.getShopConfig().getPriceDecreaseThreshold()) {
            shouldAlert = true;
        }

        if (shouldAlert) {
            // Run alert on main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                String itemName = formatMaterialName(item.getMaterial());
                String message;
                Sound alertSound = null;

                if (isIncrease) {
                    message = plugin.getShopConfig().getMessage("price-increase-alert")
                            .replace("%item%", itemName)
                            .replace("%percent%", String.format("%.0f", Math.abs(priceChangePercent)))
                            .replace("%price%", String.format("%.2f", newPrice));

                    String soundName = plugin.getShopConfig().getPriceIncreaseSound();
                    if (!soundName.equalsIgnoreCase("none")) {
                        try {
                            alertSound = Sound.valueOf(soundName);
                        } catch (IllegalArgumentException ignored) {}
                    }
                } else {
                    message = plugin.getShopConfig().getMessage("price-decrease-alert")
                            .replace("%item%", itemName)
                            .replace("%percent%", String.format("%.0f", Math.abs(priceChangePercent)))
                            .replace("%price%", String.format("%.2f", newPrice));

                    String soundName = plugin.getShopConfig().getPriceDecreaseSound();
                    if (!soundName.equalsIgnoreCase("none")) {
                        try {
                            alertSound = Sound.valueOf(soundName);
                        } catch (IllegalArgumentException ignored) {}
                    }
                }

                // Broadcast message to all players
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', message));

                    // Play sound if configured
                    if (alertSound != null) {
                        onlinePlayer.playSound(onlinePlayer.getLocation(), alertSound,
                                plugin.getShopConfig().getSoundVolume(),
                                plugin.getShopConfig().getSoundPitch());
                    }

                    // Show title if configured
                    if (plugin.getShopConfig().showTitle()) {
                        String title;
                        String subtitle;

                        if (isIncrease) {
                            title = ChatColor.RED + "" + ChatColor.BOLD + "PRICE SURGE!";
                            subtitle = ChatColor.YELLOW + itemName + " +" + String.format("%.0f%%", priceChangePercent);
                        } else {
                            title = ChatColor.GREEN + "" + ChatColor.BOLD + "PRICE DROP!";
                            subtitle = ChatColor.YELLOW + itemName + " " + String.format("%.0f%%", priceChangePercent);
                        }

                        int duration = plugin.getShopConfig().getTitleDuration();
                        onlinePlayer.sendTitle(title, subtitle, 10, duration, 20);
                    }
                }
            });
        }
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