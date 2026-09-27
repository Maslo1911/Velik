package ru.mirea.velik.repository;

import ru.mirea.velik.model.Bike;

import java.util.List;

public interface BikeRepository extends Repository<Bike, Integer> {

    List<Bike> findByType(String type);

    List<Bike> findAvailable();

    List<Bike> findByModelContaining(String modelPart);
}
