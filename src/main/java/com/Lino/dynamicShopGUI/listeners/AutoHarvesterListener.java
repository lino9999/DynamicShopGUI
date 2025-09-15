package com.Lino.dynamicShopGUI.listeners;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.models.ShopItem;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class AutoHarvesterListener implements Listener {

    private final DynamicShopGUI plugin;
    private final Map<Material, Material> cropToSeed = new HashMap<>();
    private final Map<Material, Material> cropToItem = new HashMap<>();

    public AutoHarvesterListener(DynamicShopGUI plugin) {
        this.plugin = plugin;
        initializeCropMappings();
    }

    private void initializeCropMappings() {
        cropToSeed.put(Material.WHEAT, Material.WHEAT_SEEDS);
        cropToSeed.put(Material.CARROTS, Material.CARROT);
        cropToSeed.put(Material.POTATOES, Material.POTATO);
        cropToSeed.put(Material.BEETROOTS, Material.BEETROOT_SEEDS);
        cropToSeed.put(Material.NETHER_WART, Material.NETHER_WART);

        cropToItem.put(Material.WHEAT, Material.WHEAT);
        cropToItem.put(Material.CARROTS, Material.CARROT);
        cropToItem.put(Material.POTATOES, Material.POTATO);
        cropToItem.put(Material.BEETROOTS, Material.BEETROOT);
        cropToItem.put(Material.NETHER_WART, Material.NETHER_WART);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isAutoHarvesterHoe(item)) return;

        Block block = event.getBlock();
        Material blockType = block.getType();

        if (!cropToSeed.containsKey(blockType)) return;

        BlockData blockData = block.getBlockData();
        if (!(blockData instanceof Ageable)) return;

        Ageable ageable = (Ageable) blockData;

        boolean isFullyGrown = false;
        if (blockType == Material.NETHER_WART) {
            isFullyGrown = (ageable.getAge() == 3);
        } else {
            isFullyGrown = (ageable.getAge() == ageable.getMaximumAge());
        }

        if (!isFullyGrown) {
            event.setCancelled(true);
            player.playSound(block.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            String notGrownMessage = plugin.getShopConfig().getMessage("auto-harvester.not-fully-grown");
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(notGrownMessage));
            return;
        }

        event.setCancelled(true);

        Material dropMaterial = cropToItem.get(blockType);
        int dropAmount = calculateDropAmount(blockType);

        processAutoHarvest(player, block, blockType, dropMaterial, dropAmount);
    }

    private boolean isAutoHarvesterHoe(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;

        String typeName = item.getType().name();
        if (!typeName.endsWith("_HOE")) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;

        List<String> lore = meta.getLore();
        for (String line : lore) {
            if (ChatColor.stripColor(line).contains("auto-harvester-hoe")) {
                return true;
            }
        }
        return false;
    }

    private int calculateDropAmount(Material cropType) {
        switch (cropType) {
            case WHEAT:
                return 1 + (int)(Math.random() * 3);
            case CARROTS:
                return 1 + (int)(Math.random() * 4);
            case POTATOES:
                return 1 + (int)(Math.random() * 4);
            case BEETROOTS:
                return 1;
            case NETHER_WART:
                return 2 + (int)(Math.random() * 3);
            default:
                return 1;
        }
    }

    private void processAutoHarvest(Player player, Block block, Material cropType, Material dropMaterial, int amount) {
        plugin.getDatabaseManager().getShopItem(dropMaterial).thenAccept(shopItem -> {
            if (shopItem == null) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    block.setType(Material.AIR);
                    block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(dropMaterial, amount));
                    player.sendMessage(plugin.getShopConfig().getMessage("auto-harvester.not-sellable",
                            "%item%", formatMaterialName(dropMaterial)));
                });
                return;
            }

            double sellPrice = shopItem.getSellPrice() * amount;
            double tax = plugin.getShopConfig().calculateTax(dropMaterial, shopItem.getCategory(), sellPrice);
            double netEarnings = sellPrice - tax;

            plugin.getEconomy().depositPlayer(player, netEarnings);

            shopItem.setStock(Math.min(shopItem.getStock() + amount, shopItem.getMaxStock()));
            shopItem.incrementTransactionsSell();

            double newPrice = calculateNewPrice(shopItem);
            shopItem.setCurrentPrice(newPrice);

            plugin.getDatabaseManager().updateShopItem(shopItem);
            plugin.getDatabaseManager().logTransaction(
                    player.getUniqueId().toString(),
                    dropMaterial,
                    "SELL",
                    amount,
                    sellPrice / amount,
                    netEarnings
            );

            plugin.getItemWorthManager().clearCache();

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                block.setType(Material.AIR);

                Material seedType = cropToSeed.get(cropType);
                if (seedType != null) {
                    Block soilBlock = block.getRelative(0, -1, 0);
                    if (soilBlock.getType() == Material.FARMLAND || soilBlock.getType() == Material.SOUL_SAND) {
                        block.setType(cropType);
                        if (cropType != Material.NETHER_WART) {
                            BlockData data = block.getBlockData();
                            if (data instanceof Ageable) {
                                ((Ageable) data).setAge(0);
                                block.setBlockData(data);
                            }
                        }
                    }
                }

                player.playSound(block.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);

                player.spawnParticle(Particle.HAPPY_VILLAGER,
                        block.getLocation().add(0.5, 0.5, 0.5),
                        10, 0.3, 0.3, 0.3, 0);

                String successMessage = plugin.getShopConfig().getMessage("auto-harvester.harvest-success",
                        "%amount%", String.valueOf(amount),
                        "%item%", formatMaterialName(dropMaterial),
                        "%earnings%", String.format("%.2f", netEarnings));
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(successMessage));
            });
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
}