package ru.mirea.velik.fx.util;

import ru.mirea.velik.exception.ValidationException;
import ru.mirea.velik.util.Formats;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Преобразование текста из полей ввода в числа и даты с понятными сообщениями об ошибках.
 */
public final class InputParser {

    private InputParser() {
    }

    /** Цена: допускаются и точка, и запятая в качестве разделителя. */
    public static double price(String field, String text) {
        if (text == null || text.isBlank()) {
            throw new ValidationException(field, "Укажите цену за час");
        }
        try {
            double value = Double.parseDouble(text.trim().replace(',', '.').replace(" ", ""));
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new NumberFormatException(text);
            }
            return Math.round(value * 100) / 100.0;
        } catch (NumberFormatException e) {
            throw new ValidationException(field, "Цена за час должна быть числом, например 150 или 199,50");
        }
    }

    public static LocalDateTime dateTime(String dateField, LocalDate date, String timeField, String timeText) {
        if (date == null) {
            throw new ValidationException(dateField, "Укажите дату начала аренды");
        }
        if (timeText == null || timeText.isBlank()) {
            throw new ValidationException(timeField, "Укажите время начала аренды в формате ЧЧ:ММ");
        }
        try {
            return LocalDateTime.of(date, LocalTime.parse(timeText.trim(), Formats.TIME));
        } catch (DateTimeParseException e) {
            throw new ValidationException(timeField, "Время начала укажите в формате ЧЧ:ММ, например 09:30");
        }
    }
}
