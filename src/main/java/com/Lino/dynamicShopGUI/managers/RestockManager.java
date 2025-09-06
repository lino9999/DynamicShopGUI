package com.Lino.dynamicShopGUI.managers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.models.ShopItem;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class RestockManager {

    private final DynamicShopGUI plugin;
    private final Map<Material, RestockTask> restockTasks;
    private final Map<Material, Long> restockTimers;

    public RestockManager(DynamicShopGUI plugin) {
        this.plugin = plugin;
        this.restockTasks = new ConcurrentHashMap<>();
        this.restockTimers = new ConcurrentHashMap<>();
    }

    public void startRestockTimer(Material material) {
        if (!plugin.getShopConfig().isRestockEnabled()) return;

        if (restockTasks.containsKey(material)) {
            restockTasks.get(material).cancel();
            restockTasks.remove(material);
        }

        long restockTime = plugin.getShopConfig().getRestockTime() * 60L * 20L;
        long endTime = System.currentTimeMillis() + (restockTime * 50);
        restockTimers.put(material, endTime);

        RestockTask task = new RestockTask(material);
        task.runTaskTimer(plugin, 20L, 20L);
        restockTasks.put(material, task);
    }

    public void stopRestockTimer(Material material) {
        if (restockTasks.containsKey(material)) {
            restockTasks.get(material).cancel();
            restockTasks.remove(material);
        }
        restockTimers.remove(material);
    }

    public String getRestockCountdown(Material material) {
        if (!restockTimers.containsKey(material)) {
            return null;
        }

        long endTime = restockTimers.get(material);
        long currentTime = System.currentTimeMillis();
        long remainingTime = endTime - currentTime;

        if (remainingTime <= 0) {
            return "Restocking...";
        }

        long seconds = remainingTime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    public boolean isRestocking(Material material) {
        return restockTimers.containsKey(material);
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

    private class RestockTask extends BukkitRunnable {
        private final Material material;

        public RestockTask(Material material) {
            this.material = material;
        }

        @Override
        public void run() {
            Long endTime = restockTimers.get(material);
            if (endTime == null) {
                cancel();
                return;
            }

            if (System.currentTimeMillis() >= endTime) {
                plugin.getDatabaseManager().getShopItem(material).thenAccept(item -> {
                    if (item != null) {
                        double oldPrice = item.getCurrentPrice();

                        int restockPercentage = plugin.getShopConfig().getRestockPercentage();
                        int maxStock = item.getMaxStock();
                        int restockAmount = (maxStock * restockPercentage) / 100;

                        item.setStock(restockAmount);

                        double newPrice = calculateNewPrice(item);
                        item.setCurrentPrice(newPrice);

                        double priceChangePercent = ((newPrice - item.getBasePrice()) / item.getBasePrice()) * 100;
                        double transactionPriceChange = ((newPrice - oldPrice) / oldPrice) * 100;
                        item.setPriceChangePercent(priceChangePercent);

                        plugin.getDatabaseManager().updateShopItem(item);

                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (plugin.getItemWorthManager() != null) {
                                plugin.getItemWorthManager().clearCache();
                            }

                            checkRestockPriceAlert(item, oldPrice, newPrice, transactionPriceChange);

                            plugin.getServer().getOnlinePlayers().forEach(player -> {
                                if (player.getOpenInventory() != null) {
                                    GUIManager.GUIType guiType = plugin.getGUIManager().getPlayerGUIType(player.getUniqueId());
                                    if (guiType != null) {
                                        Material selectedItem = plugin.getGUIManager().getPlayerSelectedItem(player.getUniqueId());
                                        if (selectedItem == material && (guiType == GUIManager.GUIType.TRANSACTION_BUY || guiType == GUIManager.GUIType.TRANSACTION_SELL)) {
                                            boolean isBuying = guiType == GUIManager.GUIType.TRANSACTION_BUY;
                                            plugin.getGUIManager().openTransactionMenu(player, material, isBuying);
                                        } else if (guiType == GUIManager.GUIType.CATEGORY_MENU) {
                                            String category = plugin.getGUIManager().getPlayerCategory(player.getUniqueId());
                                            if (category != null) {
                                                int page = plugin.getGUIManager().getPlayerPage(player.getUniqueId());
                                                plugin.getGUIManager().openCategoryMenu(player, category, page);
                                            }
                                        }
                                    }
                                }
                            });
                        });
                    }
                });

                restockTimers.remove(material);
                restockTasks.remove(material);
                cancel();
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

            double minPrice = item.getBasePrice() * plugin.getShopConfig().getMinPriceMultiplier();
            double maxPrice = item.getBasePrice() * plugin.getShopConfig().getMaxPriceMultiplier();

            return Math.max(minPrice, Math.min(maxPrice, newPrice));
        }

        private void checkRestockPriceAlert(ShopItem item, double oldPrice, double newPrice, double transactionPriceChange) {
            if (!plugin.getShopConfig().isPriceAlertsEnabled()) return;

            if (Math.abs(transactionPriceChange) >= plugin.getShopConfig().getPriceIncreaseThreshold()) {
                String itemName = formatMaterialName(item.getMaterial());
                String message = plugin.getShopConfig().getMessage("price-alerts.decrease",
                        "%item%", itemName,
                        "%percent%", String.format("%.0f", Math.abs(transactionPriceChange)),
                        "%price%", String.format("%.2f", newPrice));

                Sound alertSound = null;
                String soundName = plugin.getShopConfig().getPriceDecreaseSound();
                if (!soundName.equalsIgnoreCase("none")) {
                    try {
                        for (Sound sound : Sound.values()) {
                            if (sound.name().equals(soundName)) {
                                alertSound = sound;
                                break;
                            }
                        }
                    } catch (Exception ignored) {}
                }

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(plugin.getShopConfig().getPrefix() + message);

                    if (alertSound != null) {
                        onlinePlayer.playSound(onlinePlayer.getLocation(), alertSound,
                                plugin.getShopConfig().getSoundVolume(),
                                plugin.getShopConfig().getSoundPitch());
                    }

                    if (plugin.getShopConfig().showTitle()) {
                        String title = plugin.getShopConfig().getMessage("price-alerts.title-decrease");
                        String subtitle = plugin.getShopConfig().getMessage("price-alerts.subtitle-decrease",
                                "%item%", itemName,
                                "%percent%", String.format("%.0f", transactionPriceChange));

                        int duration = plugin.getShopConfig().getTitleDuration();
                        onlinePlayer.sendTitle(title, subtitle, 10, duration, 20);
                    }
                }
            }
        }
    }

    public void shutdown() {
        for (RestockTask task : restockTasks.values()) {
            task.cancel();
        }
        restockTasks.clear();
        restockTimers.clear();
    }
}