package com.Lino.dynamicShopGUI.commands;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

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
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("dynamicshop.admin")) {
                    sender.sendMessage(plugin.getShopConfig().getMessage("commands.no-permission-reload"));
                    return true;
                }

                plugin.getShopConfig().reload();
                sender.sendMessage(plugin.getShopConfig().getMessage("commands.reload-success"));
                return true;
            } else if (args.length > 1 && args[0].equalsIgnoreCase("give") && args[1].equalsIgnoreCase("sellchest")) {
                return sellChestCommand.onCommand(sender, command, label, args);
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
}
