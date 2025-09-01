package com.Lino.dynamicShopGUI.utils;

import org.bukkit.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradientColor {

    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:(#[a-fA-F0-9]{6}):(#[a-fA-F0-9]{6})>(.*?)</gradient>");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("%[a-zA-Z_]+%");

    public static String apply(String text) {
        if (text == null) return null;

        text = ChatColor.translateAlternateColorCodes('&', text);

        Matcher gradientMatcher = GRADIENT_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (gradientMatcher.find()) {
            String startColor = gradientMatcher.group(1);
            String endColor = gradientMatcher.group(2);
            String content = gradientMatcher.group(3);

            String gradientText = createGradient(content, startColor, endColor);
            gradientMatcher.appendReplacement(result, Matcher.quoteReplacement(gradientText));
        }
        gradientMatcher.appendTail(result);

        text = result.toString();

        Matcher hexMatcher = HEX_PATTERN.matcher(text);
        result = new StringBuffer();

        while (hexMatcher.find()) {
            String hexColor = hexMatcher.group();
            String replacement = net.md_5.bungee.api.ChatColor.of(hexColor).toString();
            hexMatcher.appendReplacement(result, replacement);
        }
        hexMatcher.appendTail(result);

        return result.toString();
    }

    public static String applyWithVariables(String text, Object... replacements) {
        if (text == null) return null;

        if (replacements.length % 2 != 0) {
            return apply(text);
        }

        for (int i = 0; i < replacements.length; i += 2) {
            String variable = replacements[i].toString();
            String value = replacements[i + 1].toString();
            text = text.replace(variable, value);
        }

        return apply(text);
    }

    private static String createGradient(String text, String startHex, String endHex) {
        if (text.isEmpty()) return text;

        int[] startRGB = hexToRGB(startHex);
        int[] endRGB = hexToRGB(endHex);

        StringBuilder gradientBuilder = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if (c == ' ') {
                gradientBuilder.append(' ');
                continue;
            }

            float ratio = (float) i / (length - 1);
            int r = Math.round(startRGB[0] + ratio * (endRGB[0] - startRGB[0]));
            int g = Math.round(startRGB[1] + ratio * (endRGB[1] - startRGB[1]));
            int b = Math.round(startRGB[2] + ratio * (endRGB[2] - startRGB[2]));

            String hex = String.format("#%02x%02x%02x", r, g, b);
            gradientBuilder.append(net.md_5.bungee.api.ChatColor.of(hex)).append(c);
        }

        return gradientBuilder.toString();
    }

    private static int[] hexToRGB(String hex) {
        hex = hex.replace("#", "");
        return new int[] {
                Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16),
                Integer.parseInt(hex.substring(4, 6), 16)
        };
    }
}