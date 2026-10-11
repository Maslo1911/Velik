package ru.mirea.velik.service;

import org.junit.jupiter.api.Test;
import ru.mirea.velik.model.Bike;
import ru.mirea.velik.model.Client;
import ru.mirea.velik.model.Rental;
import ru.mirea.velik.model.RentalStatus;
import ru.mirea.velik.model.RentalView;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RentalFilterTest {

    private final RentalView view = new RentalView(
            new Rental(7, 1, 2, LocalDateTime.of(2026, 9, 22, 9, 0), null, RentalStatus.ACTIVE, 0),
            new Client(1, "Сидоров Пётр Алексеевич", "+79003334455", null),
            new Bike(2, "Giant Contend AR 3", "шоссейный", false, 300));

    @Test
    void emptyFilterAcceptsEverything() {
        assertThat(new RentalFilter(null, null, null, null, null).test(view)).isTrue();
    }

    @Test
    void searchIgnoresCaseAndLooksAtClientPhoneAndBike() {
        assertThat(new RentalFilter("сидоров", null, null, null, null).test(view)).isTrue();
        assertThat(new RentalFilter("333", null, null, null, null).test(view)).isTrue();
        assertThat(new RentalFilter("GIANT", null, null, null, null).test(view)).isTrue();
        assertThat(new RentalFilter("7", null, null, null, null).test(view)).isTrue();
        assertThat(new RentalFilter("Петрова", null, null, null, null).test(view)).isFalse();
    }

    @Test
    void statusAndTypeMustBothMatch() {
        assertThat(new RentalFilter(null, RentalStatus.ACTIVE, "Шоссейный", null, null).test(view)).isTrue();
        assertThat(new RentalFilter(null, RentalStatus.COMPLETED, "шоссейный", null, null).test(view)).isFalse();
        assertThat(new RentalFilter(null, RentalStatus.ACTIVE, "горный", null, null).test(view)).isFalse();
    }

    @Test
    void periodBoundsAreInclusive() {
        LocalDate day = LocalDate.of(2026, 9, 22);
        assertThat(new RentalFilter(null, null, null, day, day).test(view)).isTrue();
        assertThat(new RentalFilter(null, null, null, day.plusDays(1), null).test(view)).isFalse();
        assertThat(new RentalFilter(null, null, null, null, day.minusDays(1)).test(view)).isFalse();
    }
}
