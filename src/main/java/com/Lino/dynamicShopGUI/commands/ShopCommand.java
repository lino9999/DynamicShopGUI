package com.Lino.dynamicShopGUI.commands;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

    private final DynamicShopGUI plugin;

    public ShopCommand(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("dynamicshop.admin")) {
                sender.sendMessage(plugin.getShopConfig().getMessage("commands.no-permission-reload"));
                return true;
            }

            plugin.getShopConfig().reload();
            sender.sendMessage(plugin.getShopConfig().getMessage("commands.reload-success"));
            return true;
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
}