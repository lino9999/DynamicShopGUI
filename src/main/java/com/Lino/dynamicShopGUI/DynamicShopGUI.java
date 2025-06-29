package com.Lino.dynamicShopGUI;

import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import com.Lino.dynamicShopGUI.database.DatabaseManager;
import com.Lino.dynamicShopGUI.managers.ShopManager;
import com.Lino.dynamicShopGUI.managers.GUIManager;
import com.Lino.dynamicShopGUI.managers.RestockManager;
import com.Lino.dynamicShopGUI.commands.ShopCommand;
import com.Lino.dynamicShopGUI.listeners.ShopListener;
import com.Lino.dynamicShopGUI.config.ShopConfig;

public class DynamicShopGUI extends JavaPlugin {

    private static DynamicShopGUI instance;
    private Economy economy;
    private DatabaseManager databaseManager;
    private ShopManager shopManager;
    private GUIManager guiManager;
    private ShopConfig shopConfig;
    private RestockManager restockManager;

    @Override
    public void onEnable() {
        instance = this;

        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        shopConfig = new ShopConfig(this);

        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Failed to initialize database! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        restockManager = new RestockManager(this);
        shopManager = new ShopManager(this);
        guiManager = new GUIManager(this);

        registerCommands();
        registerListeners();

        getLogger().info("DynamicShopGUI has been enabled!");
    }

    @Override
    public void onDisable() {
        if (restockManager != null) {
            restockManager.shutdown();
        }

        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("DynamicShopGUI has been disabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void registerCommands() {
        getCommand("shop").setExecutor(new ShopCommand(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);
    }

    public static DynamicShopGUI getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }

    public ShopConfig getShopConfig() {
        return shopConfig;
    }

    public RestockManager getRestockManager() {
        return restockManager;
    }
}