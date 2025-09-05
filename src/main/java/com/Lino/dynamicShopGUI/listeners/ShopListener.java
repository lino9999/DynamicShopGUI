package com.Lino.dynamicShopGUI.listeners;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.handlers.MainMenuHandler;
import com.Lino.dynamicShopGUI.handlers.CategoryMenuHandler;
import com.Lino.dynamicShopGUI.handlers.TransactionMenuHandler;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

public class ShopListener implements Listener {

    private final DynamicShopGUI plugin;
    private final MainMenuHandler mainMenuHandler;
    private final CategoryMenuHandler categoryMenuHandler;
    private final TransactionMenuHandler transactionMenuHandler;

    public ShopListener(DynamicShopGUI plugin) {
        this.plugin = plugin;
        this.mainMenuHandler = new MainMenuHandler(plugin);
        this.categoryMenuHandler = new CategoryMenuHandler(plugin);
        this.transactionMenuHandler = new TransactionMenuHandler(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = ChatColor.stripColor(event.getView().getTitle());

        if (!title.contains("Dynamic Shop") && !title.contains("Shop -") &&
                !title.contains("Buy ") && !title.contains("Sell ")) {
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (plugin.getShopConfig().isSoundEnabled()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }

        if (title.equals("Dynamic Shop")) {
            mainMenuHandler.handleClick(player, clicked);
        } else if (title.startsWith("Shop -")) {
            categoryMenuHandler.handleClick(player, clicked, event.getClick(), title, event.getSlot());
        } else if (title.startsWith("Buy ") || title.startsWith("Sell ")) {
            transactionMenuHandler.handleClick(player, clicked, title.startsWith("Buy "));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            plugin.getGUIManager().clearPlayerData(player.getUniqueId());
        }
    }
}