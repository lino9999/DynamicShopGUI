package com.Lino.dynamicShopGUI.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class MenuBuilder {
    private final Inventory inventory;

    private MenuBuilder(String title, int rows) {
        this.inventory = Bukkit.createInventory(null, rows * 9, title);
    }

    public static MenuBuilder create(String title, int rows) {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("Menu rows must be between 1 and 6");
        }
        return new MenuBuilder(title, rows);
    }

    public MenuBuilder fill(Material material, String displayName) {
        ItemStack filler = item(material, displayName, null);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
        return this;
    }

    public MenuBuilder set(int slot, Material material, String displayName) {
        return set(slot, item(material, displayName, null));
    }

    public MenuBuilder set(int slot, ItemStack item) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
        }
        return this;
    }

    public MenuBuilder clear(int slot) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, null);
        }
        return this;
    }

    public Inventory build() {
        return inventory;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public static ItemStack item(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
