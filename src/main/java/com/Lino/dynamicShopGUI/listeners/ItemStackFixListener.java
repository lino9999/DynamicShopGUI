
package com.Lino.dynamicShopGUI.listeners;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;

import java.util.*;

public class ItemStackFixListener implements Listener {

    private final DynamicShopGUI plugin;
    private final Set<UUID> processingInventory = new HashSet<>();

    public ItemStackFixListener(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("item-worth.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                removeWorthFromItem(item);
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.getGameMode() != GameMode.CREATIVE) {
                    for (int i = 0; i < player.getInventory().getSize(); i++) {
                        ItemStack item = player.getInventory().getItem(i);
                        if (item != null && item.getType() != Material.AIR) {
                            plugin.getItemWorthManager().updateSingleItem(item);
                        }
                    }
                    player.updateInventory();
                }
            }
        }.runTaskLater(plugin, 2L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!plugin.getConfig().getBoolean("item-worth.enabled", true)) {
            return;
        }

        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ItemStack pickedUp = event.getItem().getItemStack();
        Material pickedMaterial = pickedUp.getType();

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack invItem = player.getInventory().getItem(i);
            if (invItem != null && invItem.getType() == pickedMaterial) {
                removeWorthFromItem(invItem);
            }
        }

        removeWorthFromItem(pickedUp);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.getGameMode() != GameMode.CREATIVE) {
                    for (int i = 0; i < player.getInventory().getSize(); i++) {
                        ItemStack item = player.getInventory().getItem(i);
                        if (item != null && item.getType() == pickedMaterial) {
                            plugin.getItemWorthManager().updateSingleItem(item);
                        }
                    }
                    player.updateInventory();
                }
            }
        }.runTaskLater(plugin, 2L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.getConfig().getBoolean("item-worth.enabled", true)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        String title = event.getView().getTitle();

        // --- INIZIO MODIFICA PER LO SHOP ---
        if (title.contains("Shop") || title.contains("Buy") || title.contains("Sell") || title.contains("Dynamic")) {
            // Questa logica serve per permettere di impilare gli oggetti comprati (senza lore)
            // con quelli già presenti nell'inventario (con lore).

            boolean isShiftClick = event.isShiftClick();
            ItemStack cursorItem = event.getCursor();
            Inventory clickedInventory = event.getClickedInventory();

            // Caso 1: Shift-click per trasferire dallo shop all'inventario del giocatore
            if (isShiftClick && clickedInventory != null && !clickedInventory.equals(player.getInventory())) {
                // Dobbiamo pulire l'intero inventario per trovare uno stack compatibile.
                removeWorthFromInventorySlots(player);
                processingInventory.add(player.getUniqueId()); // Segna per la pulizia alla chiusura
            }
            // Caso 2: Click normale per posare un oggetto dal cursore nell'inventario del giocatore
            else if (cursorItem != null && cursorItem.getType() != Material.AIR && player.getInventory().equals(clickedInventory)) {
                // Puliamo solo lo slot di destinazione per essere meno invasivi.
                ItemStack currentItem = event.getCurrentItem();
                if (currentItem != null) {
                    removeWorthFromItem(currentItem);
                }
                processingInventory.add(player.getUniqueId()); // Segna per la pulizia alla chiusura
            }
            return; // Fondamentale: usciamo per non eseguire la logica di stacking generica sottostante.
        }
        // --- FINE MODIFICA PER LO SHOP ---


        if (event.getClick() == ClickType.NUMBER_KEY ||
                event.getClick() == ClickType.DROP ||
                event.getClick() == ClickType.CONTROL_DROP) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        boolean needsProcessing = false;

        if (clicked != null && cursor != null &&
                clicked.getType() != Material.AIR &&
                cursor.getType() != Material.AIR &&
                clicked.getType() == cursor.getType()) {
            needsProcessing = true;
        }

        if (event.isShiftClick() && clicked != null && clicked.getType() != Material.AIR) {
            needsProcessing = true;
        }

        if (needsProcessing) {
            processingInventory.add(player.getUniqueId());

            removeWorthFromInventorySlots(player);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && player.getGameMode() != GameMode.CREATIVE) {
                        processingInventory.remove(player.getUniqueId());
                        reapplyWorthToInventorySlots(player);
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        // --- INIZIO MODIFICA ROBUSTA ---
        String title = event.getView().getTitle();
        boolean wasInShop = title.contains("Shop") || title.contains("Buy") || title.contains("Sell") || title.contains("Dynamic");

        // Se il giocatore stava usando lo shop O il suo inventario è stato marcato per elaborazione,
        // eseguiamo una risincronizzazione completa per evitare bug.
        if (wasInShop || processingInventory.contains(player.getUniqueId())) {

            if (processingInventory.contains(player.getUniqueId())) {
                processingInventory.remove(player.getUniqueId());
            }

            // Puliamo anche l'oggetto sul cursore prima di chiudere.
            ItemStack cursor = player.getItemOnCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                removeWorthFromItem(cursor);
            }

            // Scheduliamo la riapplicazione della lore. Aumentiamo il ritardo a 2 tick (0.1s)
            // per garantire che il server abbia completato tutte le altre operazioni di inventario,
            // prevenendo problemi di desincronizzazione.
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && player.getGameMode() != GameMode.CREATIVE) {
                        reapplyWorthToInventorySlots(player);
                    }
                }
            }.runTaskLater(plugin, 2L);
        }
        // --- FINE MODIFICA ROBUSTA ---
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!plugin.getConfig().getBoolean("item-worth.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ItemStack item = event.getItemDrop().getItemStack();
        removeWorthFromItem(item);
    }

    private void removeWorthFromInventorySlots(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                removeWorthFromItem(item);
            }
        }
    }

    private void reapplyWorthToInventorySlots(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                plugin.getItemWorthManager().updateSingleItem(item);
            }
        }
        player.updateInventory();
    }

    private void removeWorthFromItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return;
        }

        List<String> lore = new ArrayList<>(meta.getLore());
        Iterator<String> iterator = lore.iterator();

        while (iterator.hasNext()) {
            String line = ChatColor.stripColor(iterator.next());
            if (line.startsWith("Worth:") ||
                    line.contains("Not Sellable") ||
                    line.contains("$")) {
                iterator.remove();
            }
        }

        if (lore.isEmpty()) {
            meta.setLore(null);
        } else {
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
    }

    public boolean isProcessingInventory(UUID uuid) {
        return processingInventory.contains(uuid);
    }
}

