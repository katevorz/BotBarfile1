package org.Baraxolkabot.Base;
import org.Baraxolkabot.BotBaraxolka.Product;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import org.Baraxolkabot.Category.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private static final String URL = "jdbc:mysql://localhost:3306/Baraxolochkabot_";
    private static final String USER = "root";
    private static final String PASSWORD = "root";
    private static Connection connection = null;

    public Database() throws SQLException {}

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        return connection;
    }

    public static void initializeDatabase() {
        String tableSQL = "CREATE TABLE IF NOT EXISTS Products ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "name VARCHAR(100) NOT NULL, "
                + "price DECIMAL(10, 2), "
                + "description TEXT, "
                + "telegramHandle VARCHAR(50), "
                + "photoId VARCHAR(100), "
                + "category VARCHAR(50)"
                + ");";

        try (Statement statement = getConnection().createStatement()) {
            statement.execute(tableSQL);
            System.out.println("Database and tables initialized successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static boolean deleteProductByName(String productName) {
        String sql = "DELETE FROM products WHERE name = ?";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, productName);
            int rowsAffected = statement.executeUpdate();

            return rowsAffected > 0; // Возвращаем true, если товар был удален
        } catch (SQLException e) {
            e.printStackTrace();
            return false; // Возвращаем false в случае ошибки
        }
    }
    public static Product getProductByName(String productName) {
        Product product = null;
        String query = "SELECT * FROM products WHERE name = ?";

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, productName);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                // Создаем объект Product из данных из базы
                product = new Product(
                        resultSet.getString("name"),
                        new Category(resultSet.getString("category")),
                        resultSet.getBigDecimal("price"),
                        resultSet.getString("description"),
                        resultSet.getString("telegramHandle"),
                        resultSet.getString("photoId")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return product;
    }

    public static void insertProduct(String name, String category, double price, String description, String telegramHandle, String photoId) {
        String sql = "INSERT INTO Products (name, category, price, description, telegramHandle, photoId) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, category);
            pstmt.setDouble(3, price);
            pstmt.setString(4, description);
            pstmt.setString(5, telegramHandle);
            pstmt.setString(6, photoId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<Product> getProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM Products";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(new Product(
                        rs.getString("name"),
                        new Category(rs.getString("category")),
                        rs.getBigDecimal("price"),
                        rs.getString("description"),
                        rs.getString("telegramHandle"),
                        rs.getString("photoId")

                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }
    public static List<Product> searchProducts(String keyword) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM Products WHERE name LIKE ? OR category LIKE ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setString(2, "%" + keyword + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(new Product(
                            rs.getString("name"),
                            new Category(rs.getString("category")),
                            rs.getBigDecimal("price"),
                            rs.getString("description"),
                            rs.getString("telegramHandle"),
                            rs.getString("photoId")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

}