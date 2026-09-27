package ru.mirea.velik.service;

import ru.mirea.velik.exception.BusinessException;
import ru.mirea.velik.exception.EntityNotFoundException;
import ru.mirea.velik.model.Bike;
import ru.mirea.velik.model.Rental;
import ru.mirea.velik.model.RentalStatus;
import ru.mirea.velik.repository.BikeRepository;
import ru.mirea.velik.repository.ClientRepository;
import ru.mirea.velik.repository.RentalRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RentalService {

    private final RentalRepository rentalRepository;
    private final ClientRepository clientRepository;
    private final BikeRepository bikeRepository;
    private final BikeService bikeService;

    public RentalService(RentalRepository rentalRepository,
                         ClientRepository clientRepository,
                         BikeRepository bikeRepository,
                         BikeService bikeService) {
        this.rentalRepository = rentalRepository;
        this.clientRepository = clientRepository;
        this.bikeRepository = bikeRepository;
        this.bikeService = bikeService;
    }

    /**
     * Создание заявки на аренду (статус CREATED).
     */
    public Rental create(int clientId, int bikeId) {
        // Бизнес-правило: клиент должен существовать
        clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Клиент", clientId));

        Bike bike = bikeRepository.findById(bikeId)
                .orElseThrow(() -> new EntityNotFoundException("Велосипед", bikeId));

        // Бизнес-правило 5: нельзя арендовать недоступный велосипед
        if (!bike.isAvailable()) {
            throw new BusinessException("Велосипед с id=" + bikeId + " сейчас недоступен для аренды");
        }

        // Бизнес-правило: у велосипеда не должно быть активной аренды
        if (rentalRepository.hasActiveRentalForBike(bikeId)) {
            throw new BusinessException("Велосипед с id=" + bikeId + " уже находится в активной аренде");
        }

        Rental rental = new Rental(clientId, bikeId, LocalDateTime.now(), RentalStatus.CREATED);
        rental.setTotalCost(0);
        Rental saved = rentalRepository.save(rental);

        // Помечаем велосипед как недоступный
        bikeService.setAvailable(bikeId, false);

        return saved;
    }

    public Rental getById(int id) {
        return rentalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Аренда", id));
    }

    public List<Rental> getAll() {
        return rentalRepository.findAll();
    }

    /**
     * Изменение статуса с проверкой допустимых переходов.
     */
    public Rental changeStatus(int id, RentalStatus newStatus) {
        Rental rental = getById(id);
        RentalStatus current = rental.getStatus();

        // Бизнес-правило 6: допустимые переходы статусов
        validateStatusTransition(current, newStatus);

        rental.setStatus(newStatus);

        if (newStatus == RentalStatus.ACTIVE && rental.getStartTime() == null) {
            rental.setStartTime(LocalDateTime.now());
        }

        if (newStatus == RentalStatus.COMPLETED) {
            completeRental(rental);
        }

        if (newStatus == RentalStatus.CANCELLED) {
            // Возвращаем велосипед в доступные
            bikeService.setAvailable(rental.getBikeId(), true);
            rental.setEndTime(LocalDateTime.now());
        }

        return rentalRepository.update(rental);
    }

    public Rental update(int id, int clientId, int bikeId) {
        Rental rental = getById(id);
        if (rental.getStatus() == RentalStatus.COMPLETED || rental.getStatus() == RentalStatus.CANCELLED) {
            throw new BusinessException("Нельзя изменять завершённую или отменённую аренду");
        }
        clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Клиент", clientId));
        bikeRepository.findById(bikeId)
                .orElseThrow(() -> new EntityNotFoundException("Велосипед", bikeId));

        rental.setClientId(clientId);
        rental.setBikeId(bikeId);
        return rentalRepository.update(rental);
    }

    public void delete(int id) {
        Rental rental = getById(id);
        if (rental.getStatus() == RentalStatus.ACTIVE) {
            throw new BusinessException("Нельзя удалить активную аренду. Сначала завершите или отмените её.");
        }
        // Если CREATED — возвращаем велосипед
        if (rental.getStatus() == RentalStatus.CREATED) {
            bikeService.setAvailable(rental.getBikeId(), true);
        }
        if (!rentalRepository.deleteById(id)) {
            throw new EntityNotFoundException("Аренда", id);
        }
    }

    public List<Rental> searchByClientId(int clientId) {
        clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Клиент", clientId));
        return rentalRepository.findByClientId(clientId);
    }

    public List<Rental> searchByBikeId(int bikeId) {
        bikeRepository.findById(bikeId)
                .orElseThrow(() -> new EntityNotFoundException("Велосипед", bikeId));
        return rentalRepository.findByBikeId(bikeId);
    }

    public List<Rental> filterByStatus(RentalStatus status) {
        return rentalRepository.findByStatus(status);
    }

    public List<Rental> filterByDateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new BusinessException("Даты начала и конца диапазона обязательны");
        }
        if (from.isAfter(to)) {
            throw new BusinessException("Дата начала диапазона не может быть позже даты конца");
        }
        return rentalRepository.findByDateRange(from, to);
    }

    public List<Rental> sortByStartTimeAsc() {
        return getAll().stream()
                .sorted(Comparator.comparing(Rental::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    public List<Rental> sortByCostDesc() {
        return getAll().stream()
                .sorted(Comparator.comparingDouble(Rental::getTotalCost).reversed())
                .collect(Collectors.toList());
    }

    public long countByStatus(RentalStatus status) {
        return getAll().stream().filter(r -> r.getStatus() == status).count();
    }

    public double totalRevenue() {
        return getAll().stream()
                .filter(r -> r.getStatus() == RentalStatus.COMPLETED)
                .mapToDouble(Rental::getTotalCost)
                .sum();
    }

    private void completeRental(Rental rental) {
        LocalDateTime end = LocalDateTime.now();
        rental.setEndTime(end);
        if (rental.getStartTime() == null) {
            rental.setStartTime(end.minusHours(1));
        }
        // Минимум 1 час
        long minutes = Duration.between(rental.getStartTime(), end).toMinutes();
        double hours = Math.max(1.0, Math.ceil(minutes / 60.0));

        Bike bike = bikeRepository.findById(rental.getBikeId())
                .orElseThrow(() -> new EntityNotFoundException("Велосипед", rental.getBikeId()));
        double cost = hours * bike.getPricePerHour();
        rental.setTotalCost(cost);

        // Возвращаем велосипед
        bikeService.setAvailable(rental.getBikeId(), true);
    }

    private void validateStatusTransition(RentalStatus current, RentalStatus next) {
        if (current == next) {
            throw new BusinessException("Аренда уже имеет статус " + current);
        }
        boolean allowed = switch (current) {
            case CREATED -> next == RentalStatus.ACTIVE || next == RentalStatus.CANCELLED;
            case ACTIVE -> next == RentalStatus.COMPLETED || next == RentalStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!allowed) {
            throw new BusinessException(
                    "Запрещённый переход статуса: " + current + " → " + next +
                            ". Допустимые переходы: CREATED→ACTIVE/CANCELLED, ACTIVE→COMPLETED/CANCELLED"
            );
        }
    }
}
