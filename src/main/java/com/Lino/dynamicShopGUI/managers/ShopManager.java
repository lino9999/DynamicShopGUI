package com.Lino.dynamicShopGUI.managers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.models.ShopItem;
import com.Lino.dynamicShopGUI.utils.GradientColor;
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
                        plugin.getShopConfig().getMessage("out-of-stock")));
            }

            double totalCost = item.getBuyPrice() * amount;

            if (!plugin.getEconomy().has(player, totalCost)) {
                return CompletableFuture.completedFuture(new TransactionResult(false,
                        plugin.getShopConfig().getMessage("insufficient-funds",
                                "%price%", String.format("%.2f", totalCost))));
            }

            boolean withdrawSuccess = plugin.getEconomy().withdrawPlayer(player, totalCost).transactionSuccess();
            if (!withdrawSuccess) {
                return CompletableFuture.completedFuture(new TransactionResult(false,
                        "Failed to process payment"));
            }

            ItemStack itemStack = new ItemStack(material, amount);
            if (player.getInventory().firstEmpty() == -1) {
                if (!canFitItems(player, itemStack)) {
                    plugin.getEconomy().depositPlayer(player, totalCost);
                    return CompletableFuture.completedFuture(new TransactionResult(false,
                            "Inventory is full! Transaction cancelled and refunded."));
                }
                player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
            } else {
                player.getInventory().addItem(itemStack);
            }

            item.setStock(item.getStock() - amount);
            item.incrementTransactionsBuy();

            double oldPrice = item.getCurrentPrice();
            double newPrice = calculateNewPrice(item);
            item.setCurrentPrice(newPrice);

            double priceChangePercent = ((newPrice - oldPrice) / oldPrice) * 100;
            item.setPriceChangePercent(priceChangePercent);

            plugin.getDatabaseManager().updateShopItem(item);
            plugin.getDatabaseManager().logTransaction(player.getUniqueId().toString(),
                    material, "BUY", amount, item.getBuyPrice(), totalCost);

            checkPriceAlert(item, oldPrice, newPrice);

            return CompletableFuture.completedFuture(new TransactionResult(true,
                    plugin.getShopConfig().getMessage("buy-success",
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
                        plugin.getShopConfig().getMessage("shop-full")));
            }

            if (!hasItem(player, material, amount)) {
                return CompletableFuture.completedFuture(new TransactionResult(false,
                        plugin.getShopConfig().getMessage("insufficient-items")));
            }

            int canAccept = Math.min(amount, item.getMaxStock() - item.getStock());

            removeItem(player, material, canAccept);

            double totalEarnings = item.getSellPrice() * canAccept;

            double taxAmount = plugin.getShopConfig().calculateTax(material, item.getCategory(), totalEarnings);
            double netEarnings = totalEarnings - taxAmount;

            plugin.getEconomy().depositPlayer(player, netEarnings);

            item.setStock(item.getStock() + canAccept);
            item.incrementTransactionsSell();

            double oldPrice = item.getCurrentPrice();
            double newPrice = calculateNewPrice(item);
            item.setCurrentPrice(newPrice);

            double priceChangePercent = ((newPrice - oldPrice) / oldPrice) * 100;
            item.setPriceChangePercent(priceChangePercent);

            plugin.getDatabaseManager().updateShopItem(item);
            plugin.getDatabaseManager().logTransaction(player.getUniqueId().toString(),
                    material, "SELL", canAccept, item.getSellPrice(), netEarnings);

            checkPriceAlert(item, oldPrice, newPrice);

            String message;
            if (taxAmount > 0) {
                message = plugin.getShopConfig().getMessage("sell-success-with-tax",
                        "%amount%", String.valueOf(canAccept),
                        "%item%", formatMaterialName(material),
                        "%price%", String.format("%.2f", netEarnings),
                        "%tax%", String.format("%.2f", taxAmount));
            } else {
                message = plugin.getShopConfig().getMessage("sell-success",
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

        double priceFactor;

        if (stockRatio <= 0.05) {
            priceFactor = 2.5;
        } else if (stockRatio <= 0.1) {
            priceFactor = 2.0;
        } else if (stockRatio <= 0.2) {
            priceFactor = 1.7;
        } else if (stockRatio <= 0.3) {
            priceFactor = 1.5;
        } else if (stockRatio <= 0.4) {
            priceFactor = 1.3;
        } else if (stockRatio <= 0.5) {
            priceFactor = 1.15;
        } else if (stockRatio <= 0.6) {
            priceFactor = 1.0;
        } else if (stockRatio <= 0.7) {
            priceFactor = 0.9;
        } else if (stockRatio <= 0.8) {
            priceFactor = 0.8;
        } else if (stockRatio <= 0.9) {
            priceFactor = 0.7;
        } else {
            priceFactor = 0.6;
        }

        int recentActivity = item.getTransactionsBuy() + item.getTransactionsSell();
        if (recentActivity > 0) {
            double buyRatio = (double) item.getTransactionsBuy() / recentActivity;
            double momentumFactor = 1.0 + ((buyRatio - 0.5) * 0.1);
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
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                String itemName = formatMaterialName(item.getMaterial());
                String message;
                Sound alertSound = null;

                if (isIncrease) {
                    message = plugin.getShopConfig().getMessage("price-increase-alert",
                            "%item%", itemName,
                            "%percent%", String.format("%.0f", Math.abs(priceChangePercent)),
                            "%price%", String.format("%.2f", newPrice));

                    String soundName = plugin.getShopConfig().getPriceIncreaseSound();
                    if (!soundName.equalsIgnoreCase("none")) {
                        try {
                            alertSound = Sound.valueOf(soundName);
                        } catch (IllegalArgumentException ignored) {}
                    }
                } else {
                    message = plugin.getShopConfig().getMessage("price-decrease-alert",
                            "%item%", itemName,
                            "%percent%", String.format("%.0f", Math.abs(priceChangePercent)),
                            "%price%", String.format("%.2f", newPrice));

                    String soundName = plugin.getShopConfig().getPriceDecreaseSound();
                    if (!soundName.equalsIgnoreCase("none")) {
                        try {
                            alertSound = Sound.valueOf(soundName);
                        } catch (IllegalArgumentException ignored) {}
                    }
                }

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(plugin.getShopConfig().getPrefix() + message);

                    if (alertSound != null) {
                        onlinePlayer.playSound(onlinePlayer.getLocation(), alertSound,
                                plugin.getShopConfig().getSoundVolume(),
                                plugin.getShopConfig().getSoundPitch());
                    }

                    if (plugin.getShopConfig().showTitle()) {
                        String title;
                        String subtitle;

                        if (isIncrease) {
                            title = GradientColor.apply("<gradient:#ff0000:#ff00ff>PRICE SURGE!</gradient>");
                            subtitle = GradientColor.applyWithVariables("<gradient:#ffff00:#ff8800>%item% +%percent%%</gradient>",
                                    "%item%", itemName,
                                    "%percent%", String.format("%.0f", priceChangePercent));
                        } else {
                            title = GradientColor.apply("<gradient:#00ff00:#00ffff>PRICE DROP!</gradient>");
                            subtitle = GradientColor.applyWithVariables("<gradient:#00ff00:#88ff00>%item% %percent%%</gradient>",
                                    "%item%", itemName,
                                    "%percent%", String.format("%.0f", priceChangePercent));
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