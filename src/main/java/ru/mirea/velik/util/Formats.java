package ru.mirea.velik.util;

import ru.mirea.velik.model.RentalView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Единые форматы отображения дат и денежных сумм.
 */
public final class Formats {

    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private static final Locale RU = Locale.forLanguageTag("ru-RU");

    private Formats() {
    }

    public static String dateTime(LocalDateTime value) {
        return value != null ? value.format(DATE_TIME) : "—";
    }

    public static String money(double value) {
        return String.format(RU, "%,.2f ₽", value);
    }

    public static String number(double value) {
        return String.format(RU, "%,.1f", value);
    }

    /** Краткое описание заявки одной строкой (для списков связанных аренд). */
    public static String rentalLine(RentalView view) {
        return "№" + view.id() + " · " + view.clientName() + " · " + view.bikeModel() + " · "
                + dateTime(view.startTime()) + " · " + view.status().getTitle() + " · " + money(view.totalCost());
    }
}
