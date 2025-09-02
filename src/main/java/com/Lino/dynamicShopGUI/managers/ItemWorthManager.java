package com.Lino.dynamicShopGUI.managers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.models.ShopItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ItemWorthManager {

    private final DynamicShopGUI plugin;
    private BukkitTask updateTask;
    private int playerIndex = 0;
    private final Map<Material, Double> priceCache = new HashMap<>();
    private final Map<Material, Boolean> sellableCache = new HashMap<>();
    private long lastCacheUpdate = 0;
    private final long CACHE_DURATION = 2000;

    public ItemWorthManager(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("item-worth.enabled", true)) {
            return;
        }

        int interval = plugin.getConfig().getInt("item-worth.update-interval", 5) * 20;

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updatePlayerInventories();
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void updatePlayerInventories() {
        List<Player> onlinePlayers = new ArrayList<>(plugin.getServer().getOnlinePlayers());

        if (onlinePlayers.isEmpty()) {
            return;
        }

        int maxPlayersPerUpdate = plugin.getConfig().getInt("item-worth.max-players-per-update", 5);
        int playersToProcess = Math.min(maxPlayersPerUpdate, onlinePlayers.size());

        if (System.currentTimeMillis() - lastCacheUpdate > CACHE_DURATION) {
            priceCache.clear();
            sellableCache.clear();
            lastCacheUpdate = System.currentTimeMillis();
        }

        for (int i = 0; i < playersToProcess; i++) {
            if (playerIndex >= onlinePlayers.size()) {
                playerIndex = 0;
            }

            Player player = onlinePlayers.get(playerIndex);
            playerIndex++;

            if (player != null && player.isOnline()) {
                updatePlayerInventory(player);
            }
        }
    }

    private void updatePlayerInventory(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        boolean updated = false;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            if (updateItemLore(item)) {
                updated = true;
            }
        }

        if (updated) {
            player.updateInventory();
        }
    }

    private boolean updateItemLore(ItemStack item) {
        Material material = item.getType();

        List<String> excludedItems = plugin.getConfig().getStringList("item-worth.excluded-items");
        if (excludedItems.contains(material.name())) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        } else {
            lore = new ArrayList<>(lore);
        }

        String worthPrefix = plugin.getShopConfig().getMessage("item-worth.worth-prefix");
        String notSellableText = plugin.getShopConfig().getMessage("item-worth.not-sellable");

        if (worthPrefix == null) worthPrefix = "Worth: ";
        if (notSellableText == null) notSellableText = "Not Sellable";

        Iterator<String> iterator = lore.iterator();
        while (iterator.hasNext()) {
            String line = org.bukkit.ChatColor.stripColor(iterator.next());
            if (line.startsWith(org.bukkit.ChatColor.stripColor(worthPrefix.substring(0, Math.min(worthPrefix.length(), 6)))) ||
                    line.contains(org.bukkit.ChatColor.stripColor(notSellableText))) {
                iterator.remove();
            }
        }

        if (!sellableCache.containsKey(material)) {
            CompletableFuture<ShopItem> future = plugin.getDatabaseManager().getShopItem(material);
            ShopItem shopItem = future.join();

            if (shopItem != null) {
                sellableCache.put(material, true);
                priceCache.put(material, shopItem.getSellPrice());
            } else {
                sellableCache.put(material, false);
            }
        }

        Boolean isSellable = sellableCache.get(material);
        if (isSellable == null || !isSellable) {
            lore.add(notSellableText);
        } else {
            Double price = priceCache.get(material);
            if (price != null) {
                String displayMode = plugin.getConfig().getString("item-worth.display-mode", "both");

                if (displayMode.equalsIgnoreCase("total")) {
                    String worthLine = plugin.getShopConfig().getMessage("item-worth.worth-total",
                            "%amount%", String.format("%.2f", price * item.getAmount()));
                    lore.add(worthLine);
                } else if (displayMode.equalsIgnoreCase("each")) {
                    String worthLine = plugin.getShopConfig().getMessage("item-worth.worth-each",
                            "%each%", String.format("%.2f", price));
                    lore.add(worthLine);
                } else {
                    String worthLine = plugin.getShopConfig().getMessage("item-worth.worth-format",
                            "%amount%", String.format("%.2f", price * item.getAmount()),
                            "%each%", String.format("%.2f", price));
                    lore.add(worthLine);
                }
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        return true;
    }

    public void updateSingleItem(ItemStack item) {
        if (item != null && item.getType() != Material.AIR) {
            updateItemLore(item);
        }
    }

    public void clearCache() {
        priceCache.clear();
        sellableCache.clear();
        lastCacheUpdate = 0;
    }
}