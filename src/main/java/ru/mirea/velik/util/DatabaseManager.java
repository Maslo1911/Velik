package ru.mirea.velik.util;

import ru.mirea.velik.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Управление подключением к PostgreSQL.
 * Параметры можно переопределить через системные свойства или переменные окружения.
 */
public final class DatabaseManager {

    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/velik";
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASSWORD = "postgres";

    private DatabaseManager() {
    }

    public static Connection getConnection() {
        String url = System.getProperty("db.url", System.getenv().getOrDefault("DB_URL", DEFAULT_URL));
        String user = System.getProperty("db.user", System.getenv().getOrDefault("DB_USER", DEFAULT_USER));
        String password = System.getProperty("db.password", System.getenv().getOrDefault("DB_PASSWORD", DEFAULT_PASSWORD));

        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Не удалось подключиться к базе данных. Проверьте, что PostgreSQL запущен и параметры подключения верны.\n" +
                            "URL: " + url + "\nПричина: " + e.getMessage(), e);
        }
    }
}
