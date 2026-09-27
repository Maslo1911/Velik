package ru.mirea.velik.repository;

import ru.mirea.velik.exception.DatabaseException;
import ru.mirea.velik.model.Client;
import ru.mirea.velik.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcClientRepository implements ClientRepository {

    @Override
    public Client save(Client entity) {
        String sql = "INSERT INTO clients (full_name, phone, email) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entity.getFullName());
            ps.setString(2, entity.getPhone());
            ps.setString(3, entity.getEmail());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getInt(1));
                }
            }
            return entity;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при сохранении клиента: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Client> findById(Integer id) {
        String sql = "SELECT id, full_name, phone, email FROM clients WHERE id = ?";
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
            throw new DatabaseException("Ошибка при поиске клиента: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Client> findAll() {
        String sql = "SELECT id, full_name, phone, email FROM clients ORDER BY id";
        List<Client> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при получении списка клиентов: " + e.getMessage(), e);
        }
    }

    @Override
    public Client update(Client entity) {
        String sql = "UPDATE clients SET full_name = ?, phone = ?, email = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getFullName());
            ps.setString(2, entity.getPhone());
            ps.setString(3, entity.getEmail());
            ps.setInt(4, entity.getId());
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new DatabaseException("Клиент с id=" + entity.getId() + " не найден для обновления");
            }
            return entity;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при обновлении клиента: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM clients WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при удалении клиента: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Client> findByNameContaining(String namePart) {
        String sql = "SELECT id, full_name, phone, email FROM clients WHERE full_name LIKE ? ORDER BY id";
        List<Client> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + namePart + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска клиентов по имени: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Client> findByPhone(String phone) {
        String sql = "SELECT id, full_name, phone, email FROM clients WHERE phone = ?";
        List<Client> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска клиентов по телефону: " + e.getMessage(), e);
        }
    }

    private Client mapRow(ResultSet rs) throws SQLException {
        return new Client(
                rs.getInt("id"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("email")
        );
    }
}
