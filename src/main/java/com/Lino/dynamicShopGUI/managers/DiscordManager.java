package com.Lino.dynamicShopGUI.managers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.models.ShopItem;
import com.Lino.dynamicShopGUI.utils.GUIUtils;
import org.bukkit.Bukkit;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordManager {

    private final DynamicShopGUI plugin;

    public DiscordManager(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    public void sendOutOfStockAlert(ShopItem item) {
        if (!plugin.getConfig().getBoolean("discord.enabled", false)) {
            return;
        }

        String webhookUrl = plugin.getConfig().getString("discord.webhook-url");
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("YOUR_WEBHOOK_URL_HERE")) {
            return;
        }

        String itemName = GUIUtils.formatMaterialName(item.getMaterial());
        double price = item.getCurrentPrice();

        // Simple JSON construction to avoid external dependencies
        String jsonPayload = "{"
                + "\"embeds\": [{"
                + "\"title\": \"Out of Stock Alert!\","
                + "\"description\": \"The item **" + itemName + "** is now out of stock.\","
                + "\"color\": 16711680," // Red color
                + "\"fields\": ["
                + "{\"name\": \"Item\", \"value\": \"" + itemName + "\", \"inline\": true},"
                + "{\"name\": \"Current Price\", \"value\": \"$" + String.format("%.2f", price) + "\", \"inline\": true}"
                + "]"
                + "}]"
                + "}";

        sendAsyncRequest(webhookUrl, jsonPayload);
    }

    private void sendAsyncRequest(String urlStr, String jsonBody) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "DynamicShopGUI-Webhook");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    plugin.getLogger().warning("Failed to send Discord webhook. Response code: " + responseCode);
                }
                connection.disconnect();

            } catch (Exception e) {
                plugin.getLogger().warning("Error sending Discord webhook: " + e.getMessage());
            }
        });
    }
}