package ru.mirea.velik.util;

import ru.mirea.velik.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Управление подключением к PostgreSQL.
 * Параметры читаются из database.properties; их можно переопределить
 * через системные свойства (db.url, db.user, db.password) или переменные окружения
 * (DB_URL, DB_USER, DB_PASSWORD).
 */
public final class DatabaseManager {

    private static final String PROPERTIES_FILE = "/database.properties";
    private static final Properties FILE_PROPERTIES = loadFileProperties();

    private DatabaseManager() {
    }

    public static Connection getConnection() {
        String url = getUrl();
        try {
            return DriverManager.getConnection(url, setting("db.user", "DB_USER"), setting("db.password", "DB_PASSWORD"));
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Не удалось подключиться к базе данных. Проверьте, что PostgreSQL запущен и параметры подключения верны.\n" +
                            "URL: " + url + "\nПричина: " + e.getMessage(), e);
        }
    }

    /** JDBC-адрес текущей базы данных (для отображения в интерфейсе). */
    public static String getUrl() {
        return setting("db.url", "DB_URL");
    }

    /**
     * Проверка доступности БД: открывает соединение и сразу закрывает его.
     * При недоступной БД выбрасывает {@link DatabaseException}.
     */
    public static void checkConnection() {
        try (Connection ignored = getConnection()) {
            // соединение успешно открыто, закроется автоматически
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка закрытия соединения: " + e.getMessage(), e);
        }
    }

    /** Имена пользовательских таблиц текущей базы данных (через метаданные JDBC). */
    public static List<String> getTableNames() {
        List<String> names = new ArrayList<>();
        try (Connection conn = getConnection();
             ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                names.add(rs.getString("TABLE_NAME"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка чтения списка таблиц: " + e.getMessage(), e);
        }
        return names;
    }

    /** Приоритет: системное свойство, переменная окружения, database.properties. */
    private static String setting(String key, String envName) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(envName);
        }
        if (value == null || value.isBlank()) {
            value = FILE_PROPERTIES.getProperty(key);
        }
        if (value == null || value.isBlank()) {
            throw new DatabaseException("Не задан параметр подключения «" + key + "». Укажите его в файле database.properties");
        }
        return value.trim();
    }

    private static Properties loadFileProperties() {
        Properties properties = new Properties();
        try (InputStream in = DatabaseManager.class.getResourceAsStream(PROPERTIES_FILE)) {
            if (in != null) {
                properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new DatabaseException("Не удалось прочитать файл database.properties: " + e.getMessage(), e);
        }
        return properties;
    }
}
