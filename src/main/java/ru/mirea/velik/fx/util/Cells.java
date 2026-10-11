package ru.mirea.velik.fx.util;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import ru.mirea.velik.model.RentalStatus;

import java.util.Locale;
import java.util.function.Function;

/**
 * Фабрики ячеек таблиц.
 */
public final class Cells {

    private Cells() {
    }

    /** Ячейка, показывающая значение в заданном текстовом формате (значение при этом остаётся сортируемым). */
    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> formatted(Function<T, String> formatter) {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : formatter.apply(item));
            }
        };
    }

    /** Ячейка статуса: русское название и цвет, заданный в CSS (status-created, status-active, ...). */
    public static <S> Callback<TableColumn<S, RentalStatus>, TableCell<S, RentalStatus>> status() {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(RentalStatus item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeIf(name -> name.startsWith("status-"));
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTitle());
                    getStyleClass().add(statusStyle(item));
                }
            }
        };
    }

    public static String statusStyle(RentalStatus status) {
        return "status-" + status.name().toLowerCase(Locale.ROOT);
    }
}
