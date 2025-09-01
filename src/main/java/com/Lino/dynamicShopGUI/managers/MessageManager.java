package com.Lino.dynamicShopGUI.managers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.utils.GradientColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MessageManager {

    private final DynamicShopGUI plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public MessageManager(DynamicShopGUI plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        InputStream defConfigStream = plugin.getResource("messages.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            messagesConfig.setDefaults(defConfig);
        }
    }

    public void reload() {
        loadMessages();
    }

    public String getMessage(String path) {
        String message = messagesConfig.getString(path, path);
        return GradientColor.apply(message);
    }

    public String getMessage(String path, Object... replacements) {
        String message = messagesConfig.getString(path, path);
        return GradientColor.applyWithVariables(message, replacements);
    }

    public String getPrefix() {
        return getMessage("prefix");
    }

    public void saveMessages() {
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save messages.yml!");
            e.printStackTrace();
        }
    }
}