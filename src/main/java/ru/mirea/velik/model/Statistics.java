package ru.mirea.velik.model;

import java.util.Map;

/**
 * Рассчитанные показатели работы проката.
 */
public record Statistics(
        long totalRentals,
        Map<RentalStatus, Long> rentalsByStatus,
        double revenue,
        double averageCheck,
        double minCheck,
        double maxCheck,
        double averageDurationHours,
        double completedSharePercent,
        long totalClients,
        long totalBikes,
        long availableBikes,
        String mostPopularBikeType,
        long rentalsInPeriod,
        double revenueInPeriod
) {
}
