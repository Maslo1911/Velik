package ru.mirea.velik.repository;

import ru.mirea.velik.exception.DatabaseException;
import ru.mirea.velik.model.Bike;
import ru.mirea.velik.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcBikeRepository implements BikeRepository {

    @Override
    public Bike save(Bike entity) {
        String sql = "INSERT INTO bikes (model, type, available, price_per_hour) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entity.getModel());
            ps.setString(2, entity.getType());
            ps.setBoolean(3, entity.isAvailable());
            ps.setDouble(4, entity.getPricePerHour());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getInt(1));
                }
            }
            return entity;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при сохранении велосипеда: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Bike> findById(Integer id) {
        String sql = "SELECT id, model, type, available, price_per_hour FROM bikes WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при поиске велосипеда: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Bike> findAll() {
        String sql = "SELECT id, model, type, available, price_per_hour FROM bikes ORDER BY id";
        List<Bike> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при получении списка велосипедов: " + e.getMessage(), e);
        }
    }

    @Override
    public Bike update(Bike entity) {
        String sql = "UPDATE bikes SET model = ?, type = ?, available = ?, price_per_hour = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getModel());
            ps.setString(2, entity.getType());
            ps.setBoolean(3, entity.isAvailable());
            ps.setDouble(4, entity.getPricePerHour());
            ps.setInt(5, entity.getId());
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new DatabaseException("Велосипед с id=" + entity.getId() + " не найден для обновления");
            }
            return entity;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при обновлении велосипеда: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM bikes WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при удалении велосипеда: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Bike> findByType(String type) {
        String sql = "SELECT id, model, type, available, price_per_hour FROM bikes WHERE type = ? ORDER BY id";
        List<Bike> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска велосипедов по типу: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Bike> findAvailable() {
        String sql = "SELECT id, model, type, available, price_per_hour FROM bikes WHERE available = TRUE ORDER BY id";
        List<Bike> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска доступных велосипедов: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Bike> findByModelContaining(String modelPart) {
        String sql = "SELECT id, model, type, available, price_per_hour FROM bikes WHERE model LIKE ? ORDER BY id";
        List<Bike> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + modelPart + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска велосипедов по модели: " + e.getMessage(), e);
        }
    }

    private Bike mapRow(ResultSet rs) throws SQLException {
        return new Bike(
                rs.getInt("id"),
                rs.getString("model"),
                rs.getString("type"),
                rs.getBoolean("available"),
                rs.getDouble("price_per_hour")
        );
    }
}
