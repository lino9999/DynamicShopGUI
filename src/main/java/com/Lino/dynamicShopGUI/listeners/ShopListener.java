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

        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }

        Material material = clicked.getType();
        boolean isBuying = clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT;

        // Check if item is out of stock
        if (clicked.getItemMeta() != null && clicked.getItemMeta().getLore() != null) {
            for (String loreLine : clicked.getItemMeta().getLore()) {
                if (loreLine.contains("Out of Stock")) {
                    player.sendMessage(ChatColor.RED + "This item is out of stock!");
                    if (plugin.getShopConfig().isSoundEnabled()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                    }
                    return;
                }
            }
        }

        plugin.getGUIManager().openTransactionMenu(player, material, isBuying);
    }

    private void handleTransactionMenuClick(Player player, ItemStack clicked, boolean currentlyBuying) {
        Material type = clicked.getType();

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
            Material selectedItem = plugin.getGUIManager().getPlayerSelectedItem(player.getUniqueId());
            if (selectedItem != null) {
                plugin.getGUIManager().openTransactionMenu(player, selectedItem, !currentlyBuying);
            }
            return;
        }

        if (type == Material.LIME_STAINED_GLASS_PANE || type == Material.RED_STAINED_GLASS_PANE) {
            if (clicked.getItemMeta() == null || clicked.getItemMeta().getDisplayName() == null) return;

            String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            Material selectedItem = plugin.getGUIManager().getPlayerSelectedItem(player.getUniqueId());

            if (selectedItem == null) return;

            int amount = 0;

            // Extract amount from the display name
            if (name.contains("All")) {
                amount = countItemsInInventory(player, selectedItem);
            } else {
                // Extract number from the string
                String[] parts = name.split(" ");
                for (String part : parts) {
                    try {
                        amount = Integer.parseInt(part);
                        break;
                    } catch (NumberFormatException ignored) {}
                }
            }

            if (amount > 0) {
                if (currentlyBuying) {
                    processBuyTransaction(player, selectedItem, amount);
                } else {
                    processSellTransaction(player, selectedItem, amount);
                }
            }
        }
    }

    private void processBuyTransaction(Player player, Material material, int amount) {
        plugin.getShopManager().buyItem(player, material, amount).thenAccept(result -> {
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

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getGUIManager().openTransactionMenu(player, material, true);
            });
        });
    }

    private void processSellTransaction(Player player, Material material, int amount) {
        plugin.getShopManager().sellItem(player, material, amount).thenAccept(result -> {
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

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getGUIManager().openTransactionMenu(player, material, false);
            });
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