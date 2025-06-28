package com.Lino.dynamicShopGUI.database;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.models.ShopItem;
import org.bukkit.Material;
import java.sql.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
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

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(shopItemsTable);
            stmt.execute(transactionHistoryTable);
        }
    }

    private void initializeDefaultData() throws SQLException {
        String checkQuery = "SELECT COUNT(*) FROM shop_items";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(checkQuery)) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        Map<String, String> categories = new HashMap<>();
        categories.put("BUILDING", "STONE,COBBLESTONE,GRANITE,DIORITE,ANDESITE,DIRT,GRASS_BLOCK,OAK_LOG,SPRUCE_LOG,BIRCH_LOG,JUNGLE_LOG,ACACIA_LOG,DARK_OAK_LOG,OAK_PLANKS,SPRUCE_PLANKS,BIRCH_PLANKS,JUNGLE_PLANKS,ACACIA_PLANKS,DARK_OAK_PLANKS,SAND,RED_SAND,GRAVEL,GLASS,BRICKS,STONE_BRICKS,NETHER_BRICKS,QUARTZ_BLOCK,TERRACOTTA,CONCRETE,WOOL");
        categories.put("ORES", "COAL,IRON_INGOT,GOLD_INGOT,DIAMOND,EMERALD,LAPIS_LAZULI,REDSTONE,COPPER_INGOT,NETHERITE_INGOT,QUARTZ,AMETHYST_SHARD");
        categories.put("FOOD", "APPLE,BREAD,COOKED_BEEF,COOKED_PORKCHOP,COOKED_CHICKEN,COOKED_COD,COOKED_SALMON,GOLDEN_APPLE,GOLDEN_CARROT,COOKIE,MELON_SLICE,PUMPKIN_PIE,CAKE,HONEY_BOTTLE,SWEET_BERRIES");
        categories.put("TOOLS", "WOODEN_PICKAXE,STONE_PICKAXE,IRON_PICKAXE,GOLDEN_PICKAXE,DIAMOND_PICKAXE,NETHERITE_PICKAXE,WOODEN_AXE,STONE_AXE,IRON_AXE,GOLDEN_AXE,DIAMOND_AXE,NETHERITE_AXE,WOODEN_SHOVEL,STONE_SHOVEL,IRON_SHOVEL,GOLDEN_SHOVEL,DIAMOND_SHOVEL,NETHERITE_SHOVEL,WOODEN_HOE,STONE_HOE,IRON_HOE,GOLDEN_HOE,DIAMOND_HOE,NETHERITE_HOE");
        categories.put("ARMOR", "LEATHER_HELMET,LEATHER_CHESTPLATE,LEATHER_LEGGINGS,LEATHER_BOOTS,CHAINMAIL_HELMET,CHAINMAIL_CHESTPLATE,CHAINMAIL_LEGGINGS,CHAINMAIL_BOOTS,IRON_HELMET,IRON_CHESTPLATE,IRON_LEGGINGS,IRON_BOOTS,GOLDEN_HELMET,GOLDEN_CHESTPLATE,GOLDEN_LEGGINGS,GOLDEN_BOOTS,DIAMOND_HELMET,DIAMOND_CHESTPLATE,DIAMOND_LEGGINGS,DIAMOND_BOOTS,NETHERITE_HELMET,NETHERITE_CHESTPLATE,NETHERITE_LEGGINGS,NETHERITE_BOOTS");
        categories.put("REDSTONE", "REDSTONE,REDSTONE_TORCH,REDSTONE_BLOCK,REPEATER,COMPARATOR,PISTON,STICKY_PISTON,OBSERVER,HOPPER,DROPPER,DISPENSER,NOTE_BLOCK,LEVER,STONE_BUTTON,TRIPWIRE_HOOK,DAYLIGHT_DETECTOR");
        categories.put("FARMING", "WHEAT_SEEDS,PUMPKIN_SEEDS,MELON_SEEDS,BEETROOT_SEEDS,CARROT,POTATO,WHEAT,SUGAR_CANE,CACTUS,BAMBOO,KELP,BONE_MEAL,COCOA_BEANS");
        categories.put("MISC", "ENDER_PEARL,BLAZE_ROD,SLIME_BALL,GUNPOWDER,STRING,SPIDER_EYE,GHAST_TEAR,MAGMA_CREAM,PHANTOM_MEMBRANE,SHULKER_SHELL,NAUTILUS_SHELL,HEART_OF_THE_SEA,TOTEM_OF_UNDYING,ENCHANTED_BOOK,NAME_TAG,SADDLE,LEAD,ELYTRA");

        String insertQuery = "INSERT INTO shop_items (material, category, base_price, current_price, stock, max_stock) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(insertQuery)) {
            for (Map.Entry<String, String> entry : categories.entrySet()) {
                String category = entry.getKey();
                String[] items = entry.getValue().split(",");

                for (String item : items) {
                    try {
                        Material material = Material.valueOf(item);
                        double basePrice = calculateBasePrice(material, category);
                        int maxStock = calculateMaxStock(material, category);
                        int initialStock = maxStock / 2;

                        pstmt.setString(1, item);
                        pstmt.setString(2, category);
                        pstmt.setDouble(3, basePrice);
                        pstmt.setDouble(4, basePrice);
                        pstmt.setInt(5, initialStock);
                        pstmt.setInt(6, maxStock);
                        pstmt.addBatch();
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            pstmt.executeBatch();
        }
    }

    private double calculateBasePrice(Material material, String category) {
        switch (category) {
            case "ORES":
                if (material.name().contains("NETHERITE")) return 5000.0;
                if (material.name().contains("DIAMOND")) return 500.0;
                if (material.name().contains("EMERALD")) return 300.0;
                if (material.name().contains("GOLD")) return 150.0;
                if (material.name().contains("IRON")) return 50.0;
                return 20.0;
            case "TOOLS":
                if (material.name().contains("NETHERITE")) return 10000.0;
                if (material.name().contains("DIAMOND")) return 1500.0;
                if (material.name().contains("GOLDEN")) return 500.0;
                if (material.name().contains("IRON")) return 200.0;
                if (material.name().contains("STONE")) return 50.0;
                return 20.0;
            case "ARMOR":
                if (material.name().contains("NETHERITE")) return 12000.0;
                if (material.name().contains("DIAMOND")) return 2000.0;
                if (material.name().contains("GOLDEN")) return 800.0;
                if (material.name().contains("IRON")) return 400.0;
                if (material.name().contains("CHAINMAIL")) return 300.0;
                return 100.0;
            case "FOOD":
                if (material == Material.GOLDEN_APPLE) return 1000.0;
                if (material == Material.GOLDEN_CARROT) return 500.0;
                if (material == Material.CAKE) return 100.0;
                return 10.0;
            case "MISC":
                if (material == Material.ELYTRA) return 50000.0;
                if (material == Material.TOTEM_OF_UNDYING) return 10000.0;
                if (material == Material.SHULKER_SHELL) return 1000.0;
                if (material == Material.HEART_OF_THE_SEA) return 5000.0;
                return 100.0;
            default:
                return 10.0;
        }
    }

    private int calculateMaxStock(Material material, String category) {
        switch (category) {
            case "BUILDING":
                return 10000;
            case "ORES":
                if (material.name().contains("NETHERITE")) return 10;
                if (material.name().contains("DIAMOND")) return 100;
                if (material.name().contains("EMERALD")) return 200;
                return 1000;
            case "TOOLS":
            case "ARMOR":
                if (material.name().contains("NETHERITE")) return 5;
                if (material.name().contains("DIAMOND")) return 20;
                return 50;
            case "MISC":
                if (material == Material.ELYTRA) return 3;
                if (material == Material.TOTEM_OF_UNDYING) return 5;
                return 100;
            default:
                return 500;
        }
    }

    public CompletableFuture<ShopItem> getShopItem(Material material) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT * FROM shop_items WHERE material = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, material.name());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return new ShopItem(
                                Material.valueOf(rs.getString("material")),
                                rs.getString("category"),
                                rs.getDouble("base_price"),
                                rs.getDouble("current_price"),
                                rs.getInt("stock"),
                                rs.getInt("max_stock"),
                                rs.getInt("transactions_buy"),
                                rs.getInt("transactions_sell"),
                                rs.getDouble("price_change_percent")
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
                        ShopItem item = new ShopItem(
                                material,
                                rs.getString("category"),
                                rs.getDouble("base_price"),
                                rs.getDouble("current_price"),
                                rs.getInt("stock"),
                                rs.getInt("max_stock"),
                                rs.getInt("transactions_buy"),
                                rs.getInt("transactions_sell"),
                                rs.getDouble("price_change_percent")
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