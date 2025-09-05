package com.Lino.dynamicShopGUI.utils;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import org.bukkit.Material;

public class GUIUtils {

    public static String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        StringBuilder formatted = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : name.toCharArray()) {
            if (c == ' ') {
                formatted.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                formatted.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                formatted.append(c);
            }
        }

        return formatted.toString();
    }

    public static String formatPriceChange(DynamicShopGUI plugin, double percent) {
        if (percent > 0) {
            return plugin.getShopConfig().getMessage("gui.price-increase",
                    "%percent%", String.format("%.1f", percent));
        } else if (percent < 0) {
            return plugin.getShopConfig().getMessage("gui.price-decrease",
                    "%percent%", String.format("%.1f", Math.abs(percent)));
        } else {
            return plugin.getShopConfig().getMessage("gui.price-stable");
        }
    }
}