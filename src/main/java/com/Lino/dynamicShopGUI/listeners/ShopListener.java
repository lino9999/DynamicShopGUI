package com.Lino.dynamicShopGUI.listeners;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.config.CategoryConfigLoader;
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
import java.util.Map;

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
        if (clicked.getItemMeta() == null || clicked.getItemMeta().getDisplayName() == null) return;

        String clickedName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        Map<String, CategoryConfigLoader.CategoryConfig> categories = plugin.getShopConfig().getAllCategories();

        for (Map.Entry<String, CategoryConfigLoader.CategoryConfig> entry : categories.entrySet()) {
            if (entry.getValue().getDisplayName().equalsIgnoreCase(clickedName)) {
                plugin.getGUIManager().openCategoryMenu(player, entry.getKey(), 0);
                return;
            }
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

        plugin.getGUIManager().setPlayerSelectedItem(player.getUniqueId(), material);
        plugin.getGUIManager().openTransactionMenu(player, material, isBuying);
    }

    private void handleTransactionMenuClick(Player player, ItemStack clicked, boolean currentlyBuying) {
        Material type = clicked.getType();

        Material selectedItem = plugin.getGUIManager().getPlayerSelectedItem(player.getUniqueId());

        if (selectedItem == null) {
            selectedItem = extractMaterialFromGUI(player);
            if (selectedItem != null) {
                plugin.getGUIManager().setPlayerSelectedItem(player.getUniqueId(), selectedItem);
            }
        }

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
                plugin.getGUIManager().openTransactionMenu(player, selectedItem, !currentlyBuying);
            } else {
                player.sendMessage(ChatColor.RED + "Error: No item selected. Please close and reopen the shop.");
            }
            return;
        }

        if (type == Material.LIME_STAINED_GLASS_PANE || type == Material.RED_STAINED_GLASS_PANE) {
            if (clicked.getItemMeta() == null || clicked.getItemMeta().getDisplayName() == null) {
                return;
            }

            String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

            if (selectedItem == null) {
                player.sendMessage(ChatColor.RED + "Error: Could not determine the item. Please try reopening the shop.");
                return;
            }

            int amount = 0;

            if (name.toLowerCase().contains("all")) {
                if (!currentlyBuying) {
                    amount = countItemsInInventory(player, selectedItem);
                } else {
                    return;
                }
            } else {
                String[] parts = name.split(" ");
                for (String part : parts) {
                    try {
                        amount = Integer.parseInt(part);
                        break;
                    } catch (NumberFormatException ignored) {}
                }

                if (amount == 0) {
                    String numberOnly = name.replaceAll("[^0-9]", "");
                    if (!numberOnly.isEmpty()) {
                        try {
                            amount = Integer.parseInt(numberOnly);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            if (amount > 0) {
                if (currentlyBuying) {
                    processBuyTransaction(player, selectedItem, amount);
                } else {
                    processSellTransaction(player, selectedItem, amount);
                }
            } else {
                player.sendMessage(ChatColor.RED + "Error: Could not determine amount to trade");
            }
        }
    }

    private Material extractMaterialFromGUI(Player player) {
        String title = player.getOpenInventory().getTitle();
        String cleanTitle = ChatColor.stripColor(title);

        Material material = null;

        if (cleanTitle.startsWith("Buy ") || cleanTitle.startsWith("Sell ")) {
            String itemName = cleanTitle.substring(4).trim();

            String[] possibleNames = {
                    itemName.toUpperCase().replace(" ", "_"),
                    itemName.toUpperCase().replace(" ", ""),
                    itemName.replace(" ", "_").toUpperCase()
            };

            for (String possibleName : possibleNames) {
                try {
                    material = Material.valueOf(possibleName);
                    break;
                } catch (IllegalArgumentException e) {
                    // Continue trying
                }
            }
        }

        if (material == null) {
            ItemStack displayItem = player.getOpenInventory().getItem(13);
            if (displayItem != null && displayItem.getType() != Material.AIR) {
                material = displayItem.getType();
            }
        }

        return material;
    }

    private void processBuyTransaction(Player player, Material material, int amount) {
        plugin.getShopManager().buyItem(player, material, amount).thenAccept(result -> {
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

                plugin.getGUIManager().openTransactionMenu(player, material, true);
            });
        }).exceptionally(throwable -> {
            throwable.printStackTrace();

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(ChatColor.RED + "An error occurred during the transaction");
                plugin.getGUIManager().openTransactionMenu(player, material, true);
            });

            return null;
        });
    }

    private void processSellTransaction(Player player, Material material, int amount) {
        plugin.getShopManager().sellItem(player, material, amount).thenAccept(result -> {
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

                plugin.getGUIManager().openTransactionMenu(player, material, false);
            });
        }).exceptionally(throwable -> {
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