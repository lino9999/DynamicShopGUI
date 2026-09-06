package com.Lino.dynamicShopGUI.config;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MenuConfigManager {
    private final DynamicShopGUI plugin;
    private File menuFile;
    private FileConfiguration menuConfig;

    public MenuConfigManager(DynamicShopGUI plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        menuFile = new File(plugin.getDataFolder(), "menus" + File.separator + "config.yml");
        if (!menuFile.exists()) {
            plugin.saveResource("menus/config.yml", false);
        }
        menuConfig = YamlConfiguration.loadConfiguration(menuFile);
    }

    public boolean getBoolean(String path, boolean fallback) {
        return menuConfig.contains(path) ? menuConfig.getBoolean(path) : plugin.getConfig().getBoolean(path, fallback);
    }

    public int getInt(String path, int fallback) {
        return menuConfig.contains(path) ? menuConfig.getInt(path) : plugin.getConfig().getInt(path, fallback);
    }

    public boolean isConfigurationSection(String path) {
        return menuConfig.isConfigurationSection(path) || plugin.getConfig().isConfigurationSection(path);
    }

    public ConfigurationSection getSection(String path) {
        ConfigurationSection section = menuConfig.getConfigurationSection(path);
        return section != null ? section : plugin.getConfig().getConfigurationSection(path);
    }

    public FileConfiguration getConfig() {
        return menuConfig;
    }

    public void save() {
        try {
            menuConfig.save(menuFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save menus/config.yml!");
            plugin.getLogger().severe(exception.getMessage());
        }
    }
}
