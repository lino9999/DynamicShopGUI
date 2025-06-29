package com.Lino.dynamicShopGUI.managers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.models.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class GUIManager {

    private final DynamicShopGUI plugin;
    private final Map<UUID, String> playerCategory = new HashMap<>();
    private final Map<UUID, Material> playerSelectedItem = new HashMap<>();

    public GUIManager(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GREEN + "Dynamic Shop");

        setCategory(inv, 10, Material.STONE, ChatColor.GRAY + "Building Blocks", "BUILDING");
        setCategory(inv, 11, Material.DIAMOND, ChatColor.AQUA + "Ores & Minerals", "ORES");
        setCategory(inv, 12, Material.BREAD, ChatColor.GOLD + "Food", "FOOD");
        setCategory(inv, 13, Material.DIAMOND_PICKAXE, ChatColor.BLUE + "Tools", "TOOLS");
        setCategory(inv, 14, Material.DIAMOND_CHESTPLATE, ChatColor.DARK_AQUA + "Armor", "ARMOR");
        setCategory(inv, 15, Material.REDSTONE, ChatColor.RED + "Redstone", "REDSTONE");
        setCategory(inv, 16, Material.WHEAT, ChatColor.GREEN + "Farming", "FARMING");
        setCategory(inv, 22, Material.ENDER_PEARL, ChatColor.LIGHT_PURPLE + "Miscellaneous", "MISC");

        fillBorders(inv);
        player.openInventory(inv);
    }

    public void openCategoryMenu(Player player, String category) {
        playerCategory.put(player.getUniqueId(), category);
        String displayName = plugin.getShopConfig().getCategoryDisplayName(category.toLowerCase());
        String title = ChatColor.DARK_GREEN + "Shop - " + displayName;
        Inventory inv = Bukkit.createInventory(null, 54, title);

        plugin.getDatabaseManager().getItemsByCategory(category).thenAccept(items -> {
            List<Map.Entry<Material, ShopItem>> sortedItems = new ArrayList<>(items.entrySet());
            sortedItems.sort(Map.Entry.comparingByKey());

            int slot = 0;
            for (Map.Entry<Material, ShopItem> entry : sortedItems) {
                if (slot >= 45) break;

                ShopItem shopItem = entry.getValue();
                ItemStack displayItem = createShopItemDisplay(shopItem);
                inv.setItem(slot++, displayItem);
            }

            ItemStack backButton = new ItemStack(Material.ARROW);
            ItemMeta backMeta = backButton.getItemMeta();
            backMeta.setDisplayName(ChatColor.RED + "Back to Categories");
            backButton.setItemMeta(backMeta);
            inv.setItem(49, backButton);

            fillBottomBorder(inv);

            Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inv));
        });
    }

    public void openTransactionMenu(Player player, Material material, boolean isBuying) {
        playerSelectedItem.put(player.getUniqueId(), material);

        plugin.getDatabaseManager().getShopItem(material).thenAccept(item -> {
            if (item == null) return;

            String title = isBuying ?
                    ChatColor.DARK_GREEN + "Buy " + formatMaterialName(material) :
                    ChatColor.DARK_RED + "Sell " + formatMaterialName(material);

            Inventory inv = Bukkit.createInventory(null, 54, title);

            ItemStack displayItem = new ItemStack(material);
            ItemMeta displayMeta = displayItem.getItemMeta();
            displayMeta.setDisplayName(ChatColor.YELLOW + formatMaterialName(material));
            List<String> displayLore = new ArrayList<>();
            displayLore.add(ChatColor.GRAY + "Stock: " + ChatColor.WHITE + item.getStock() + "/" + item.getMaxStock());
            displayLore.add(ChatColor.GRAY + "Buy Price: " + ChatColor.GREEN + "$" + String.format("%.2f", item.getBuyPrice()));
            displayLore.add(ChatColor.GRAY + "Sell Price: " + ChatColor.RED + "$" + String.format("%.2f", item.getSellPrice()));
            displayLore.add("");
            displayLore.add(formatPriceChange(item.getPriceChangePercent()));
            displayMeta.setLore(displayLore);
            displayItem.setItemMeta(displayMeta);
            inv.setItem(13, displayItem);

            if (isBuying) {
                setAmountButton(inv, 29, Material.LIME_STAINED_GLASS_PANE, 1, true);
                setAmountButton(inv, 30, Material.LIME_STAINED_GLASS_PANE, 10, true);
                setAmountButton(inv, 31, Material.LIME_STAINED_GLASS_PANE, 32, true);
                setAmountButton(inv, 32, Material.LIME_STAINED_GLASS_PANE, 64, true);
                setAmountButton(inv, 33, Material.LIME_STAINED_GLASS_PANE, 128, true);
            } else {
                setAmountButton(inv, 29, Material.RED_STAINED_GLASS_PANE, 1, false);
                setAmountButton(inv, 30, Material.RED_STAINED_GLASS_PANE, 10, false);
                setAmountButton(inv, 31, Material.RED_STAINED_GLASS_PANE, 32, false);
                setAmountButton(inv, 32, Material.RED_STAINED_GLASS_PANE, 64, false);
                setAmountButton(inv, 33, Material.RED_STAINED_GLASS_PANE, -1, false);
            }

            ItemStack toggleButton = new ItemStack(Material.HOPPER);
            ItemMeta toggleMeta = toggleButton.getItemMeta();
            toggleMeta.setDisplayName(isBuying ? ChatColor.GOLD + "Switch to Sell" : ChatColor.GOLD + "Switch to Buy");
            toggleButton.setItemMeta(toggleMeta);
            inv.setItem(22, toggleButton);

            ItemStack backButton = new ItemStack(Material.ARROW);
            ItemMeta backMeta = backButton.getItemMeta();
            backMeta.setDisplayName(ChatColor.RED + "Back");
            backButton.setItemMeta(backMeta);
            inv.setItem(49, backButton);

            fillBorders(inv);

            Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inv));
        });
    }

    private ItemStack createShopItemDisplay(ShopItem item) {
        ItemStack display = new ItemStack(item.getMaterial());
        ItemMeta meta = display.getItemMeta();

        meta.setDisplayName(ChatColor.YELLOW + formatMaterialName(item.getMaterial()));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Stock: " + ChatColor.WHITE + item.getStock() + "/" + item.getMaxStock());
        lore.add("");
        lore.add(ChatColor.GRAY + "Buy Price: " + ChatColor.GREEN + "$" + String.format("%.2f", item.getBuyPrice()));
        lore.add(ChatColor.GRAY + "Sell Price: " + ChatColor.RED + "$" + String.format("%.2f", item.getSellPrice()));
        lore.add("");
        lore.add(formatPriceChange(item.getPriceChangePercent()));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Left Click to Buy");
        lore.add(ChatColor.YELLOW + "Right Click to Sell");

        meta.setLore(lore);
        display.setItemMeta(meta);

        return display;
    }

    private void setCategory(Inventory inv, int slot, Material material, String name, String category) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Click to browse " + category.toLowerCase() + " items");
        meta.setLore(lore);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private void setAmountButton(Inventory inv, int slot, Material material, int amount, boolean isBuying) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String action = isBuying ? "Buy" : "Sell";
        String amountText = amount == -1 ? "All" : String.valueOf(amount);

        meta.setDisplayName(ChatColor.GREEN + action + " " + amountText);

        if (amount == -1) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Sell all items in inventory");
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private void fillBorders(Inventory inv) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        int size = inv.getSize();
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
            if (inv.getItem(size - 9 + i) == null) inv.setItem(size - 9 + i, filler);
        }

        for (int i = 0; i < size; i += 9) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
            if (inv.getItem(i + 8) == null) inv.setItem(i + 8, filler);
        }
    }

    private void fillBottomBorder(Inventory inv) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        for (int i = 45; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }
    }

    private String formatPriceChange(double percent) {
        if (percent > 0) {
            return ChatColor.GREEN + "▲ " + String.format("%.1f%%", percent) + " Price Increase";
        } else if (percent < 0) {
            return ChatColor.RED + "▼ " + String.format("%.1f%%", Math.abs(percent)) + " Price Decrease";
        } else {
            return ChatColor.GRAY + "● Price Stable";
        }
    }

    private String formatMaterialName(Material material) {
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


    public String getPlayerCategory(UUID uuid) {
        return playerCategory.get(uuid);
    }

    public Material getPlayerSelectedItem(UUID uuid) {
        return playerSelectedItem.get(uuid);
    }

    public void clearPlayerData(UUID uuid) {
        playerCategory.remove(uuid);
        playerSelectedItem.remove(uuid);
    }
}