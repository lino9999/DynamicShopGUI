package com.Lino.dynamicShopGUI.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SellTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("dynamicshop.sell")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("hand");
            completions.add("all");
            return completions;
        }

        return Collections.emptyList();
    }
}