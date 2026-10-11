package ru.mirea.velik.service;

import ru.mirea.velik.exception.BusinessException;
import ru.mirea.velik.exception.EntityNotFoundException;
import ru.mirea.velik.exception.ValidationException;
import ru.mirea.velik.model.Bike;
import ru.mirea.velik.repository.BikeRepository;
import ru.mirea.velik.repository.RentalRepository;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BikeService {

    // Длины соответствуют столбцам таблицы bikes
    private static final int MAX_MODEL_LENGTH = 100;
    private static final int MAX_TYPE_LENGTH = 50;
    private static final double MAX_PRICE_PER_HOUR = 100_000;

    private final BikeRepository bikeRepository;
    private final RentalRepository rentalRepository;

    public BikeService(BikeRepository bikeRepository, RentalRepository rentalRepository) {
        this.bikeRepository = bikeRepository;
        this.rentalRepository = rentalRepository;
    }

    public Bike create(String model, String type, boolean available, double pricePerHour) {
        validateBikeData(model, type, pricePerHour);
        Bike bike = new Bike(model.trim(), type.trim(), available, pricePerHour);
        return bikeRepository.save(bike);
    }

    public Bike getById(int id) {
        return bikeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Велосипед", id));
    }

    public List<Bike> getAll() {
        return bikeRepository.findAll();
    }

    public Bike update(int id, String model, String type, boolean available, double pricePerHour) {
        Bike existing = getById(id);
        validateBikeData(model, type, pricePerHour);
        // Бизнес-правило: нельзя вручную сделать велосипед «доступным», пока у него есть незавершённая аренда
        if (available && rentalRepository.hasActiveRentalForBike(id)) {
            throw new BusinessException("Велосипед с id=" + id + " нельзя пометить доступным: у него есть активные или созданные аренды");
        }
        existing.setModel(model.trim());
        existing.setType(type.trim());
        existing.setAvailable(available);
        existing.setPricePerHour(pricePerHour);
        return bikeRepository.update(existing);
    }

    public void delete(int id) {
        getById(id);
        // Бизнес-правило: нельзя удалить велосипед с активными арендами
        if (rentalRepository.hasActiveRentalForBike(id)) {
            throw new BusinessException("Нельзя удалить велосипед с id=" + id + ": у него есть активные или созданные аренды");
        }
        if (!bikeRepository.deleteById(id)) {
            throw new EntityNotFoundException("Велосипед", id);
        }
    }

    public List<Bike> filterByType(String type) {
        if (type == null || type.isBlank()) {
            throw new BusinessException("Тип велосипеда для фильтра не может быть пустым");
        }
        return bikeRepository.findByType(type.trim());
    }

    public List<Bike> filterAvailable() {
        return bikeRepository.findAvailable();
    }

    public List<Bike> searchByModel(String modelPart) {
        if (modelPart == null || modelPart.isBlank()) {
            throw new BusinessException("Строка поиска по модели не может быть пустой");
        }
        return bikeRepository.findByModelContaining(modelPart.trim());
    }

    public List<Bike> sortByPriceAsc() {
        return getAll().stream()
                .sorted(Comparator.comparingDouble(Bike::getPricePerHour))
                .collect(Collectors.toList());
    }

    public List<Bike> sortByPriceDesc() {
        return getAll().stream()
                .sorted(Comparator.comparingDouble(Bike::getPricePerHour).reversed())
                .collect(Collectors.toList());
    }

    public void setAvailable(int bikeId, boolean available) {
        Bike bike = getById(bikeId);
        bike.setAvailable(available);
        bikeRepository.update(bike);
    }

    private void validateBikeData(String model, String type, double pricePerHour) {
        // Бизнес-правило 3: модель обязательна
        if (model == null || model.isBlank()) {
            throw new ValidationException("model", "Модель велосипеда обязательна");
        }
        if (model.trim().length() > MAX_MODEL_LENGTH) {
            throw new ValidationException("model", "Модель не должна быть длиннее " + MAX_MODEL_LENGTH + " символов");
        }
        if (type == null || type.isBlank()) {
            throw new ValidationException("type", "Тип велосипеда обязателен");
        }
        if (type.trim().length() > MAX_TYPE_LENGTH) {
            throw new ValidationException("type", "Тип не должен быть длиннее " + MAX_TYPE_LENGTH + " символов");
        }
        // Бизнес-правило 4: цена должна быть положительной
        if (pricePerHour <= 0) {
            throw new ValidationException("price", "Цена за час должна быть больше 0");
        }
        if (pricePerHour > MAX_PRICE_PER_HOUR) {
            throw new ValidationException("price", "Цена за час не может превышать " + (long) MAX_PRICE_PER_HOUR + " ₽");
        }
    }

    /** Типы велосипедов, уже используемые в базе данных (для выпадающих списков). */
    public List<String> getTypes() {
        return getAll().stream()
                .map(Bike::getType)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }
}
