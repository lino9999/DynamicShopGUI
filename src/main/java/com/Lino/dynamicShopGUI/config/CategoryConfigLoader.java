package com.Lino.dynamicShopGUI.config;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class CategoryConfigLoader {
    private final DynamicShopGUI plugin;
    private final Map<String, CategoryConfig> categories = new HashMap<>();

    public CategoryConfigLoader(DynamicShopGUI plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        loadCategories();
    }

    public void reload() {
        categories.clear();
        loadCategories();
    }

    private void loadCategories() {
        File shopFile = new File(plugin.getDataFolder(), "shop.yml");

        if (!shopFile.exists()) {
            try {
                plugin.saveResource("shop.yml", false);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save default shop.yml: " + e.getMessage());
                return;
            }
        }

        FileConfiguration shopConfig = YamlConfiguration.loadConfiguration(shopFile);

        InputStream defConfigStream = plugin.getResource("shop.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            shopConfig.setDefaults(defConfig);
        }

        ConfigurationSection categoriesSection = shopConfig.getConfigurationSection("categories");
        if (categoriesSection == null) {
            plugin.getLogger().severe("No categories section found in shop.yml!");
            return;
        }

        for (String categoryKey : categoriesSection.getKeys(false)) {
            ConfigurationSection categorySection = categoriesSection.getConfigurationSection(categoryKey);
            if (categorySection == null) continue;

            String displayName = categorySection.getString("display-name", categoryKey);
            Material icon = Material.CHEST;

            switch (categoryKey.toLowerCase()) {
                case "building":
                    icon = Material.BRICKS;
                    break;
                case "ores":
                    icon = Material.DIAMOND;
                    break;
                case "food":
                    icon = Material.APPLE;
                    break;
                case "tools":
                    icon = Material.IRON_PICKAXE;
                    break;
                case "armor":
                    icon = Material.IRON_CHESTPLATE;
                    break;
                case "redstone":
                    icon = Material.REDSTONE;
                    break;
                case "farming":
                    icon = Material.WHEAT;
                    break;
                case "misc":
                    icon = Material.ENDER_PEARL;
                    break;
            }

            CategoryConfig category = new CategoryConfig(displayName, icon);

            ConfigurationSection itemsSection = categorySection.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String itemKey : itemsSection.getKeys(false)) {
                    try {
                        Material material = Material.valueOf(itemKey.toUpperCase());
                        double price = itemsSection.getDouble(itemKey);

                        int maxStock = plugin.getConfig().getInt("stock.max-stock." + categoryKey.toLowerCase(), 1000);

                        if (plugin.getConfig().contains("stock.special-items." + itemKey)) {
                            maxStock = plugin.getConfig().getInt("stock.special-items." + itemKey);
                        }

                        int initialStock = maxStock;

                        ItemConfig itemConfig = new ItemConfig(price, initialStock, maxStock);
                        category.addItem(material, itemConfig);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid material: " + itemKey);
                    }
                }
            }

            double taxRate = plugin.getConfig().getDouble("tax.category-rates." + categoryKey.toLowerCase(),
                    plugin.getConfig().getDouble("tax.rate", 15.0));
            category.setTaxRate(taxRate);

            categories.put(categoryKey, category);
        }

        plugin.getLogger().info("Loaded " + categories.size() + " shop categories");
    }

    public CategoryConfig getCategory(String name) {
        if (name == null) {
            return null;
        }
        return categories.get(name.toLowerCase());
    }

    public Map<String, CategoryConfig> getAllCategories() {
        return new HashMap<>(categories);
    }

    public static class CategoryConfig {
        private final String displayName;
        private final Material icon;
        private final Map<Material, ItemConfig> items = new HashMap<>();
        private double taxRate = 15.0;

        public CategoryConfig(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }

        public void addItem(Material material, ItemConfig config) {
            items.put(material, config);
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getIcon() {
            return icon;
        }

        public Map<Material, ItemConfig> getItems() {
            return items;
        }

        public ItemConfig getItemConfig(Material material) {
            return items.get(material);
        }

        public double getTaxRate() {
            return taxRate;
        }

        public void setTaxRate(double taxRate) {
            this.taxRate = taxRate;
        }
    }

    public static class ItemConfig {
        private final double price;
        private final int initialStock;
        private final int maxStock;

        public ItemConfig(double price, int initialStock, int maxStock) {
            this.price = price;
            this.initialStock = initialStock;
            this.maxStock = maxStock;
        }

        public double getPrice() {
            return price;
        }

        public int getInitialStock() {
            return initialStock;
        }

        public int getMaxStock() {
            return maxStock;
        }
    }
}