package ru.mirea.velik.service;

import ru.mirea.velik.model.RentalStatus;
import ru.mirea.velik.model.RentalView;

import java.time.LocalDate;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Условия отбора заявок: строка поиска, статус, тип велосипеда и период дат начала.
 * Пустое (null) условие не ограничивает выборку.
 */
public record RentalFilter(String query, RentalStatus status, String bikeType, LocalDate from, LocalDate to)
        implements Predicate<RentalView> {

    @Override
    public boolean test(RentalView view) {
        return matchesQuery(view)
                && (status == null || view.status() == status)
                && (bikeType == null || bikeType.equalsIgnoreCase(view.bikeType()))
                && matchesPeriod(view);
    }

    /** Поиск по ФИО клиента, телефону, модели велосипеда и номеру заявки (регистр не важен). */
    private boolean matchesQuery(RentalView view) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        return contains(view.clientName(), needle)
                || contains(view.clientPhone(), needle)
                || contains(view.bikeModel(), needle)
                || String.valueOf(view.id()).equals(needle);
    }

    private boolean matchesPeriod(RentalView view) {
        if (from == null && to == null) {
            return true;
        }
        if (view.startTime() == null) {
            return false;
        }
        LocalDate start = view.startTime().toLocalDate();
        return (from == null || !start.isBefore(from)) && (to == null || !start.isAfter(to));
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
}
