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

    private void initializeDefaultData() throws SQLException {
        Map<String, CategoryConfigLoader.CategoryConfig> categories = plugin.getShopConfig().getAllCategories();

        String insertQuery = "INSERT OR IGNORE INTO shop_items (material, category, base_price, current_price, stock, max_stock) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(insertQuery)) {
            for (Map.Entry<String, CategoryConfigLoader.CategoryConfig> categoryEntry : categories.entrySet()) {
                String categoryName = categoryEntry.getKey();
                CategoryConfigLoader.CategoryConfig category = categoryEntry.getValue();

                for (Map.Entry<Material, CategoryConfigLoader.ItemConfig> itemEntry : category.getItems().entrySet()) {
                    Material material = itemEntry.getKey();
                    CategoryConfigLoader.ItemConfig itemConfig = itemEntry.getValue();

                    pstmt.setString(1, material.name());
                    pstmt.setString(2, categoryName);
                    pstmt.setDouble(3, itemConfig.getPrice());
                    pstmt.setDouble(4, itemConfig.getPrice());
                    pstmt.setInt(5, itemConfig.getInitialStock());
                    pstmt.setInt(6, itemConfig.getMaxStock());
                    pstmt.addBatch();
                }
            }
            pstmt.executeBatch();
        }
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

                        return new ShopItem(
                                Material.valueOf(rs.getString("material")),
                                rs.getString("category"),
                                basePrice,
                                currentPrice,
                                rs.getInt("stock"),
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

                        ShopItem item = new ShopItem(
                                material,
                                rs.getString("category"),
                                basePrice,
                                currentPrice,
                                rs.getInt("stock"),
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
