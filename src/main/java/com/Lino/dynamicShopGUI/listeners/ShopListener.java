package com.Lino.dynamicShopGUI.listeners;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.managers.ShopManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

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

        if (title.equals(ChatColor.DARK_GREEN + "Dynamic Shop")) {
            handleMainMenuClick(player, clicked);
        } else if (title.startsWith(ChatColor.DARK_GREEN + "Shop -")) {
            handleCategoryMenuClick(player, clicked, event.getClick());
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
            plugin.getGUIManager().openCategoryMenu(player, category);
        }
    }

    private void handleCategoryMenuClick(Player player, ItemStack clicked, ClickType clickType) {
        if (clicked.getType() == Material.ARROW) {
            plugin.getGUIManager().openMainMenu(player);
            return;
        }

        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }

        Material material = clicked.getType();
        boolean isBuying = clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT;

        plugin.getGUIManager().openTransactionMenu(player, material, isBuying);
    }

    private void handleTransactionMenuClick(Player player, ItemStack clicked, boolean currentlyBuying) {
        Material type = clicked.getType();

        if (type == Material.ARROW) {
            String category = plugin.getGUIManager().getPlayerCategory(player.getUniqueId());
            if (category != null) {
                plugin.getGUIManager().openCategoryMenu(player, category);
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
            String name = clicked.getItemMeta().getDisplayName();
            Material selectedItem = plugin.getGUIManager().getPlayerSelectedItem(player.getUniqueId());

            if (selectedItem == null) return;

            int amount = 0;
            if (name.contains("1") && !name.contains("10") && !name.contains("128")) {
                amount = 1;
            } else if (name.contains("10")) {
                amount = 10;
            } else if (name.contains("32")) {
                amount = 32;
            } else if (name.contains("64")) {
                amount = 64;
            } else if (name.contains("128")) {
                amount = 128;
            } else if (name.contains("All")) {
                amount = countItemsInInventory(player, selectedItem);
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
            } else {
                player.sendMessage(ChatColor.RED + result.getMessage());
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
            } else {
                player.sendMessage(ChatColor.RED + result.getMessage());
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