package com.Lino.dynamicShopGUI.managers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.config.CategoryConfigLoader;
import com.Lino.dynamicShopGUI.models.ShopItem;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RestockManager {

    private final DynamicShopGUI plugin;
    private final Map<Material, Long> restockTimers;
    private BukkitTask masterTask;
    private BukkitTask decayTask;
    private long nextDecayTime = 0;

    public RestockManager(DynamicShopGUI plugin) {
        this.plugin = plugin;
        this.restockTimers = new ConcurrentHashMap<>();
        startMasterTask();
        startDecayTask();
    }

    private void startMasterTask() {
        masterTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getShopConfig().isRestockEnabled() || restockTimers.isEmpty()) return;

                long now = System.currentTimeMillis();
                Iterator<Map.Entry<Material, Long>> iterator = restockTimers.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<Material, Long> entry = iterator.next();
                    if (now >= entry.getValue()) {
                        Material material = entry.getKey();
                        performRestock(material);
                        iterator.remove();
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void startDecayTask() {
        boolean enabled = plugin.getConfig().getBoolean("stock-decay.enabled", false);
        if (!enabled) return;

        long intervalTicks = plugin.getConfig().getLong("stock-decay.interval", 60) * 60 * 20L;
        long intervalMillis = intervalTicks * 50;

        nextDecayTime = System.currentTimeMillis() + intervalMillis;

        decayTask = new BukkitRunnable() {
            @Override
            public void run() {
                performDecayCheck();
                nextDecayTime = System.currentTimeMillis() + intervalMillis;
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    private void performDecayCheck() {
        double triggerThreshold = plugin.getConfig().getDouble("stock-decay.trigger-threshold", 0.99);
        double targetPercentage = plugin.getConfig().getDouble("stock-decay.target-percentage", 0.50);

        Map<String, CategoryConfigLoader.CategoryConfig> categories = plugin.getShopConfig().getAllCategories();

        for (CategoryConfigLoader.CategoryConfig category : categories.values()) {
            for (Material material : category.getItems().keySet()) {
                plugin.getDatabaseManager().getShopItem(material).thenAccept(item -> {
                    if (item != null) {
                        double currentRatio = (double) item.getStock() / item.getMaxStock();

                        if (currentRatio >= triggerThreshold) {
                            int newStock = (int) (item.getMaxStock() * targetPercentage);

                            newStock = Math.max(newStock, item.getMinStock());

                            if (newStock < item.getStock()) {
                                item.setStock(newStock);

                                double newPrice = calculateNewPrice(item);
                                item.setCurrentPrice(newPrice);

                                double priceChangePercent = ((newPrice - item.getBasePrice()) / item.getBasePrice()) * 100;
                                item.setPriceChangePercent(priceChangePercent);

                                plugin.getDatabaseManager().updateShopItem(item);

                                plugin.getLogger().info("Decayed stock for " + material.name() + " from " + item.getStock() + " to " + newStock);
                            }
                        }
                    }
                });
            }
        }
    }

    public void startRestockTimer(Material material) {
        if (!plugin.getShopConfig().isRestockEnabled()) return;

        if (restockTimers.containsKey(material)) return;

        long restockTime = plugin.getShopConfig().getRestockTime() * 60L * 1000L;
        long endTime = System.currentTimeMillis() + restockTime;

        restockTimers.put(material, endTime);
    }

    public void stopRestockTimer(Material material) {
        restockTimers.remove(material);
    }

    public String getRestockCountdown(Material material) {
        if (!restockTimers.containsKey(material)) {
            return null;
        }

        long endTime = restockTimers.get(material);
        long currentTime = System.currentTimeMillis();
        long remainingTime = endTime - currentTime;

        return formatDuration(remainingTime);
    }

    public String getDecayCountdown() {
        if (!plugin.getShopConfig().isStockDecayEnabled() || nextDecayTime == 0) {
            return null;
        }
        long remainingTime = nextDecayTime - System.currentTimeMillis();
        return formatDuration(remainingTime);
    }

    private String formatDuration(long remainingMillis) {
        if (remainingMillis <= 0) {
            return "0s";
        }

        long seconds = remainingMillis / 1000;
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

    private void performRestock(Material material) {
        plugin.getDatabaseManager().getShopItem(material).thenAccept(item -> {
            if (item != null) {
                int restockPercentage = plugin.getShopConfig().getRestockPercentage();
                int maxStock = item.getMaxStock();
                int restockAmount = (maxStock * restockPercentage) / 100;

                item.setStock(restockAmount);

                double newPrice = calculateNewPrice(item);
                item.setCurrentPrice(newPrice);

                double priceChangePercent = ((newPrice - item.getBasePrice()) / item.getBasePrice()) * 100;
                item.setPriceChangePercent(priceChangePercent);

                plugin.getDatabaseManager().updateShopItem(item);

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (plugin.getItemWorthManager() != null) {
                        plugin.getItemWorthManager().clearCache();
                    }
                    refreshOpenGUIs(material);
                });
            }
        });
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

    private void refreshOpenGUIs(Material material) {
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
    }

    public void shutdown() {
        if (masterTask != null && !masterTask.isCancelled()) {
            masterTask.cancel();
        }
        if (decayTask != null && !decayTask.isCancelled()) {
            decayTask.cancel();
        }
        restockTimers.clear();
    }
}