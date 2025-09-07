package com.Lino.dynamicShopGUI.listeners;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.handlers.MainMenuHandler;
import com.Lino.dynamicShopGUI.handlers.CategoryMenuHandler;
import com.Lino.dynamicShopGUI.handlers.TransactionMenuHandler;
import com.Lino.dynamicShopGUI.managers.GUIManager;
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
        GUIManager.GUIType guiType = plugin.getGUIManager().getPlayerGUIType(player.getUniqueId());

        if (guiType == null) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (plugin.getShopConfig().isSoundEnabled()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }

        String title = ChatColor.stripColor(event.getView().getTitle());

        switch (guiType) {
            case MAIN_MENU:
                mainMenuHandler.handleClick(player, clicked, event.getSlot());
                break;
            case CATEGORY_MENU:
                categoryMenuHandler.handleClick(player, clicked, event.getClick(), title, event.getSlot());
                break;
            case TRANSACTION_BUY:
                transactionMenuHandler.handleClick(player, clicked, true);
                break;
            case TRANSACTION_SELL:
                transactionMenuHandler.handleClick(player, clicked, false);
                break;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();

        // Eseguiamo la logica solo se il giocatore era effettivamente in una delle GUI dello shop. [cite: 397]
        if (plugin.getGUIManager().getPlayerGUIType(player.getUniqueId()) != null) {

            // Scheduliamo un controllo per il prossimo tick del server.
            // Questo ritardo è FONDAMENTALE per dare al server il tempo di aprire una nuova GUI
            // nel caso in cui il giocatore stia navigando tra i menu.
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    // Controlliamo che il giocatore sia ancora online.
                    if (!player.isOnline()) {
                        return;
                    }

                    // Verifichiamo il tipo di inventario superiore attualmente aperto dal giocatore.
                    // Se è di tipo CRAFTING, significa che è tornato alla visuale standard
                    // del suo inventario (con la griglia di crafting 2x2 in alto).
                    if (player.getOpenInventory().getTopInventory().getType() == org.bukkit.event.inventory.InventoryType.CRAFTING) {

                        // Dato che è uscito completamente dal sistema dello shop,
                        // puliamo i suoi dati. Questo risolve il bug dell'inventario bloccato
                        // e assicura che i click futuri non vengano più annullati.
                        plugin.getGUIManager().clearPlayerData(player.getUniqueId());
                    }
                }
            }.runTaskLater(plugin, 1L); // Il ritardo di 1 tick è essenziale.
        }
    }
}