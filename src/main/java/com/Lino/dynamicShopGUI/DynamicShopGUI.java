package com.Lino.dynamicShopGUI;

import com.Lino.dynamicShopGUI.listeners.AutoSellChestListener;
import com.Lino.dynamicShopGUI.managers.AutoSellChestManager;
import com.Lino.dynamicShopGUI.managers.ItemWorthManager;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import com.Lino.dynamicShopGUI.database.DatabaseManager;
import com.Lino.dynamicShopGUI.managers.ShopManager;
import com.Lino.dynamicShopGUI.managers.GUIManager;
import com.Lino.dynamicShopGUI.managers.RestockManager;
import com.Lino.dynamicShopGUI.commands.ShopCommand;
import com.Lino.dynamicShopGUI.listeners.ShopListener;
import com.Lino.dynamicShopGUI.listeners.ItemWorthListener;
import com.Lino.dynamicShopGUI.listeners.ItemStackFixListener;
import com.Lino.dynamicShopGUI.config.ShopConfig;

public class DynamicShopGUI extends JavaPlugin {

    private static DynamicShopGUI instance;
    private Economy economy;
    private DatabaseManager databaseManager;
    private ShopManager shopManager;
    private GUIManager guiManager;
    private ShopConfig shopConfig;
    private RestockManager restockManager;
    private ItemWorthManager itemWorthManager;
    private ItemStackFixListener itemStackFixListener;
    private AutoSellChestManager autoSellChestManager;

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
        itemWorthManager = new ItemWorthManager(this);
        itemStackFixListener = new ItemStackFixListener(this);
        autoSellChestManager = new AutoSellChestManager(this);


        itemWorthManager.start();

        registerCommands();
        registerListeners();

        getLogger().info("DynamicShopGUI has been enabled!");
    }

    @Override
    public void onDisable() {
        if (itemWorthManager != null) {
            itemWorthManager.stop();
        }

        if (autoSellChestManager != null) {
            autoSellChestManager.shutdown();
        }

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
        getServer().getPluginManager().registerEvents(new ItemWorthListener(this), this);
        getServer().getPluginManager().registerEvents(itemStackFixListener, this);
        getServer().getPluginManager().registerEvents(new AutoSellChestListener(this), this);
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

    public ItemWorthManager getItemWorthManager() {
        return itemWorthManager;
    }

    public ItemStackFixListener getItemStackFixListener() {
        return itemStackFixListener;
    }

    public AutoSellChestManager getAutoSellChestManager() {
        return autoSellChestManager;
    }
}
