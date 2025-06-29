package com.Lino.dynamicShopGUI.managers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.models.ShopItem;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

        // Cancel any existing task
        if (restockTasks.containsKey(material)) {
            restockTasks.get(material).cancel();
            restockTasks.remove(material);
        }

        long restockTime = plugin.getShopConfig().getRestockTime() * 60L * 20L; // Convert minutes to ticks
        long endTime = System.currentTimeMillis() + (restockTime * 50); // Convert ticks to milliseconds
        restockTimers.put(material, endTime);

        RestockTask task = new RestockTask(material);
        task.runTaskTimer(plugin, 20L, 20L); // Run every second
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
                // Perform restock
                plugin.getDatabaseManager().getShopItem(material).thenAccept(item -> {
                    if (item != null) {
                        int restockPercentage = plugin.getShopConfig().getRestockPercentage();
                        int initialStock = plugin.getShopConfig().getInitialStock();
                        int restockAmount = (initialStock * restockPercentage) / 100;

                        item.setStock(restockAmount);
                        plugin.getDatabaseManager().updateShopItem(item);

                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            // Update any open GUIs
                            plugin.getServer().getOnlinePlayers().forEach(player -> {
                                if (player.getOpenInventory() != null) {
                                    String title = player.getOpenInventory().getTitle();
                                    if (title.contains("Shop") || title.contains("Buy") || title.contains("Sell")) {
                                        // Refresh the GUI
                                        Material selectedItem = plugin.getGUIManager().getPlayerSelectedItem(player.getUniqueId());
                                        if (selectedItem == material) {
                                            boolean isBuying = title.contains("Buy");
                                            plugin.getGUIManager().openTransactionMenu(player, material, isBuying);
                                        } else {
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

                // Remove from tracking
                restockTimers.remove(material);
                restockTasks.remove(material);
                cancel();
            }
        }
    }

    public void shutdown() {
        // Cancel all tasks when plugin disables
        for (RestockTask task : restockTasks.values()) {
            task.cancel();
        }
        restockTasks.clear();
        restockTimers.clear();
    }
}