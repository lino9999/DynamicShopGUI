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
import java.util.List;
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

    // Tax related methods
    public boolean isTaxEnabled() {
        return plugin.getConfig().getBoolean("tax.enabled", true);
    }

    public double getTaxRate() {
        return plugin.getConfig().getDouble("tax.rate", 15.0) / 100.0; // Convert percentage to decimal
    }

    public double getTaxRate(String category) {
        // Check for category-specific tax rate
        String categoryKey = "tax.category-rates." + category.toLowerCase();
        if (plugin.getConfig().contains(categoryKey)) {
            return plugin.getConfig().getDouble(categoryKey) / 100.0;
        }
        // Fall back to global tax rate
        return getTaxRate();
    }

    public double getMinimumTax() {
        return plugin.getConfig().getDouble("tax.minimum", 0.01);
    }

    public boolean isTaxExempt(Material material) {
        List<String> exemptItems = plugin.getConfig().getStringList("tax.exempt-items");
        return exemptItems.contains(material.name());
    }

    public double calculateTax(Material material, String category, double amount) {
        if (!isTaxEnabled() || isTaxExempt(material)) {
            return 0.0;
        }

        double taxRate = getTaxRate(category);
        double tax = amount * taxRate;

        // Apply minimum tax if configured
        return Math.max(tax, getMinimumTax());
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

    public boolean showTaxInfo() {
        return plugin.getConfig().getBoolean("gui.show-tax-info", true);
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

    // Price alert methods
    public boolean isPriceAlertsEnabled() {
        return plugin.getConfig().getBoolean("price-alerts.enabled", true);
    }

    public double getPriceIncreaseThreshold() {
        return plugin.getConfig().getDouble("price-alerts.increase-threshold", 70.0);
    }

    public double getPriceDecreaseThreshold() {
        return plugin.getConfig().getDouble("price-alerts.decrease-threshold", -70.0);
    }

    public String getPriceIncreaseSound() {
        return plugin.getConfig().getString("price-alerts.increase-sound", "ENTITY_ENDER_DRAGON_GROWL");
    }

    public String getPriceDecreaseSound() {
        return plugin.getConfig().getString("price-alerts.decrease-sound", "ENTITY_PLAYER_LEVELUP");
    }

    public float getSoundVolume() {
        return (float) plugin.getConfig().getDouble("price-alerts.sound-volume", 0.5);
    }

    public float getSoundPitch() {
        return (float) plugin.getConfig().getDouble("price-alerts.sound-pitch", 1.0);
    }

    public boolean showTitle() {
        return plugin.getConfig().getBoolean("price-alerts.show-title", true);
    }

    public int getTitleDuration() {
        return plugin.getConfig().getInt("price-alerts.title-duration", 60);
    }

    public void save() {
        try {
            shopConfig.save(shopFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}