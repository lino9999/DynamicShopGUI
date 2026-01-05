package com.Lino.dynamicShopGUI.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ShopTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("dynamicshop.use")) {
                completions.add("help");
            }
            if (sender.hasPermission("dynamicshop.admin")) {
                completions.add("reload");
                completions.add("top");
                completions.add("give");
                completions.add("bestsellers");
                completions.add("open");
                completions.add("debugItem");
            }
            return StringUtil.copyPartialMatches(args[0], completions, new ArrayList<>());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") && sender.hasPermission("dynamicshop.admin")) {
                return StringUtil.copyPartialMatches(args[1], Arrays.asList("sellchest", "hoe"), new ArrayList<>());
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give") && sender.hasPermission("dynamicshop.admin")) {
                return null;
            }
            if (args[0].equalsIgnoreCase("open") && sender.hasPermission("dynamicshop.admin")) {
                List<String> onlinePlayers = new ArrayList<>(List.of());
                for (Player player : Bukkit.getOnlinePlayers()) {
                    onlinePlayers.add(player.getName());
                }
                return StringUtil.copyPartialMatches(args[2], onlinePlayers, new ArrayList<>());
            }
        }

        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("give") && sender.hasPermission("dynamicshop.admin")) {
                return StringUtil.copyPartialMatches(args[3], Arrays.asList("1", "64"), new ArrayList<>());
            }
        }

        return Collections.emptyList();
    }
}