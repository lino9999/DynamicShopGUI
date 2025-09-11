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

    public AutoSellChestManager(DynamicShopGUI plugin) {
        this.plugin = plugin;
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
        }.runTaskLater(plugin, 5 * 60 * 20L); // 5 minutes

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

        for (ItemStack item : contents) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            ShopItem shopItem = plugin.getDatabaseManager().getShopItem(item.getType()).join();
            if (shopItem != null) {
                int amount = item.getAmount();
                double sellPrice = shopItem.getSellPrice();
                double tax = plugin.getShopConfig().calculateTax(item.getType(), shopItem.getCategory(), sellPrice * amount);
                double earnings = (sellPrice * amount) - tax;

                if (earnings > 0) {
                    plugin.getEconomy().depositPlayer(owner, earnings);
                    totalSold += earnings;

                    shopItem.setStock(shopItem.getStock() + amount);
                    double newPrice = calculateNewPrice(shopItem);
                    shopItem.setCurrentPrice(newPrice);
                    plugin.getDatabaseManager().updateShopItem(shopItem);
                }
            }
        }

        chest.getInventory().clear();

        if (owner.isOnline() && totalSold > 0) {
            owner.getPlayer().sendMessage(plugin.getShopConfig().getMessage("autosell.success", "%value%", String.format("%.2f", totalSold)));
        }
    }

    private double calculateNewPrice(ShopItem item) {
        double stockRatio = (double) item.getStock() / item.getMaxStock();
        double newPrice;

        if (stockRatio > 0.95) {
            newPrice = item.getBasePrice() * plugin.getShopConfig().getMinPriceMultiplier();
        } else if (stockRatio > 0.75) {
            newPrice = item.getCurrentPrice() * (1 - plugin.getShopConfig().getPriceDecreaseFactor());
        } else {
            newPrice = item.getCurrentPrice() * (1 - plugin.getShopConfig().getPriceDecreaseFactor() / 2);
        }

        double minPrice = item.getBasePrice() * plugin.getShopConfig().getMinPriceMultiplier();
        return Math.max(minPrice, newPrice);
    }

    private void loadChests() {
        autoSellChests.putAll(plugin.getDatabaseManager().loadAutoSellChests().join());
    }

    public void shutdown() {
        activeTimers.values().forEach(BukkitTask::cancel);
        activeTimers.clear();
    }
}
