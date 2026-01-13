package com.Lino.dynamicShopGUI.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComponentParser {

    public static final class FishStats {
        public final Double size;
        public final Double weight;
        public final String rarity;

        public FishStats(Double size, Double weight, String rarity) {
            this.size = size;
            this.weight = weight;
            this.rarity = rarity;
        }
    }

    public static FishStats parseFishStats(String componentString) {
        if (componentString == null || componentString.isBlank()) return new FishStats(null,null,null);

        double size = parseNumberField(componentString, "size");
        double weight = parseNumberField(componentString, "weight");
        String rarity = parseStringField(componentString, "rarity");

        return new FishStats(size, weight, rarity);
    }

    private static double parseNumberField(String componentString, String key) {
        Pattern pattern = Pattern.compile(
                "(?i)(?:\\b|\\\")" + Pattern.quote(key) + "(?:\\b|\\\")\\s*[:=]\\s*([-+]?\\d+(?:\\.\\d+)?)([dDfFlL]?)"
        );
        Matcher matcher = pattern.matcher(componentString);
        if (!matcher.find()) return 0;

        String number = matcher.group(1);
        try {
            return Double.parseDouble(number);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String parseStringField(String componentString, String key) {
        Pattern pattern = Pattern.compile(
                "(?i)(?:\\b|\\\")" + Pattern.quote(key) + "(?:\\b|\\\")\\s*[:=]\\s*(\"[^\"]*\"|'[^']*'|[^,}\\]\\s]+)"
        );
        Matcher matcher = pattern.matcher(componentString);
        if (!matcher.find()) return null;

        String raw = matcher.group(1);
        return stripQuotes(raw.trim());
    }

    private static String stripQuotes(String string) {
        if (string == null) return null;
        string = string.trim();
        if ((string.startsWith("\"") && string.endsWith("\"")) || (string.startsWith("'") && string.endsWith("'"))) {
            return string.substring(1, string.length() - 1);
        }
        return string;
    }
}
