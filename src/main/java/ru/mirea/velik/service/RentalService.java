package ru.mirea.velik.service;

import ru.mirea.velik.exception.BusinessException;
import ru.mirea.velik.exception.EntityNotFoundException;
import ru.mirea.velik.exception.ValidationException;
import ru.mirea.velik.model.Bike;
import ru.mirea.velik.model.Client;
import ru.mirea.velik.model.Rental;
import ru.mirea.velik.model.RentalStatus;
import ru.mirea.velik.model.RentalView;
import ru.mirea.velik.repository.BikeRepository;
import ru.mirea.velik.repository.ClientRepository;
import ru.mirea.velik.repository.RentalRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RentalService {

    private static final int MAX_PLANNING_DAYS = 30;

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
        return create(Integer.valueOf(clientId), Integer.valueOf(bikeId), LocalDateTime.now());
    }

    /**
     * Создание заявки с плановым временем начала аренды.
     * Фактическое время начала фиксируется при выдаче велосипеда (переход в ACTIVE).
     */
    public Rental create(Integer clientId, Integer bikeId, LocalDateTime plannedStart) {
        requireSelected(clientId, bikeId);
        validatePlannedStart(plannedStart);

        // Бизнес-правило: клиент должен существовать
        clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Клиент", clientId));

        Bike bike = bikeRepository.findById(bikeId)
                .orElseThrow(() -> new EntityNotFoundException("Велосипед", bikeId));

        // Бизнес-правило 5: нельзя арендовать недоступный велосипед
        if (!bike.isAvailable()) {
            throw new ValidationException("bike", "Велосипед «" + bike.getModel() + "» сейчас недоступен для аренды");
        }

        // Бизнес-правило: у велосипеда не должно быть активной аренды
        if (rentalRepository.hasActiveRentalForBike(bikeId)) {
            throw new ValidationException("bike", "Велосипед «" + bike.getModel() + "» уже находится в активной аренде");
        }

        Rental rental = new Rental(clientId, bikeId, plannedStart, RentalStatus.CREATED);
        rental.setTotalCost(0);
        Rental saved = rentalRepository.save(rental);

        // Помечаем велосипед как недоступный. Если это не удалось, отменяем создание заявки,
        // чтобы не осталось заявки при «свободном» велосипеде.
        try {
            bikeService.setAvailable(bikeId, false);
        } catch (RuntimeException e) {
            rentalRepository.deleteById(saved.getId());
            throw e;
        }

        return saved;
    }

    public Rental getById(int id) {
        return rentalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Аренда", id));
    }

    public List<Rental> getAll() {
        return rentalRepository.findAll();
    }

    /** Все заявки вместе со связанными клиентами и велосипедами. */
    public List<RentalView> getAllViews() {
        Map<Integer, Client> clients = clientRepository.findAll().stream()
                .collect(Collectors.toMap(Client::getId, Function.identity()));
        Map<Integer, Bike> bikes = bikeRepository.findAll().stream()
                .collect(Collectors.toMap(Bike::getId, Function.identity()));
        return rentalRepository.findAll().stream()
                .map(r -> new RentalView(r, clients.get(r.getClientId()), bikes.get(r.getBikeId())))
                .collect(Collectors.toList());
    }

    public List<RentalView> getViewsByClient(int clientId) {
        return getAllViews().stream()
                .filter(v -> v.rental().getClientId() == clientId)
                .collect(Collectors.toList());
    }

    public List<RentalView> getViewsByBike(int bikeId) {
        return getAllViews().stream()
                .filter(v -> v.rental().getBikeId() == bikeId)
                .collect(Collectors.toList());
    }

    /**
     * Изменение статуса с проверкой допустимых переходов.
     * Сначала сохраняется сама аренда, и только потом меняется доступность велосипеда.
     */
    public Rental changeStatus(int id, RentalStatus newStatus) {
        Rental rental = getById(id);

        // Бизнес-правило 6: допустимые переходы статусов
        validateStatusTransition(rental.getStatus(), newStatus);

        LocalDateTime now = LocalDateTime.now();
        rental.setStatus(newStatus);

        switch (newStatus) {
            // Фактическое начало аренды: считаем время с момента выдачи велосипеда
            case ACTIVE -> rental.setStartTime(now);
            case COMPLETED -> {
                rental.setEndTime(now);
                rental.setTotalCost(calculateCost(rental, now));
            }
            // Заявку с плановым началом в будущем закрываем временем начала:
            // в базе действует ограничение end_time >= start_time
            case CANCELLED -> rental.setEndTime(now.isBefore(rental.getStartTime()) ? rental.getStartTime() : now);
            default -> { }
        }

        Rental saved = rentalRepository.update(rental);

        // Велосипед освобождается при завершении или отмене
        if (newStatus == RentalStatus.COMPLETED || newStatus == RentalStatus.CANCELLED) {
            bikeService.setAvailable(rental.getBikeId(), true);
        }
        return saved;
    }

    /**
     * Изменение клиента и/или велосипеда в незавершённой аренде.
     * При смене велосипеда старый освобождается, новый занимается.
     */
    public Rental update(int id, int clientId, int bikeId) {
        return update(id, Integer.valueOf(clientId), Integer.valueOf(bikeId), null);
    }

    /**
     * Изменение заявки. Плановое время начала можно менять только до выдачи велосипеда
     * (статус CREATED); plannedStart == null означает «не менять».
     */
    public Rental update(int id, Integer clientId, Integer bikeId, LocalDateTime plannedStart) {
        requireSelected(clientId, bikeId);
        Rental rental = getById(id);
        if (rental.getStatus() == RentalStatus.COMPLETED || rental.getStatus() == RentalStatus.CANCELLED) {
            throw new BusinessException("Нельзя изменять завершённую или отменённую аренду");
        }
        if (plannedStart != null && !sameMinute(plannedStart, rental.getStartTime())) {
            if (rental.getStatus() != RentalStatus.CREATED) {
                throw new ValidationException("startDate", "Время начала нельзя менять после выдачи велосипеда");
            }
            validatePlannedStart(plannedStart);
            rental.setStartTime(plannedStart);
        }
        clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Клиент", clientId));
        Bike newBike = bikeRepository.findById(bikeId)
                .orElseThrow(() -> new EntityNotFoundException("Велосипед", bikeId));

        int oldBikeId = rental.getBikeId();
        boolean bikeChanged = oldBikeId != bikeId;

        // Новый велосипед должен быть свободен
        if (bikeChanged && (!newBike.isAvailable() || rentalRepository.hasActiveRentalForBike(bikeId))) {
            throw new ValidationException("bike", "Велосипед «" + newBike.getModel() + "» недоступен для аренды");
        }

        rental.setClientId(clientId);
        rental.setBikeId(bikeId);
        Rental saved = rentalRepository.update(rental);

        if (bikeChanged) {
            bikeService.setAvailable(bikeId, false);
            bikeService.setAvailable(oldBikeId, true);
        }
        return saved;
    }

    public void delete(int id) {
        Rental rental = getById(id);
        if (rental.getStatus() == RentalStatus.ACTIVE) {
            throw new BusinessException("Нельзя удалить активную аренду. Сначала завершите или отмените её.");
        }
        if (!rentalRepository.deleteById(id)) {
            throw new EntityNotFoundException("Аренда", id);
        }
        // Если была CREATED, велосипед всё ещё занят этой заявкой, возвращаем его
        if (rental.getStatus() == RentalStatus.CREATED) {
            bikeService.setAvailable(rental.getBikeId(), true);
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

    /** Количество аренд по каждому статусу (для статистики). */
    public Map<RentalStatus, Long> countByStatuses() {
        Map<RentalStatus, Long> result = new EnumMap<>(RentalStatus.class);
        for (RentalStatus status : RentalStatus.values()) {
            result.put(status, 0L);
        }
        for (Rental rental : getAll()) {
            result.merge(rental.getStatus(), 1L, Long::sum);
        }
        return result;
    }

    public double totalRevenue() {
        return getAll().stream()
                .filter(r -> r.getStatus() == RentalStatus.COMPLETED)
                .mapToDouble(Rental::getTotalCost)
                .sum();
    }

    /** Стоимость = число начатых часов (минимум 1) * цена за час. */
    private double calculateCost(Rental rental, LocalDateTime end) {
        long minutes = Duration.between(rental.getStartTime(), end).toMinutes();
        double hours = Math.max(1.0, Math.ceil(minutes / 60.0));

        Bike bike = bikeRepository.findById(rental.getBikeId())
                .orElseThrow(() -> new EntityNotFoundException("Велосипед", rental.getBikeId()));
        return hours * bike.getPricePerHour();
    }

    private void requireSelected(Integer clientId, Integer bikeId) {
        if (clientId == null) {
            throw new ValidationException("client", "Выберите клиента из списка");
        }
        if (bikeId == null) {
            throw new ValidationException("bike", "Выберите велосипед из списка");
        }
    }

    /** Плановое начало: не раньше сегодняшнего дня и не позже чем через MAX_PLANNING_DAYS дней. */
    private void validatePlannedStart(LocalDateTime plannedStart) {
        if (plannedStart == null) {
            throw new ValidationException("startDate", "Укажите дату и время начала аренды");
        }
        LocalDate today = LocalDate.now();
        if (plannedStart.toLocalDate().isBefore(today)) {
            throw new ValidationException("startDate", "Дата начала аренды не может быть в прошлом");
        }
        if (plannedStart.toLocalDate().isAfter(today.plusDays(MAX_PLANNING_DAYS))) {
            throw new ValidationException("startDate",
                    "Аренду можно запланировать не более чем на " + MAX_PLANNING_DAYS + " дней вперёд");
        }
    }

    private boolean sameMinute(LocalDateTime a, LocalDateTime b) {
        return b != null && a.truncatedTo(ChronoUnit.MINUTES).equals(b.truncatedTo(ChronoUnit.MINUTES));
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
