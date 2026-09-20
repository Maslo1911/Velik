package ru.mirea.velik.repository;

import ru.mirea.velik.model.Rental;

import java.util.List;

public interface RentalRepository extends Repository<Rental, Integer> {

    // Все аренды конкретного клиента
    List<Rental> findByClientId(int clientId);

    // Есть ли у велосипеда сейчас активная (незавершённая) аренда
    boolean hasActiveRentalForBike(int bikeId);

    // Есть ли у клиента незавершённые аренды (нужно для запрета удаления клиента)
    boolean hasActiveRentalForClient(int clientId);
}
