package ru.mirea.velik.repository;

import ru.mirea.velik.exception.DatabaseException;
import ru.mirea.velik.model.Rental;
import ru.mirea.velik.model.RentalStatus;
import ru.mirea.velik.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcRentalRepository implements RentalRepository {

    @Override
    public Rental save(Rental entity) {
        String sql = "INSERT INTO rentals (client_id, bike_id, start_time, end_time, status, total_cost) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setRentalParams(ps, entity);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getInt(1));
                }
            }
            return entity;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при сохранении аренды: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Rental> findById(Integer id) {
        String sql = "SELECT id, client_id, bike_id, start_time, end_time, status, total_cost FROM rentals WHERE id = ?";
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
            throw new DatabaseException("Ошибка при поиске аренды: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Rental> findAll() {
        String sql = "SELECT id, client_id, bike_id, start_time, end_time, status, total_cost FROM rentals ORDER BY id";
        return queryList(sql);
    }

    @Override
    public Rental update(Rental entity) {
        String sql = "UPDATE rentals SET client_id = ?, bike_id = ?, start_time = ?, end_time = ?, status = ?, total_cost = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setRentalParams(ps, entity);
            ps.setInt(7, entity.getId());
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new DatabaseException("Аренда с id=" + entity.getId() + " не найдена для обновления");
            }
            return entity;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при обновлении аренды: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM rentals WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при удалении аренды: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Rental> findByClientId(int clientId) {
        String sql = "SELECT id, client_id, bike_id, start_time, end_time, status, total_cost FROM rentals WHERE client_id = ? ORDER BY id";
        return queryList(sql, ps -> ps.setInt(1, clientId));
    }

    @Override
    public List<Rental> findByBikeId(int bikeId) {
        String sql = "SELECT id, client_id, bike_id, start_time, end_time, status, total_cost FROM rentals WHERE bike_id = ? ORDER BY id";
        return queryList(sql, ps -> ps.setInt(1, bikeId));
    }

    @Override
    public List<Rental> findByStatus(RentalStatus status) {
        String sql = "SELECT id, client_id, bike_id, start_time, end_time, status, total_cost FROM rentals WHERE status = ? ORDER BY id";
        return queryList(sql, ps -> ps.setString(1, status.name()));
    }

    @Override
    public List<Rental> findByDateRange(LocalDateTime from, LocalDateTime to) {
        String sql = "SELECT id, client_id, bike_id, start_time, end_time, status, total_cost FROM rentals " +
                "WHERE start_time >= ? AND start_time <= ? ORDER BY start_time";
        return queryList(sql, ps -> {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
        });
    }

    @Override
    public boolean hasActiveRentalForBike(int bikeId) {
        String sql = "SELECT COUNT(*) FROM rentals WHERE bike_id = ? AND status IN ('CREATED', 'ACTIVE')";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bikeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка проверки активной аренды велосипеда: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasActiveRentalForClient(int clientId) {
        String sql = "SELECT COUNT(*) FROM rentals WHERE client_id = ? AND status IN ('CREATED', 'ACTIVE')";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка проверки активной аренды клиента: " + e.getMessage(), e);
        }
    }

    private void setRentalParams(PreparedStatement ps, Rental entity) throws SQLException {
        ps.setInt(1, entity.getClientId());
        ps.setInt(2, entity.getBikeId());
        ps.setTimestamp(3, entity.getStartTime() != null ? Timestamp.valueOf(entity.getStartTime()) : null);
        if (entity.getEndTime() != null) {
            ps.setTimestamp(4, Timestamp.valueOf(entity.getEndTime()));
        } else {
            ps.setNull(4, Types.TIMESTAMP);
        }
        ps.setString(5, entity.getStatus().name());
        ps.setDouble(6, entity.getTotalCost());
    }

    private Rental mapRow(ResultSet rs) throws SQLException {
        Timestamp startTs = rs.getTimestamp("start_time");
        Timestamp endTs = rs.getTimestamp("end_time");
        return new Rental(
                rs.getInt("id"),
                rs.getInt("client_id"),
                rs.getInt("bike_id"),
                startTs != null ? startTs.toLocalDateTime() : null,
                endTs != null ? endTs.toLocalDateTime() : null,
                RentalStatus.valueOf(rs.getString("status")),
                rs.getDouble("total_cost")
        );
    }

    private List<Rental> queryList(String sql) {
        return queryList(sql, ps -> {});
    }

    private List<Rental> queryList(String sql, SqlConsumer consumer) {
        List<Rental> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            consumer.accept(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка выполнения запроса аренд: " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    private interface SqlConsumer {
        void accept(PreparedStatement ps) throws SQLException;
    }
}
