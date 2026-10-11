package ru.mirea.velik.service;

import org.junit.jupiter.api.Test;
import ru.mirea.velik.model.Bike;
import ru.mirea.velik.model.Client;
import ru.mirea.velik.model.Rental;
import ru.mirea.velik.model.RentalStatus;
import ru.mirea.velik.model.RentalView;
import ru.mirea.velik.model.Statistics;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StatisticsServiceTest {

    private final Client client = new Client(1, "Иванов Иван Иванович", "+79001112233", null);
    private final Bike city = new Bike(1, "Stels Navigator 500", "городской", true, 150);
    private final Bike mountain = new Bike(2, "Trek Marlin 5", "горный", false, 250);

    private RentalView rental(int id, Bike bike, int day, Integer hours, RentalStatus status, double cost) {
        LocalDateTime start = LocalDateTime.of(2026, 9, day, 10, 0);
        LocalDateTime end = hours != null ? start.plusHours(hours) : null;
        return new RentalView(new Rental(id, client.getId(), bike.getId(), start, end, status, cost), client, bike);
    }

    @Test
    void calculatesIndicatorsOverCompletedRentals() {
        List<RentalView> rentals = List.of(
                rental(1, city, 20, 3, RentalStatus.COMPLETED, 450),
                rental(2, city, 21, 1, RentalStatus.COMPLETED, 150),
                rental(3, mountain, 22, null, RentalStatus.ACTIVE, 0),
                rental(4, city, 23, 1, RentalStatus.CANCELLED, 0));

        Statistics stats = StatisticsService.compute(rentals, 1, List.of(city, mountain), null, null);

        assertThat(stats.totalRentals()).isEqualTo(4);
        assertThat(stats.rentalsByStatus())
                .containsEntry(RentalStatus.COMPLETED, 2L)
                .containsEntry(RentalStatus.ACTIVE, 1L)
                .containsEntry(RentalStatus.CANCELLED, 1L)
                .containsEntry(RentalStatus.CREATED, 0L);
        assertThat(stats.revenue()).isEqualTo(600);
        assertThat(stats.averageCheck()).isEqualTo(300);
        assertThat(stats.minCheck()).isEqualTo(150);
        assertThat(stats.maxCheck()).isEqualTo(450);
        assertThat(stats.averageDurationHours()).isEqualTo(2);
        assertThat(stats.completedSharePercent()).isEqualTo(50);
        assertThat(stats.totalBikes()).isEqualTo(2);
        assertThat(stats.availableBikes()).isEqualTo(1);
        assertThat(stats.mostPopularBikeType()).isEqualTo("городской");
    }

    @Test
    void periodLimitsCountAndRevenue() {
        List<RentalView> rentals = List.of(
                rental(1, city, 20, 3, RentalStatus.COMPLETED, 450),
                rental(2, city, 25, 1, RentalStatus.COMPLETED, 150));

        Statistics stats = StatisticsService.compute(rentals, 1, List.of(city),
                LocalDate.of(2026, 9, 24), LocalDate.of(2026, 9, 30));

        assertThat(stats.rentalsInPeriod()).isEqualTo(1);
        assertThat(stats.revenueInPeriod()).isEqualTo(150);
        assertThat(stats.revenue()).isEqualTo(600);
    }

    @Test
    void emptyDataGivesZeroesInsteadOfErrors() {
        Statistics stats = StatisticsService.compute(List.of(), 0, List.of(), null, null);

        assertThat(stats.totalRentals()).isZero();
        assertThat(stats.averageCheck()).isZero();
        assertThat(stats.minCheck()).isZero();
        assertThat(stats.completedSharePercent()).isZero();
        assertThat(stats.mostPopularBikeType()).isEqualTo("—");
    }
}
