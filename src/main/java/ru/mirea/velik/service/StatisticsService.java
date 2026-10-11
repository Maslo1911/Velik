package ru.mirea.velik.service;

import ru.mirea.velik.exception.ValidationException;
import ru.mirea.velik.model.Bike;
import ru.mirea.velik.model.RentalStatus;
import ru.mirea.velik.model.RentalView;
import ru.mirea.velik.model.Statistics;

import java.time.Duration;
import java.time.LocalDate;
import java.util.DoubleSummaryStatistics;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Расчёт статистических показателей проката.
 */
public class StatisticsService {

    private final RentalService rentalService;
    private final ClientService clientService;
    private final BikeService bikeService;

    public StatisticsService(RentalService rentalService, ClientService clientService, BikeService bikeService) {
        this.rentalService = rentalService;
        this.clientService = clientService;
        this.bikeService = bikeService;
    }

    /**
     * @param from начало периода (включительно) или null
     * @param to   конец периода (включительно) или null
     */
    public Statistics calculate(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ValidationException("period", "Дата начала периода не может быть позже даты окончания");
        }
        return compute(rentalService.getAllViews(), clientService.getAll().size(), bikeService.getAll(), from, to);
    }

    static Statistics compute(List<RentalView> rentals, long totalClients, List<Bike> bikes,
                              LocalDate from, LocalDate to) {
        Map<RentalStatus, Long> byStatus = new EnumMap<>(RentalStatus.class);
        for (RentalStatus status : RentalStatus.values()) {
            byStatus.put(status, 0L);
        }
        for (RentalView view : rentals) {
            byStatus.merge(view.status(), 1L, Long::sum);
        }

        List<RentalView> completed = rentals.stream()
                .filter(v -> v.status() == RentalStatus.COMPLETED)
                .collect(Collectors.toList());

        // Выручка, средний, минимальный и максимальный чек считаются по завершённым арендам
        DoubleSummaryStatistics checks = completed.stream().mapToDouble(RentalView::totalCost).summaryStatistics();
        boolean hasCompleted = checks.getCount() > 0;

        double averageDurationHours = completed.stream()
                .filter(v -> v.startTime() != null && v.endTime() != null)
                .mapToLong(v -> Duration.between(v.startTime(), v.endTime()).toMinutes())
                .average()
                .orElse(0) / 60.0;

        double completedShare = rentals.isEmpty() ? 0 : completed.size() * 100.0 / rentals.size();

        String popularType = rentals.stream()
                .filter(v -> v.bike() != null)
                .collect(Collectors.groupingBy(RentalView::bikeType, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry.<String, Long>comparingByKey()))
                .map(Map.Entry::getKey)
                .orElse("—");

        RentalFilter period = new RentalFilter(null, null, null, from, to);
        List<RentalView> inPeriod = rentals.stream().filter(period).collect(Collectors.toList());
        double revenueInPeriod = inPeriod.stream()
                .filter(v -> v.status() == RentalStatus.COMPLETED)
                .mapToDouble(RentalView::totalCost)
                .sum();

        return new Statistics(
                rentals.size(),
                byStatus,
                checks.getSum(),
                hasCompleted ? checks.getAverage() : 0,
                hasCompleted ? checks.getMin() : 0,
                hasCompleted ? checks.getMax() : 0,
                averageDurationHours,
                completedShare,
                totalClients,
                bikes.size(),
                bikes.stream().filter(Bike::isAvailable).count(),
                popularType,
                inPeriod.size(),
                revenueInPeriod
        );
    }
}
