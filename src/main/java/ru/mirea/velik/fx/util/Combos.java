package ru.mirea.velik.fx.util;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

import java.util.function.Function;

/**
 * Настройка выпадающих списков с объектами предметной области.
 */
public final class Combos {

    private Combos() {
    }

    /**
     * Задаёт текст элементов списка и подсказку, которая показывается, пока ничего не выбрано
     * (в том числе после сброса значения).
     */
    public static <T> void setup(ComboBox<T> combo, String prompt, Function<T, String> text) {
        combo.setPromptText(prompt);
        combo.setCellFactory(list -> cell(null, text));
        combo.setButtonCell(cell(prompt, text));
    }

    private static <T> ListCell<T> cell(String emptyText, Function<T, String> text) {
        return new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? emptyText : text.apply(item));
            }
        };
    }
}
