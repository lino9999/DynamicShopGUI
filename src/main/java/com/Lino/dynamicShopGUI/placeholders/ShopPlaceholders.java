package com.Lino.dynamicShopGUI.placeholders;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.models.ShopItem;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class ShopPlaceholders extends PlaceholderExpansion {

    private final DynamicShopGUI plugin;

    public ShopPlaceholders(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "dynamicshop";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Lino";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // Placeholder: %dynamicshop_price_buy_DIAMOND%
        if (params.startsWith("price_buy_")) {
            return getPrice(params.substring(10), true);
        }

        // Placeholder: %dynamicshop_price_sell_DIAMOND%
        if (params.startsWith("price_sell_")) {
            return getPrice(params.substring(11), false);
        }

        // Placeholder: %dynamicshop_stock_DIAMOND%
        if (params.startsWith("stock_")) {
            return getStock(params.substring(6));
        }

        // Placeholder: %dynamicshop_max_stock_DIAMOND%
        if (params.startsWith("max_stock_")) {
            return getMaxStock(params.substring(10));
        }

        return null;
    }

    private String getPrice(String matName, boolean isBuy) {
        Material material = parseMaterial(matName);
        if (material == null) return "Invalid Item";

        ShopItem item = fetchItem(material);
        if (item == null) return "N/A";

        double price = isBuy ? item.getBuyPrice() : item.getSellPrice();
        return String.format("%.2f", price);
    }

    private String getStock(String matName) {
        Material material = parseMaterial(matName);
        if (material == null) return "Invalid Item";

        ShopItem item = fetchItem(material);
        if (item == null) return "N/A";

        return String.valueOf(item.getStock());
    }

    private String getMaxStock(String matName) {
        Material material = parseMaterial(matName);
        if (material == null) return "Invalid Item";

        ShopItem item = fetchItem(material);
        if (item == null) return "N/A";

        return String.valueOf(item.getMaxStock());
    }

    private Material parseMaterial(String name) {
        Material material = Material.matchMaterial(name);
        if (material == null) {
            // Tenta di correggere formati comuni (es. diamond_pickaxe invece di DIAMOND_PICKAXE)
            material = Material.matchMaterial(name.toUpperCase().replace("-", "_").replace(" ", "_"));
        }
        return material;
    }

    private ShopItem fetchItem(Material material) {
        // Nota: join() qui blocca momentaneamente il thread per recuperare il dato aggiornato.
        // Dato che usiamo SQLite locale, l'impatto è minimo, ma garantisce dati real-time.
        try {
            return plugin.getDatabaseManager().getShopItem(material).join();
        } catch (Exception e) {
            return null;
        }
    }
}