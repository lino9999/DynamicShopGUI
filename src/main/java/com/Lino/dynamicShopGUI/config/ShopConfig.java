package com.Lino.dynamicShopGUI.config;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ShopConfig {

    private final DynamicShopGUI plugin;
    private File shopFile;
    private FileConfiguration shopConfig;

    public ShopConfig(DynamicShopGUI plugin) {
        this.plugin = plugin;
        createDefaultShopConfig();
        loadShopConfig();
    }

    private void createDefaultShopConfig() {
        shopFile = new File(plugin.getDataFolder(), "shop.yml");
        if (!shopFile.exists()) {
            shopFile.getParentFile().mkdirs();
            plugin.saveResource("shop.yml", false);
        }
    }

    private void loadShopConfig() {
        shopConfig = YamlConfiguration.loadConfiguration(shopFile);
    }

    public void reload() {
        plugin.reloadConfig();
        loadShopConfig();
    }

    public Map<String, Map<String, Double>> getShopItems() {
        Map<String, Map<String, Double>> items = new HashMap<>();

        ConfigurationSection categoriesSection = shopConfig.getConfigurationSection("categories");
        if (categoriesSection == null) return items;

        for (String category : categoriesSection.getKeys(false)) {
            Map<String, Double> categoryItems = new HashMap<>();
            ConfigurationSection itemsSection = categoriesSection.getConfigurationSection(category + ".items");

            if (itemsSection != null) {
                for (String item : itemsSection.getKeys(false)) {
                    double price = itemsSection.getDouble(item);
                    categoryItems.put(item, price);
                }
            }

            items.put(category.toUpperCase(), categoryItems);
        }

        return items;
    }

    public String getCategoryDisplayName(String category) {
        return shopConfig.getString("categories." + category.toLowerCase() + ".display-name", category);
    }

    // Stock related methods
    public int getInitialStock() {
        return plugin.getConfig().getInt("stock.initial-stock", 100);
    }

    public int getMaxStock(String category, Material material) {
        // Check for special item override first
        String materialName = material.name();
        if (plugin.getConfig().contains("stock.special-items." + materialName)) {
            return plugin.getConfig().getInt("stock.special-items." + materialName);
        }

        // Then check category max stock
        String categoryKey = "stock.max-stock." + category.toLowerCase();
        if (plugin.getConfig().contains(categoryKey)) {
            return plugin.getConfig().getInt(categoryKey);
        }

        // Default fallback
        return 1000;
    }

    // Restock related methods
    public boolean isRestockEnabled() {
        return plugin.getConfig().getBoolean("restock.enabled", true);
    }

    public int getRestockTime() {
        return plugin.getConfig().getInt("restock.restock-time", 60);
    }

    public int getRestockPercentage() {
        return plugin.getConfig().getInt("restock.restock-percentage", 100);
    }

    // GUI related methods
    public boolean isSoundEnabled() {
        return plugin.getConfig().getBoolean("gui.sounds-enabled", true);
    }

    public int getItemsPerPage() {
        return plugin.getConfig().getInt("gui.items-per-page", 45);
    }

    public boolean showStock() {
        return plugin.getConfig().getBoolean("gui.show-stock", true);
    }

    public boolean showPriceTrends() {
        return plugin.getConfig().getBoolean("gui.show-price-trends", true);
    }

    // Message methods
    public String getMessage(String key) {
        String message = plugin.getConfig().getString("messages." + key, key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getPrefix() {
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&aDynamicShop&8] &7");
        return ChatColor.translateAlternateColorCodes('&', prefix);
    }

    // Price factor methods
    public double getPriceIncreaseFactor() {
        return plugin.getConfig().getDouble("price-factors.increase", 0.05);
    }

    public double getPriceDecreaseFactor() {
        return plugin.getConfig().getDouble("price-factors.decrease", 0.03);
    }

    public double getMinPriceMultiplier() {
        return plugin.getConfig().getDouble("price-limits.min-multiplier", 0.1);
    }

    public double getMaxPriceMultiplier() {
        return plugin.getConfig().getDouble("price-limits.max-multiplier", 10.0);
    }

    public void save() {
        try {
            shopConfig.save(shopFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}