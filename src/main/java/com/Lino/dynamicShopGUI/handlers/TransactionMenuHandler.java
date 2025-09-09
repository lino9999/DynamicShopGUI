package com.Lino.dynamicShopGUI.handlers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class TransactionMenuHandler {

    private final DynamicShopGUI plugin;

    public TransactionMenuHandler(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    public void handleClick(Player player, ItemStack clicked, boolean currentlyBuying, int slot) {
        Material type = clicked.getType();

        Material selectedItem = plugin.getGUIManager().getPlayerSelectedItem(player.getUniqueId());

        if (selectedItem == null) {
            selectedItem = extractMaterialFromGUI(player);
            if (selectedItem != null) {
                plugin.getGUIManager().setPlayerSelectedItem(player.getUniqueId(), selectedItem);
            }
        }

        if (slot == 49 && type == Material.ARROW) {
            String category = plugin.getGUIManager().getPlayerCategory(player.getUniqueId());
            if (category != null) {
                int page = plugin.getGUIManager().getPlayerPage(player.getUniqueId());
                plugin.getGUIManager().openCategoryMenu(player, category, page);
            } else {
                plugin.getGUIManager().openMainMenu(player);
            }
            return;
        }

        if (slot == 22 && type == Material.HOPPER) {
            if (selectedItem != null) {
                plugin.getGUIManager().openTransactionMenu(player, selectedItem, !currentlyBuying);
            } else {
                player.sendMessage(plugin.getShopConfig().getMessage("errors.no-item-selected"));
            }
            return;
        }

        if (slot == 13 || slot == 12 || slot == 14 || slot == 40 || slot >= 45) {
            return;
        }

        if (type == Material.BLACK_STAINED_GLASS_PANE || type == Material.GRAY_STAINED_GLASS_PANE ||
                type == Material.IRON_BLOCK || type == Material.COAL_BLOCK ||
                type == Material.EMERALD_BLOCK || type == Material.REDSTONE_BLOCK ||
                type == Material.LAPIS_BLOCK || type == Material.LIME_STAINED_GLASS ||
                type == Material.RED_STAINED_GLASS) {
            return;
        }

        if (slot >= 29 && slot <= 33) {
            boolean isValidButton = (currentlyBuying && type == Material.LIME_STAINED_GLASS_PANE) ||
                    (!currentlyBuying && type == Material.RED_STAINED_GLASS_PANE);

            if (isValidButton) {
                if (clicked.getItemMeta() == null || clicked.getItemMeta().getDisplayName() == null) {
                    return;
                }

                String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

                if (selectedItem == null) {
                    player.sendMessage(plugin.getShopConfig().getMessage("errors.item-error"));
                    return;
                }

                int amount = extractAmount(name, currentlyBuying, player, selectedItem, slot);

                if (amount > 0) {
                    if (currentlyBuying) {
                        processBuyTransaction(player, selectedItem, amount);
                    } else {
                        processSellTransaction(player, selectedItem, amount);
                    }
                } else {
                    player.sendMessage(plugin.getShopConfig().getMessage("errors.amount-error"));
                }
            }
        }
    }

    private int extractAmount(String name, boolean currentlyBuying, Player player, Material material, int slot) {
        int amount = 0;

        switch (slot) {
            case 29:
                amount = 1;
                break;
            case 30:
                amount = 10;
                break;
            case 31:
                amount = 32;
                break;
            case 32:
                amount = 64;
                break;
            case 33:
                if (!currentlyBuying) {
                    amount = countItemsInInventory(player, material);
                } else {
                    amount = 128;
                }
                break;
        }

        return amount;
    }

    private Material extractMaterialFromGUI(Player player) {
        String title = ChatColor.stripColor(player.getOpenInventory().getTitle());
        String cleanTitle = title;

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
                    player.sendMessage(plugin.getShopConfig().getPrefix() + result.getMessage());
                    if (plugin.getShopConfig().isSoundEnabled()) {
                        player.playSound(player.getLocation(), "entity.player.levelup", 0.7f, 1.2f);
                    }
                } else {
                    player.sendMessage(plugin.getShopConfig().getPrefix() + result.getMessage());
                    if (plugin.getShopConfig().isSoundEnabled()) {
                        player.playSound(player.getLocation(), "entity.villager.no", 0.5f, 1.0f);
                    }
                }

                plugin.getGUIManager().openTransactionMenu(player, material, true);
            });
        }).exceptionally(throwable -> {
            throwable.printStackTrace();

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(plugin.getShopConfig().getMessage("errors.transaction-error"));
                plugin.getGUIManager().openTransactionMenu(player, material, true);
            });

            return null;
        });
    }

    private void processSellTransaction(Player player, Material material, int amount) {
        plugin.getShopManager().sellItem(player, material, amount).thenAccept(result -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (result.isSuccess()) {
                    player.sendMessage(plugin.getShopConfig().getPrefix() + result.getMessage());
                    if (plugin.getShopConfig().isSoundEnabled()) {
                        player.playSound(player.getLocation(), "entity.experience_orb.pickup", 0.7f, 1.0f);
                    }
                } else {
                    player.sendMessage(plugin.getShopConfig().getPrefix() + result.getMessage());
                    if (plugin.getShopConfig().isSoundEnabled()) {
                        player.playSound(player.getLocation(), "entity.villager.no", 0.5f, 1.0f);
                    }
                }

                plugin.getGUIManager().openTransactionMenu(player, material, false);
            });
        }).exceptionally(throwable -> {
            throwable.printStackTrace();

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(plugin.getShopConfig().getMessage("errors.transaction-error"));
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
}