package com.Lino.dynamicShopGUI.config;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
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

    public void save() {
        try {
            shopConfig.save(shopFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}