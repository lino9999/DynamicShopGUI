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

public class SellChestCommand implements CommandExecutor {

    private final DynamicShopGUI plugin;

    public SellChestCommand(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dynamicshop.admin")) {
            sender.sendMessage(plugin.getShopConfig().getMessage("commands.no-permission"));
            return true;
        }

        if (args.length < 3 || !args[0].equalsIgnoreCase("give") || !args[1].equalsIgnoreCase("sellchest")) {
            sender.sendMessage(ChatColor.RED + "Usage: /shop give sellchest <player> <amount>");
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
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount specified.");
                return true;
            }
        }

        ItemStack sellChest = new ItemStack(Material.CHEST, amount);
        ItemMeta meta = sellChest.getItemMeta();

        meta.setDisplayName(plugin.getShopConfig().getMessage("autosell.chest-name"));

        List<String> lore = plugin.getShopConfig().getMessageList("autosell.chest-lore");

        if (lore.isEmpty()) {
            lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Place this chest to sell");
            lore.add(ChatColor.GRAY + "its contents automatically");
            lore.add(ChatColor.GRAY + "every 5 minutes.");
            lore.add(ChatColor.DARK_GRAY + "autosell-chest");
        }

        meta.setLore(lore);
        sellChest.setItemMeta(meta);

        target.getInventory().addItem(sellChest);
        target.sendMessage(plugin.getShopConfig().getMessage("autosell.received", "%amount%", String.valueOf(amount)));
        sender.sendMessage(plugin.getShopConfig().getMessage("autosell.admin-give-success", "%amount%", String.valueOf(amount), "%player%", target.getName()));

        return true;
    }
}