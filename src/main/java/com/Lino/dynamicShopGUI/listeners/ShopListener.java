package com.Lino.dynamicShopGUI.listeners;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.managers.ShopManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class ShopListener implements Listener {

    private final DynamicShopGUI plugin;

    public ShopListener(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Debug print per capire quale GUI stiamo gestendo
        plugin.getLogger().info("GUI Title: " + title);

        if (!title.contains("Dynamic Shop") && !title.contains("Shop -") &&
                !title.contains("Buy ") && !title.contains("Sell ")) {
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Play click sound if enabled
        if (plugin.getShopConfig().isSoundEnabled()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }

        // Debug print per capire cosa è stato cliccato
        String itemName = clicked.getItemMeta() != null && clicked.getItemMeta().getDisplayName() != null ?
                clicked.getItemMeta().getDisplayName() : clicked.getType().name();
        plugin.getLogger().info("Clicked item: " + itemName + " of type: " + clicked.getType());

        if (title.equals(ChatColor.DARK_GREEN + "Dynamic Shop")) {
            handleMainMenuClick(player, clicked);
        } else if (title.startsWith(ChatColor.DARK_GREEN + "Shop -")) {
            handleCategoryMenuClick(player, clicked, event.getClick(), title);
        } else if (title.startsWith(ChatColor.DARK_GREEN + "Buy ") ||
                title.startsWith(ChatColor.DARK_RED + "Sell ")) {
            handleTransactionMenuClick(player, clicked, title.startsWith(ChatColor.DARK_GREEN + "Buy "));
        }
    }

    private void handleMainMenuClick(Player player, ItemStack clicked) {
        Material type = clicked.getType();
        String category = null;

        switch (type) {
            case STONE:
                category = "BUILDING";
                break;
            case DIAMOND:
                category = "ORES";
                break;
            case BREAD:
                category = "FOOD";
                break;
            case DIAMOND_PICKAXE:
                category = "TOOLS";
                break;
            case DIAMOND_CHESTPLATE:
                category = "ARMOR";
                break;
            case REDSTONE:
                category = "REDSTONE";
                break;
            case WHEAT:
                category = "FARMING";
                break;
            case ENDER_PEARL:
                category = "MISC";
                break;
        }

        if (category != null) {
            plugin.getLogger().info("Opening category: " + category);
            plugin.getGUIManager().openCategoryMenu(player, category, 0);
        }
    }

    private void handleCategoryMenuClick(Player player, ItemStack clicked, ClickType clickType, String title) {
        if (clicked.getType() == Material.ARROW) {
            String displayName = clicked.getItemMeta().getDisplayName();

            if (displayName.contains("Back to Categories")) {
                plugin.getGUIManager().openMainMenu(player);
            } else if (displayName.contains("Previous Page")) {
                String category = plugin.getGUIManager().getPlayerCategory(player.getUniqueId());
                int currentPage = plugin.getGUIManager().getPlayerPage(player.getUniqueId());
                plugin.getGUIManager().openCategoryMenu(player, category, currentPage - 1);
            } else if (displayName.contains("Next Page")) {
                String category = plugin.getGUIManager().getPlayerCategory(player.getUniqueId());
                int currentPage = plugin.getGUIManager().getPlayerPage(player.getUniqueId());
                plugin.getGUIManager().openCategoryMenu(player, category, currentPage + 1);
            }
            return;
        }

        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE || clicked.getType() == Material.PAPER) {
            return;
        }

        Material material = clicked.getType();
        boolean isBuying = clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT;

        // Check if item is out of stock ONLY for buying, not selling
        if (isBuying && clicked.getItemMeta() != null && clicked.getItemMeta().getLore() != null) {
            for (String loreLine : clicked.getItemMeta().getLore()) {
                if (loreLine.contains("Out of Stock")) {
                    player.sendMessage(ChatColor.RED + "This item is out of stock for purchase!");
                    if (plugin.getShopConfig().isSoundEnabled()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                    }
                    return;
                }
            }
        }

        plugin.getLogger().info("Opening transaction menu for: " + material + " isBuying: " + isBuying);

        // Assicurati che l'item sia impostato nel GUIManager prima di aprire il menu
        plugin.getGUIManager().setPlayerSelectedItem(player.getUniqueId(), material);

        plugin.getGUIManager().openTransactionMenu(player, material, isBuying);
    }

    private void handleTransactionMenuClick(Player player, ItemStack clicked, boolean currentlyBuying) {
        Material type = clicked.getType();

        plugin.getLogger().info("Transaction menu click - Type: " + type + " Currently buying: " + currentlyBuying);

        // Prima di tutto, recupera sempre il material selezionato
        Material selectedItem = plugin.getGUIManager().getPlayerSelectedItem(player.getUniqueId());

        // Se selectedItem è null, prova a estrarlo dal titolo o dal display item
        if (selectedItem == null) {
            selectedItem = extractMaterialFromGUI(player);
            if (selectedItem != null) {
                plugin.getGUIManager().setPlayerSelectedItem(player.getUniqueId(), selectedItem);
            }
        }

        plugin.getLogger().info("Current selected item for " + player.getName() + ": " + selectedItem);

        if (type == Material.ARROW) {
            String category = plugin.getGUIManager().getPlayerCategory(player.getUniqueId());
            if (category != null) {
                int page = plugin.getGUIManager().getPlayerPage(player.getUniqueId());
                plugin.getGUIManager().openCategoryMenu(player, category, page);
            } else {
                plugin.getGUIManager().openMainMenu(player);
            }
            return;
        }

        if (type == Material.HOPPER) {
            if (selectedItem != null) {
                plugin.getLogger().info("Switching transaction mode for: " + selectedItem);
                plugin.getGUIManager().openTransactionMenu(player, selectedItem, !currentlyBuying);
            } else {
                plugin.getLogger().warning("Cannot switch transaction mode - no selected item");
                player.sendMessage(ChatColor.RED + "Error: No item selected. Please close and reopen the shop.");
            }
            return;
        }

        // Gestione dei pulsanti per comprare/vendere quantità specifiche
        if (type == Material.LIME_STAINED_GLASS_PANE || type == Material.RED_STAINED_GLASS_PANE) {
            if (clicked.getItemMeta() == null || clicked.getItemMeta().getDisplayName() == null) {
                plugin.getLogger().warning("Button clicked but no display name found");
                return;
            }

            String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

            plugin.getLogger().info("Button clicked: " + name + " for item: " + selectedItem);

            if (selectedItem == null) {
                player.sendMessage(ChatColor.RED + "Error: Could not determine the item. Please try reopening the shop.");
                plugin.getLogger().severe("Could not determine selected item for player " + player.getName());
                return;
            }

            int amount = 0;

            // Extract amount from the display name
            if (name.toLowerCase().contains("all")) {
                if (!currentlyBuying) {
                    amount = countItemsInInventory(player, selectedItem);
                    plugin.getLogger().info("Selling all items: " + amount);
                } else {
                    plugin.getLogger().warning("'All' option should not be available for buying");
                    return;
                }
            } else {
                // Extract number from the string - improved parsing
                String[] parts = name.split(" ");
                for (String part : parts) {
                    try {
                        amount = Integer.parseInt(part);
                        break;
                    } catch (NumberFormatException ignored) {}
                }

                // Se non troviamo un numero nei parts, proviamo a cercare numeri nella stringa
                if (amount == 0) {
                    String numberOnly = name.replaceAll("[^0-9]", "");
                    if (!numberOnly.isEmpty()) {
                        try {
                            amount = Integer.parseInt(numberOnly);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            plugin.getLogger().info("Parsed amount: " + amount);

            if (amount > 0) {
                if (currentlyBuying) {
                    plugin.getLogger().info("Processing buy transaction: " + amount + "x " + selectedItem);
                    processBuyTransaction(player, selectedItem, amount);
                } else {
                    plugin.getLogger().info("Processing sell transaction: " + amount + "x " + selectedItem);
                    processSellTransaction(player, selectedItem, amount);
                }
            } else {
                plugin.getLogger().warning("Could not parse amount from button: " + name);
                player.sendMessage(ChatColor.RED + "Error: Could not determine amount to trade");
            }
        }
    }

    // Metodo helper per estrarre il material dalla GUI
    private Material extractMaterialFromGUI(Player player) {
        String title = player.getOpenInventory().getTitle();
        String cleanTitle = ChatColor.stripColor(title);
        plugin.getLogger().info("Trying to extract item from title: '" + cleanTitle + "'");

        Material material = null;

        // Prima prova a estrarre dal titolo
        if (cleanTitle.startsWith("Buy ") || cleanTitle.startsWith("Sell ")) {
            String itemName = cleanTitle.substring(4).trim(); // Rimuovi "Buy " o "Sell "
            plugin.getLogger().info("Extracted item name: '" + itemName + "'");

            // Prova diversi formati del nome
            String[] possibleNames = {
                    itemName.toUpperCase().replace(" ", "_"),
                    itemName.toUpperCase().replace(" ", ""),
                    itemName.replace(" ", "_").toUpperCase()
            };

            for (String possibleName : possibleNames) {
                try {
                    material = Material.valueOf(possibleName);
                    plugin.getLogger().info("Successfully extracted item from title: " + material);
                    break;
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().info("Failed to parse material: " + possibleName);
                }
            }
        }

        // Se non funziona, prova a cercare nel display item della GUI (slot 13)
        if (material == null) {
            ItemStack displayItem = player.getOpenInventory().getItem(13); // Item centrale
            if (displayItem != null && displayItem.getType() != Material.AIR) {
                material = displayItem.getType();
                plugin.getLogger().info("Extracted item from display slot: " + material);
            }
        }

        return material;
    }

    private void processBuyTransaction(Player player, Material material, int amount) {
        plugin.getLogger().info("Starting buy transaction for " + player.getName() + ": " + amount + "x " + material);

        plugin.getShopManager().buyItem(player, material, amount).thenAccept(result -> {
            plugin.getLogger().info("Buy transaction result: " + result.isSuccess() + " - " + result.getMessage());

            // Esegui sulla main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (result.isSuccess()) {
                    player.sendMessage(ChatColor.GREEN + result.getMessage());
                    if (plugin.getShopConfig().isSoundEnabled()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
                    }
                } else {
                    player.sendMessage(ChatColor.RED + result.getMessage());
                    if (plugin.getShopConfig().isSoundEnabled()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                    }
                }

                // Refresh the GUI
                plugin.getGUIManager().openTransactionMenu(player, material, true);
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error in buy transaction: " + throwable.getMessage());
            throwable.printStackTrace();

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(ChatColor.RED + "An error occurred during the transaction");
                plugin.getGUIManager().openTransactionMenu(player, material, true);
            });

            return null;
        });
    }

    private void processSellTransaction(Player player, Material material, int amount) {
        plugin.getLogger().info("Starting sell transaction for " + player.getName() + ": " + amount + "x " + material);

        plugin.getShopManager().sellItem(player, material, amount).thenAccept(result -> {
            plugin.getLogger().info("Sell transaction result: " + result.isSuccess() + " - " + result.getMessage());

            // Esegui sulla main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (result.isSuccess()) {
                    player.sendMessage(ChatColor.GREEN + result.getMessage());
                    if (plugin.getShopConfig().isSoundEnabled()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.0f);
                    }
                } else {
                    player.sendMessage(ChatColor.RED + result.getMessage());
                    if (plugin.getShopConfig().isSoundEnabled()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                    }
                }

                // Refresh the GUI
                plugin.getGUIManager().openTransactionMenu(player, material, false);
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error in sell transaction: " + throwable.getMessage());
            throwable.printStackTrace();

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(ChatColor.RED + "An error occurred during the transaction");
                plugin.getGUIManager().openTransactionMenu(player, material, false);
            });

            return null;
        });
    }

    private int countItemsInInventory(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            plugin.getGUIManager().clearPlayerData(player.getUniqueId());
        }
    }
}