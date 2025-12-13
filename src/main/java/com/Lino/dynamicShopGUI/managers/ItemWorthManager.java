package com.Lino.dynamicShopGUI.managers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.models.ShopItem;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ItemWorthManager {

    private final DynamicShopGUI plugin;
    private final Map<Material, CachedItem> itemCache = new ConcurrentHashMap<>();
    private final Map<Material, Boolean> sellableCache = new ConcurrentHashMap<>();
    private BukkitTask cacheUpdateTask;

    private static class CachedItem {
        final double sellPrice;
        final String category;

        CachedItem(double sellPrice, String category) {
            this.sellPrice = sellPrice;
            this.category = category;
        }
    }

    public ItemWorthManager(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("item-worth.enabled", true)) {
            return;
        }

        if (plugin.getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            return;
        }

        refreshCache();

        int interval = plugin.getConfig().getInt("item-worth.update-interval", 5) * 20;
        cacheUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                refreshCache();
            }
        }.runTaskTimerAsynchronously(plugin, interval, interval);

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                plugin,
                ListenerPriority.HIGH,
                PacketType.Play.Server.WINDOW_ITEMS,
                PacketType.Play.Server.SET_SLOT
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.getPlayer() == null) return;

                if (plugin.getConfig().getBoolean("item-worth.disable-in-creative", true) &&
                        event.getPlayer().getGameMode() == org.bukkit.GameMode.CREATIVE) {
                    return;
                }

                PacketContainer packet = event.getPacket();

                if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
                    ItemStack item = packet.getItemModifier().read(0);
                    if (item != null && item.getType() != Material.AIR) {
                        packet.getItemModifier().write(0, addWorthLore(item));
                    }
                } else if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
                    List<ItemStack> items = packet.getItemListModifier().read(0);
                    if (items != null) {
                        List<ItemStack> newItems = new ArrayList<>();
                        for (ItemStack item : items) {
                            if (item != null && item.getType() != Material.AIR) {
                                newItems.add(addWorthLore(item));
                            } else {
                                newItems.add(item);
                            }
                        }
                        packet.getItemListModifier().write(0, newItems);
                    }
                }
            }
        });
    }

    public void stop() {
        if (cacheUpdateTask != null) {
            cacheUpdateTask.cancel();
            cacheUpdateTask = null;
        }
        ProtocolLibrary.getProtocolManager().removePacketListeners(plugin);
        clearCache();
    }

    private ItemStack addWorthLore(ItemStack originalItem) {
        ItemStack item = originalItem.clone();
        Material material = item.getType();

        List<String> excludedItems = plugin.getConfig().getStringList("item-worth.excluded-items");
        if (excludedItems.contains(material.name())) {
            return item;
        }

        if (isCustomItem(item)) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        boolean hasContentValue = false;

        if (meta instanceof BlockStateMeta) {
            BlockStateMeta blockStateMeta = (BlockStateMeta) meta;
            if (blockStateMeta.getBlockState() instanceof ShulkerBox) {
                double shulkerValue = calculateShulkerValue(blockStateMeta);
                if (shulkerValue > 0) {
                    hasContentValue = true;
                    String shulkerLine = plugin.getShopConfig().getMessage("item-worth.shulker-contents",
                            "%value%", String.format("%.2f", shulkerValue));
                    lore.add(shulkerLine);
                }
            }
        }

        Boolean isSellable = sellableCache.get(material);
        CachedItem cachedItem = itemCache.get(material);

        String notSellableText = plugin.getShopConfig().getMessage("item-worth.not-sellable");
        if (notSellableText == null) notSellableText = "Not Sellable";

        if (isSellable == null || !isSellable) {
            // MOSTRA "Not Sellable" SOLO SE NON HA CONTENUTO DI VALORE
            if (!hasContentValue) {
                lore.add(notSellableText);
            }
        } else if (cachedItem != null) {
            int amount = item.getAmount();
            double grossValue = cachedItem.sellPrice * amount;
            double tax = plugin.getShopConfig().calculateTax(material, cachedItem.category, grossValue);
            double netValue = grossValue - tax;

            if (amount > 1) {
                String stackLine = plugin.getShopConfig().getMessage("item-worth.worth-stack",
                        "%value%", String.format("%.2f", netValue));
                lore.add(stackLine);
            } else {
                String unitLine = plugin.getShopConfig().getMessage("item-worth.worth-unit",
                        "%value%", String.format("%.2f", netValue));
                lore.add(unitLine);
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private double calculateShulkerValue(BlockStateMeta meta) {
        double totalNetValue = 0.0;
        BlockState state = meta.getBlockState();

        if (state instanceof ShulkerBox) {
            ShulkerBox shulker = (ShulkerBox) state;
            for (ItemStack content : shulker.getInventory().getContents()) {
                if (content != null && content.getType() != Material.AIR) {
                    CachedItem cachedContent = itemCache.get(content.getType());
                    if (cachedContent != null) {
                        double contentGross = cachedContent.sellPrice * content.getAmount();
                        double contentTax = plugin.getShopConfig().calculateTax(content.getType(), cachedContent.category, contentGross);
                        totalNetValue += (contentGross - contentTax);
                    }
                }
            }
        }
        return totalNetValue;
    }

    private void refreshCache() {
        Map<String, com.Lino.dynamicShopGUI.config.CategoryConfigLoader.CategoryConfig> categories = plugin.getShopConfig().getAllCategories();

        for (var category : categories.values()) {
            for (Material material : category.getItems().keySet()) {
                try {
                    ShopItem shopItem = plugin.getDatabaseManager().getShopItem(material).join();
                    if (shopItem != null) {
                        itemCache.put(material, new CachedItem(shopItem.getSellPrice(), shopItem.getCategory()));
                        sellableCache.put(material, true);
                    } else {
                        sellableCache.put(material, false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void clearCache() {
        if (plugin.isEnabled()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    refreshCache();
                }
            }.runTaskAsynchronously(plugin);
        }
    }

    private boolean isCustomItem(ItemStack item) {
        if (!plugin.getConfig().getBoolean("item-worth.skip-custom-items", true)) {
            return false;
        }
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        if (meta.hasDisplayName()) return true;
        if (meta.hasEnchants()) return true;

        return false;
    }
}