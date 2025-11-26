package com.Lino.dynamicShopGUI.config;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.managers.MessageManager;
import com.Lino.dynamicShopGUI.utils.GradientColor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class ShopConfig {

    private final DynamicShopGUI plugin;
    private final CategoryConfigLoader categoryLoader;
    private final MessageManager messageManager;

    public ShopConfig(DynamicShopGUI plugin) {
        this.plugin = plugin;
        this.messageManager = new MessageManager(plugin);
        this.categoryLoader = new CategoryConfigLoader(plugin);
    }

    public void reload() {
        plugin.reloadConfig();
        messageManager.reload();
        categoryLoader.reload();
        plugin.getDatabaseManager().syncWithConfig();

        if (plugin.getItemWorthManager() != null) {
            plugin.getItemWorthManager().stop();
            plugin.getItemWorthManager().start();
            plugin.getItemWorthManager().clearCache();
        }
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

    public int getInitialStock(String category, Material material) {
        if (category == null) {
            return getMaxStock(category, material);
        }
        CategoryConfigLoader.CategoryConfig categoryConfig = categoryLoader.getCategory(category);
        if (categoryConfig != null) {
            CategoryConfigLoader.ItemConfig itemConfig = categoryConfig.getItemConfig(material);
            if (itemConfig != null) {
                return itemConfig.getInitialStock();
            }
        }
        return getMaxStock(category, material);
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
        return plugin.getConfig().getInt("restock.restock-percentage", 50);
    }

    public double getRestockTriggerThreshold() {
        return plugin.getConfig().getDouble("restock.trigger-threshold", 0.10);
    }

    public boolean isStockDecayEnabled() {
        return plugin.getConfig().getBoolean("stock-decay.enabled", true);
    }

    public double getStockDecayTriggerThreshold() {
        return plugin.getConfig().getDouble("stock-decay.trigger-threshold", 0.95);
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

    public List<CustomButtonConfig> getCustomButtons() {
        List<CustomButtonConfig> buttons = new ArrayList<>();

        if (plugin.getConfig().isConfigurationSection("custom-buttons")) {
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("custom-buttons");
            for (String key : section.getKeys(false)) {
                if (section.getBoolean(key + ".enabled")) {
                    buttons.add(new CustomButtonConfig(
                            section.getInt(key + ".slot"),
                            section.getString(key + ".material", "STONE"),
                            section.getString(key + ".command", ""),
                            section.getString(key + ".display-name", "Button"),
                            section.getStringList(key + ".lore")
                    ));
                }
            }
        }

        return buttons;
    }

    public static class CustomButtonConfig {
        private final int slot;
        private final Material material;
        private final String command;
        private final String displayName;
        private final List<String> lore;

        public CustomButtonConfig(int slot, String materialName, String command, String displayName, List<String> lore) {
            this.slot = slot;
            Material mat;
            try {
                mat = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                mat = Material.STONE;
            }
            this.material = mat;
            this.command = command;
            this.displayName = GradientColor.apply(displayName);

            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(GradientColor.apply(line));
            }
            this.lore = coloredLore;
        }

        public int getSlot() { return slot; }
        public Material getMaterial() { return material; }
        public String getCommand() { return command; }
        public String getDisplayName() { return displayName; }
        public List<String> getLore() { return lore; }
    }

    public String getMessage(String key) {
        return messageManager.getMessage(key);
    }

    public String getMessage(String key, Object... replacements) {
        return messageManager.getMessage(key, replacements);
    }

    public List<String> getMessageList(String key) {
        return messageManager.getMessageList(key);
    }

    public List<String> getMessageList(String key, Object... replacements) {
        return messageManager.getMessageList(key, replacements);
    }

    public String getPrefix() {
        return messageManager.getPrefix();
    }

    public MessageManager getMessageManager() {
        return messageManager;
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

    public boolean isOutOfStockAlertEnabled() {
        return plugin.getConfig().getBoolean("out-of-stock-alerts.enabled", true);
    }

    public String getOutOfStockSound() {
        return plugin.getConfig().getString("out-of-stock-alerts.sound", "BLOCK_ANVIL_LAND");
    }

    public float getOutOfStockSoundVolume() {
        return (float) plugin.getConfig().getDouble("out-of-stock-alerts.sound-volume", 0.8);
    }

    public float getOutOfStockSoundPitch() {
        return (float) plugin.getConfig().getDouble("out-of-stock-alerts.sound-pitch", 1.0);
    }

    public boolean showOutOfStockTitle() {
        return plugin.getConfig().getBoolean("out-of-stock-alerts.show-title", true);
    }

    public int getOutOfStockTitleDuration() {
        return plugin.getConfig().getInt("out-of-stock-alerts.title-duration", 60);
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