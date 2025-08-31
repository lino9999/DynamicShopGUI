package com.Lino.dynamicShopGUI.config;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopConfig {

    private final DynamicShopGUI plugin;
    private final CategoryConfigLoader categoryLoader;

    public ShopConfig(DynamicShopGUI plugin) {
        this.plugin = plugin;
        this.categoryLoader = new CategoryConfigLoader(plugin);
    }

    public void reload() {
        plugin.reloadConfig();
        categoryLoader.reload();
    }

    public Map<String, Map<String, Double>> getShopItems() {
        Map<String, Map<String, Double>> items = new HashMap<>();

        for (Map.Entry<String, CategoryConfigLoader.CategoryConfig> entry : categoryLoader.getAllCategories().entrySet()) {
            String categoryName = entry.getKey();
            CategoryConfigLoader.CategoryConfig category = entry.getValue();

            Map<String, Double> categoryItems = new HashMap<>();
            for (Map.Entry<Material, CategoryConfigLoader.ItemConfig> itemEntry : category.getItems().entrySet()) {
                categoryItems.put(itemEntry.getKey().name(), itemEntry.getValue().getPrice());
            }

            items.put(categoryName, categoryItems);
        }

        return items;
    }

    public String getCategoryDisplayName(String category) {
        if (category == null) {
            return "Unknown";
        }
        CategoryConfigLoader.CategoryConfig categoryConfig = categoryLoader.getCategory(category);
        return categoryConfig != null ? categoryConfig.getDisplayName() : category;
    }

    public Material getCategoryIcon(String category) {
        if (category == null) {
            return Material.CHEST;
        }
        CategoryConfigLoader.CategoryConfig categoryConfig = categoryLoader.getCategory(category);
        return categoryConfig != null ? categoryConfig.getIcon() : Material.CHEST;
    }

    public int getInitialStock() {
        return plugin.getConfig().getInt("stock.initial-stock", 100);
    }

    public int getInitialStock(String category, Material material) {
        if (category == null) {
            return getInitialStock();
        }
        CategoryConfigLoader.CategoryConfig categoryConfig = categoryLoader.getCategory(category);
        if (categoryConfig != null) {
            CategoryConfigLoader.ItemConfig itemConfig = categoryConfig.getItemConfig(material);
            if (itemConfig != null) {
                return itemConfig.getInitialStock();
            }
        }
        return getInitialStock();
    }

    public int getMaxStock(String category, Material material) {
        if (category == null) {
            return 1000;
        }
        CategoryConfigLoader.CategoryConfig categoryConfig = categoryLoader.getCategory(category);
        if (categoryConfig != null) {
            CategoryConfigLoader.ItemConfig itemConfig = categoryConfig.getItemConfig(material);
            if (itemConfig != null) {
                return itemConfig.getMaxStock();
            }
        }

        String categoryKey = "stock.max-stock." + category.toLowerCase();
        if (plugin.getConfig().contains(categoryKey)) {
            return plugin.getConfig().getInt(categoryKey);
        }

        return 1000;
    }

    public boolean isTaxEnabled() {
        return plugin.getConfig().getBoolean("tax.enabled", true);
    }

    public double getTaxRate() {
        return plugin.getConfig().getDouble("tax.rate", 15.0) / 100.0;
    }

    public double getTaxRate(String category) {
        if (category == null) {
            return getTaxRate();
        }
        CategoryConfigLoader.CategoryConfig categoryConfig = categoryLoader.getCategory(category);
        if (categoryConfig != null) {
            return categoryConfig.getTaxRate() / 100.0;
        }

        String categoryKey = "tax.category-rates." + category.toLowerCase();
        if (plugin.getConfig().contains(categoryKey)) {
            return plugin.getConfig().getDouble(categoryKey) / 100.0;
        }

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

        return Math.max(tax, getMinimumTax());
    }

    public boolean isRestockEnabled() {
        return plugin.getConfig().getBoolean("restock.enabled", true);
    }

    public int getRestockTime() {
        return plugin.getConfig().getInt("restock.restock-time", 60);
    }

    public int getRestockPercentage() {
        return plugin.getConfig().getInt("restock.restock-percentage", 100);
    }

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

    public String getMessage(String key) {
        String message = plugin.getConfig().getString("messages." + key, key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getPrefix() {
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&aDynamicShop&8] &7");
        return ChatColor.translateAlternateColorCodes('&', prefix);
    }

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

    public CategoryConfigLoader getCategoryLoader() {
        return categoryLoader;
    }

    public Map<String, CategoryConfigLoader.CategoryConfig> getAllCategories() {
        return categoryLoader.getAllCategories();
    }

    public void save() {
    }
}