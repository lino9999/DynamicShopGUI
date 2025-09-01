package com.Lino.dynamicShopGUI.commands;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.utils.GradientColor;
import org.bukkit.ChatColor;
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
                sender.sendMessage(GradientColor.apply("<gradient:#ff0000:#ff8800>You don't have permission to reload the shop!</gradient>"));
                return true;
            }

            plugin.getShopConfig().reload();
            sender.sendMessage(GradientColor.apply("<gradient:#00ff00:#00ffff>Shop configuration reloaded!</gradient>"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(GradientColor.apply("<gradient:#ff0000:#ff8800>This command can only be used by players!</gradient>"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("dynamicshop.use")) {
            player.sendMessage(GradientColor.apply("<gradient:#ff0000:#ff8800>You don't have permission to use the shop!</gradient>"));
            return true;
        }

        plugin.getGUIManager().openMainMenu(player);
        return true;
    }
}