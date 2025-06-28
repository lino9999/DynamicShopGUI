package com.Lino.dynamicShopGUI.models;

import org.bukkit.Material;

public class ShopItem {

    private final Material material;
    private final String category;
    private final double basePrice;
    private double currentPrice;
    private int stock;
    private final int maxStock;
    private int transactionsBuy;
    private int transactionsSell;
    private double priceChangePercent;

    public ShopItem(Material material, String category, double basePrice, double currentPrice,
                    int stock, int maxStock, int transactionsBuy, int transactionsSell,
                    double priceChangePercent) {
        this.material = material;
        this.category = category;
        this.basePrice = basePrice;
        this.currentPrice = currentPrice;
        this.stock = stock;
        this.maxStock = maxStock;
        this.transactionsBuy = transactionsBuy;
        this.transactionsSell = transactionsSell;
        this.priceChangePercent = priceChangePercent;
    }

    public Material getMaterial() {
        return material;
    }

    public String getCategory() {
        return category;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = Math.max(0, Math.min(stock, maxStock));
    }

    public int getMaxStock() {
        return maxStock;
    }

    public int getTransactionsBuy() {
        return transactionsBuy;
    }

    public void incrementTransactionsBuy() {
        this.transactionsBuy++;
    }

    public int getTransactionsSell() {
        return transactionsSell;
    }

    public void incrementTransactionsSell() {
        this.transactionsSell++;
    }

    public double getPriceChangePercent() {
        return priceChangePercent;
    }

    public void setPriceChangePercent(double priceChangePercent) {
        this.priceChangePercent = priceChangePercent;
    }

    public double getBuyPrice() {
        return currentPrice;
    }

    public double getSellPrice() {
        return currentPrice * 0.7;
    }
}