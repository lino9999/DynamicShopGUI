package com.Lino.dynamicShopGUI.database;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.config.CategoryConfigLoader;
import com.Lino.dynamicShopGUI.models.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import java.sql.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final DynamicShopGUI plugin;
    private Connection connection;
    private final String DATABASE_FILE = "dynamicshop.db";

    public DatabaseManager(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            String url = "jdbc:sqlite:" + new File(dataFolder, DATABASE_FILE).getAbsolutePath();
            connection = DriverManager.getConnection(url);

            createTables();
            updateSchema();
            initializeDefaultData();

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void createTables() throws SQLException {
        String shopItemsTable = "CREATE TABLE IF NOT EXISTS shop_items (" +
                "material VARCHAR(100) PRIMARY KEY," +
                "category VARCHAR(50) NOT NULL," +
                "base_price DOUBLE NOT NULL," +
                "current_price DOUBLE NOT NULL," +
                "stock INTEGER NOT NULL," +
                "min_stock INTEGER DEFAULT 0," +
                "max_stock INTEGER NOT NULL," +
                "transactions_buy INTEGER DEFAULT 0," +
                "transactions_sell INTEGER DEFAULT 0," +
                "price_change_percent DOUBLE DEFAULT 0.0" +
                ")";

        String transactionHistoryTable = "CREATE TABLE IF NOT EXISTS transaction_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "material VARCHAR(100) NOT NULL," +
                "transaction_type VARCHAR(10) NOT NULL," +
                "amount INTEGER NOT NULL," +
                "price_per_unit DOUBLE NOT NULL," +
                "total_price DOUBLE NOT NULL," +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        String autoSellChestsTable = "CREATE TABLE IF NOT EXISTS autosell_chests (" +
                "world VARCHAR(100) NOT NULL," +
                "x INTEGER NOT NULL," +
                "y INTEGER NOT NULL," +
                "z INTEGER NOT NULL," +
                "owner_uuid VARCHAR(36) NOT NULL," +
                "PRIMARY KEY (world, x, y, z)" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(shopItemsTable);
            stmt.execute(transactionHistoryTable);
            stmt.execute(autoSellChestsTable);
        }
    }

    private void updateSchema() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE shop_items ADD COLUMN min_stock INTEGER DEFAULT 0");
        } catch (SQLException ignored) {
        }
    }

    private void initializeDefaultData() throws SQLException {
        Map<String, CategoryConfigLoader.CategoryConfig> categories = plugin.getShopConfig().getAllCategories();

        String insertQuery = "INSERT OR IGNORE INTO shop_items (material, category, base_price, current_price, stock, min_stock, max_stock) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String updateQuery = "UPDATE shop_items SET base_price = ?, max_stock = ?, min_stock = ?, category = ? WHERE material = ?";

        try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery);
             PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {

            for (Map.Entry<String, CategoryConfigLoader.CategoryConfig> categoryEntry : categories.entrySet()) {
                String categoryName = categoryEntry.getKey();
                CategoryConfigLoader.CategoryConfig category = categoryEntry.getValue();

                for (Map.Entry<Material, CategoryConfigLoader.ItemConfig> itemEntry : category.getItems().entrySet()) {
                    Material material = itemEntry.getKey();
                    CategoryConfigLoader.ItemConfig itemConfig = itemEntry.getValue();

                    // Calcola il prezzo iniziale basato sullo stock iniziale per evitare sbalzi
                    double initialPrice = calculateInitialPrice(itemConfig.getPrice(), itemConfig.getInitialStock(), itemConfig.getMaxStock());

                    // Insert batch
                    insertStmt.setString(1, material.name());
                    insertStmt.setString(2, categoryName);
                    insertStmt.setDouble(3, itemConfig.getPrice());
                    insertStmt.setDouble(4, initialPrice); // Usa il prezzo calcolato, non solo quello base
                    insertStmt.setInt(5, itemConfig.getInitialStock());
                    insertStmt.setInt(6, itemConfig.getMinStock());
                    insertStmt.setInt(7, itemConfig.getMaxStock());
                    insertStmt.addBatch();

                    // Update batch
                    updateStmt.setDouble(1, itemConfig.getPrice());
                    updateStmt.setInt(2, itemConfig.getMaxStock());
                    updateStmt.setInt(3, itemConfig.getMinStock());
                    updateStmt.setString(4, categoryName);
                    updateStmt.setString(5, material.name());
                    updateStmt.addBatch();
                }
            }
            insertStmt.executeBatch();
            updateStmt.executeBatch();
        }
    }

    // Helper per calcolare il prezzo all'inizializzazione del DB
    private double calculateInitialPrice(double basePrice, int stock, int maxStock) {
        double stockRatio = (double) stock / maxStock;
        double basePriceFactor;

        if (stockRatio < 0.01) {
            basePriceFactor = 2.5;
        } else if (stockRatio < 0.05) {
            double t = (stockRatio - 0.01) / 0.04;
            basePriceFactor = 2.5 - (t * 0.3);
        } else if (stockRatio < 0.1) {
            double t = (stockRatio - 0.05) / 0.05;
            basePriceFactor = 2.2 - (t * 0.2);
        } else if (stockRatio < 0.3) {
            double t = (stockRatio - 0.1) / 0.2;
            basePriceFactor = 2.0 - (t * 0.5);
        } else if (stockRatio < 0.5) {
            double t = (stockRatio - 0.3) / 0.2;
            basePriceFactor = 1.5 - (t * 0.3);
        } else if (stockRatio < 0.7) {
            double t = (stockRatio - 0.5) / 0.2;
            basePriceFactor = 1.2 - (t * 0.1);
        } else if (stockRatio < 0.9) {
            double t = (stockRatio - 0.7) / 0.2;
            basePriceFactor = 1.1 - (t * 0.05);
        } else if (stockRatio < 0.95) {
            double t = (stockRatio - 0.9) / 0.05;
            basePriceFactor = 1.05 - (t * 0.03);
        } else if (stockRatio < 0.99) {
            double t = (stockRatio - 0.95) / 0.04;
            basePriceFactor = 1.02 - (t * 0.015);
        } else {
            double t = (stockRatio - 0.99) / 0.01;
            basePriceFactor = 1.005 - (t * 0.005);
        }

        double priceFactor = basePriceFactor;

        if (maxStock > 1000) {
            double scaleFactor = Math.log10(maxStock / 1000.0) + 1.0;
            double adjustment = (basePriceFactor - 1.0) / scaleFactor;
            priceFactor = 1.0 + adjustment;
        }

        double newPrice = basePrice * priceFactor;

        // Limiti hardcoded per l'init sicuri (10% min, 1000% max)
        double minPrice = basePrice * 0.1;
        double maxPrice = basePrice * 10.0;

        return Math.max(minPrice, Math.min(maxPrice, newPrice));
    }

    public CompletableFuture<Void> saveAutoSellChest(Location location, UUID owner) {
        return CompletableFuture.runAsync(() -> {
            String query = "INSERT OR REPLACE INTO autosell_chests (world, x, y, z, owner_uuid) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, location.getWorld().getName());
                pstmt.setInt(2, location.getBlockX());
                pstmt.setInt(3, location.getBlockY());
                pstmt.setInt(4, location.getBlockZ());
                pstmt.setString(5, owner.toString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Void> removeAutoSellChest(Location location) {
        return CompletableFuture.runAsync(() -> {
            String query = "DELETE FROM autosell_chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, location.getWorld().getName());
                pstmt.setInt(2, location.getBlockX());
                pstmt.setInt(3, location.getBlockY());
                pstmt.setInt(4, location.getBlockZ());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Map<Location, UUID>> loadAutoSellChests() {
        return CompletableFuture.supplyAsync(() -> {
            Map<Location, UUID> chests = new HashMap<>();
            String query = "SELECT * FROM autosell_chests";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    String worldName = rs.getString("world");
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    UUID owner = UUID.fromString(rs.getString("owner_uuid"));
                    Location loc = new Location(Bukkit.getWorld(worldName), x, y, z);
                    chests.put(loc, owner);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return chests;
        });
    }

    public CompletableFuture<ShopItem> getShopItem(Material material) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT * FROM shop_items WHERE material = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, material.name());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        double basePrice = rs.getDouble("base_price");
                        double currentPrice = rs.getDouble("current_price");
                        double priceChangePercent = ((currentPrice - basePrice) / basePrice) * 100;

                        int minStock = 0;
                        try { minStock = rs.getInt("min_stock"); } catch (SQLException ignored) {}

                        return new ShopItem(
                                Material.valueOf(rs.getString("material")),
                                rs.getString("category"),
                                basePrice,
                                currentPrice,
                                rs.getInt("stock"),
                                minStock,
                                rs.getInt("max_stock"),
                                rs.getInt("transactions_buy"),
                                rs.getInt("transactions_sell"),
                                priceChangePercent
                        );
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    public CompletableFuture<Map<Material, ShopItem>> getItemsByCategory(String category) {
        return CompletableFuture.supplyAsync(() -> {
            Map<Material, ShopItem> items = new HashMap<>();
            String query = "SELECT * FROM shop_items WHERE category = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, category);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Material material = Material.valueOf(rs.getString("material"));
                        double basePrice = rs.getDouble("base_price");
                        double currentPrice = rs.getDouble("current_price");
                        double priceChangePercent = ((currentPrice - basePrice) / basePrice) * 100;

                        int minStock = 0;
                        try { minStock = rs.getInt("min_stock"); } catch (SQLException ignored) {}

                        ShopItem item = new ShopItem(
                                material,
                                rs.getString("category"),
                                basePrice,
                                currentPrice,
                                rs.getInt("stock"),
                                minStock,
                                rs.getInt("max_stock"),
                                rs.getInt("transactions_buy"),
                                rs.getInt("transactions_sell"),
                                priceChangePercent
                        );
                        items.put(material, item);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return items;
        });
    }

    public CompletableFuture<Void> updateShopItem(ShopItem item) {
        return CompletableFuture.runAsync(() -> {
            String query = "UPDATE shop_items SET current_price = ?, stock = ?, transactions_buy = ?, " +
                    "transactions_sell = ?, price_change_percent = ? WHERE material = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setDouble(1, item.getCurrentPrice());
                pstmt.setInt(2, item.getStock());
                pstmt.setInt(3, item.getTransactionsBuy());
                pstmt.setInt(4, item.getTransactionsSell());
                pstmt.setDouble(5, item.getPriceChangePercent());
                pstmt.setString(6, item.getMaterial().name());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Void> logTransaction(String playerUuid, Material material, String type,
                                                  int amount, double pricePerUnit, double totalPrice) {
        return CompletableFuture.runAsync(() -> {
            String query = "INSERT INTO transaction_history (player_uuid, material, transaction_type, " +
                    "amount, price_per_unit, total_price) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, playerUuid);
                pstmt.setString(2, material.name());
                pstmt.setString(3, type);
                pstmt.setInt(4, amount);
                pstmt.setDouble(5, pricePerUnit);
                pstmt.setDouble(6, totalPrice);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}