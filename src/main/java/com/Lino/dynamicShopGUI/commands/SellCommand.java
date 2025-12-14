package com.Lino.dynamicShopGUI.commands;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SellCommand implements CommandExecutor {

    private final DynamicShopGUI plugin;

    public SellCommand(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getShopConfig().getMessage("commands.players-only"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("dynamicshop.use")) {
            player.sendMessage(plugin.getShopConfig().getMessage("commands.no-permission"));
            return true;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("all")) {
                plugin.getShopManager().sellAllItems(player);
                return true;
            } else if (args[0].equalsIgnoreCase("hand")) {
                plugin.getShopManager().sellHandItem(player);
                return true;
            }
        }

        plugin.getShopManager().sellHandItem(player);
        return true;
    }
}