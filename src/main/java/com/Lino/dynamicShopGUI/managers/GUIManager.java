package com.Lino.dynamicShopGUI.managers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.gui.MainMenuGUI;
import com.Lino.dynamicShopGUI.gui.CategoryMenuGUI;
import com.Lino.dynamicShopGUI.gui.TransactionMenuGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import java.util.*;

public class GUIManager {

    private final DynamicShopGUI plugin;
    private final Map<UUID, String> playerCategory = new HashMap<>();
    private final Map<UUID, Material> playerSelectedItem = new HashMap<>();
    private final Map<UUID, Integer> playerPage = new HashMap<>();

    private final MainMenuGUI mainMenuGUI;
    private final CategoryMenuGUI categoryMenuGUI;
    private final TransactionMenuGUI transactionMenuGUI;

    public GUIManager(DynamicShopGUI plugin) {
        this.plugin = plugin;
        this.mainMenuGUI = new MainMenuGUI(plugin);
        this.categoryMenuGUI = new CategoryMenuGUI(plugin, this);
        this.transactionMenuGUI = new TransactionMenuGUI(plugin, this);
    }

    public void openMainMenu(Player player) {
        mainMenuGUI.open(player);
    }

    public void openCategoryMenu(Player player, String category, int page) {
        playerCategory.put(player.getUniqueId(), category);
        playerPage.put(player.getUniqueId(), page);
        categoryMenuGUI.open(player, category, page);
    }

    public void openTransactionMenu(Player player, Material material, boolean isBuying) {
        playerSelectedItem.put(player.getUniqueId(), material);
        transactionMenuGUI.open(player, material, isBuying);
    }

    public String getPlayerCategory(UUID uuid) {
        return playerCategory.get(uuid);
    }

    public Material getPlayerSelectedItem(UUID uuid) {
        return playerSelectedItem.get(uuid);
    }

    public void setPlayerSelectedItem(UUID uuid, Material material) {
        playerSelectedItem.put(uuid, material);
    }

    public int getPlayerPage(UUID uuid) {
        return playerPage.getOrDefault(uuid, 0);
    }

    public void clearPlayerData(UUID uuid) {
        playerCategory.remove(uuid);
        playerSelectedItem.remove(uuid);
        playerPage.remove(uuid);
    }
}