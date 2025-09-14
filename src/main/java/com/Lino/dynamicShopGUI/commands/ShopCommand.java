package com.Lino.dynamicShopGUI.commands;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShopCommand implements CommandExecutor {

    private final DynamicShopGUI plugin;
    private final SellChestCommand sellChestCommand;

    public ShopCommand(DynamicShopGUI plugin) {
        this.plugin = plugin;
        this.sellChestCommand = new SellChestCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("help")) {
                sendHelpMessage(sender);
                return true;
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("dynamicshop.admin")) {
                    sender.sendMessage(plugin.getShopConfig().getMessage("commands.no-permission-reload"));
                    return true;
                }

                plugin.getShopConfig().reload();
                sender.sendMessage(plugin.getShopConfig().getMessage("commands.reload-success"));
                return true;
            } else if (args.length > 1 && args[0].equalsIgnoreCase("give")) {
                if (!sender.hasPermission("dynamicshop.admin")) {
                    sender.sendMessage(plugin.getShopConfig().getMessage("commands.no-permission"));
                    return true;
                }

                if (args[1].equalsIgnoreCase("sellchest")) {
                    return sellChestCommand.onCommand(sender, command, label, args);
                } else if (args[1].equalsIgnoreCase("hoe")) {
                    return handleGiveHoe(sender, args);
                }
            }
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getShopConfig().getMessage("commands.players-only"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("dynamicshop.use")) {
            player.sendMessage(plugin.getShopConfig().getMessage("commands.no-permission"));
            return true;
        }

        plugin.getGUIManager().openMainMenu(player);
        return true;
    }

    private boolean handleGiveHoe(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /shop give hoe <player> [amount]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(plugin.getShopConfig().getMessage("errors.player-not-found"));
            return true;
        }

        int amount = 1;
        if (args.length > 3) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount < 1) amount = 1;
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount specified.");
                return true;
            }
        }

        ItemStack hoe = createAutoHarvesterHoe(amount);
        target.getInventory().addItem(hoe);

        target.sendMessage(plugin.getShopConfig().getMessage("auto-harvester.received",
                "%amount%", String.valueOf(amount)));
        sender.sendMessage(plugin.getShopConfig().getMessage("auto-harvester.admin-give-success",
                "%amount%", String.valueOf(amount),
                "%player%", target.getName()));

        return true;
    }

    private ItemStack createAutoHarvesterHoe(int amount) {
        ItemStack hoe = new ItemStack(Material.DIAMOND_HOE, amount);
        ItemMeta meta = hoe.getItemMeta();

        meta.setDisplayName(plugin.getShopConfig().getMessage("auto-harvester.hoe-name"));

        List<String> lore = plugin.getShopConfig().getMessageList("auto-harvester.hoe-lore");

        if (lore.isEmpty()) {
            lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Automatically harvests crops");
            lore.add(ChatColor.GRAY + "Sells them instantly");
            lore.add(ChatColor.GRAY + "and replants seeds!");
            lore.add(ChatColor.DARK_GRAY + "auto-harvester-hoe");
        }

        meta.setLore(lore);
        hoe.setItemMeta(meta);

        return hoe;
    }

    private void sendHelpMessage(CommandSender sender) {
        List<String> helpMessages = plugin.getShopConfig().getMessageList("commands.help-message");

        if (helpMessages.isEmpty()) {
            sender.sendMessage(plugin.getShopConfig().getMessage("commands.help-header"));
            sender.sendMessage("");
            sender.sendMessage(plugin.getShopConfig().getMessage("commands.help-shop"));
            sender.sendMessage(plugin.getShopConfig().getMessage("commands.help-help"));

            if (sender.hasPermission("dynamicshop.admin")) {
                sender.sendMessage("");
                sender.sendMessage(plugin.getShopConfig().getMessage("commands.help-admin-header"));
                sender.sendMessage(plugin.getShopConfig().getMessage("commands.help-reload"));
                sender.sendMessage(plugin.getShopConfig().getMessage("commands.help-give-sellchest"));
                sender.sendMessage(plugin.getShopConfig().getMessage("commands.help-give-hoe"));
            }

            sender.sendMessage("");
            sender.sendMessage(plugin.getShopConfig().getMessage("commands.help-footer"));
        } else {
            for (String line : helpMessages) {
                sender.sendMessage(line);
            }
        }
    }
}