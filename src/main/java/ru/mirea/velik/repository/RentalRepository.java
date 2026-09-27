package ru.mirea.velik.repository;

import ru.mirea.velik.model.Rental;
import ru.mirea.velik.model.RentalStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface RentalRepository extends Repository<Rental, Integer> {

    List<Rental> findByClientId(int clientId);

    List<Rental> findByBikeId(int bikeId);

    List<Rental> findByStatus(RentalStatus status);

    List<Rental> findByDateRange(LocalDateTime from, LocalDateTime to);

    boolean hasActiveRentalForBike(int bikeId);

    boolean hasActiveRentalForClient(int clientId);
}
