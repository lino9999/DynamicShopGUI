package com.Lino.dynamicShopGUI.commands;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
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
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("dynamicshop.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use the shop!");
            return true;
        }

        plugin.getGUIManager().openMainMenu(player);
        return true;
    }
}