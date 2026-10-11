package ru.mirea.velik.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import ru.mirea.velik.fx.AppContext;
import ru.mirea.velik.fx.util.InputParser;
import ru.mirea.velik.model.Bike;

import java.util.Locale;
import java.util.Map;

/**
 * Форма велосипеда. Тип выбирается из уже существующих или вводится новый.
 */
public class BikeFormController extends FormController {

    private final AppContext context;

    @FXML
    private TextField modelField;
    @FXML
    private ComboBox<String> typeBox;
    @FXML
    private TextField priceField;
    @FXML
    private CheckBox availableCheck;

    /** Редактируемый велосипед или null при создании нового. */
    private Bike editing;

    public BikeFormController(AppContext context) {
        this.context = context;
    }

    public void setBike(Bike bike) {
        this.editing = bike;
        typeBox.getItems().setAll(context.bikeService().getTypes());
        if (bike != null) {
            modelField.setText(bike.getModel());
            typeBox.setValue(bike.getType());
            priceField.setText(String.format(Locale.ROOT, "%.2f", bike.getPricePerHour()));
            availableCheck.setSelected(bike.isAvailable());
        }
    }

    @Override
    protected void save() {
        // у редактируемого списка читаем текст поля: пользователь мог ввести новый тип и не нажать Enter
        String type = typeBox.getEditor().getText();
        double price = InputParser.price("price", priceField.getText());

        if (editing == null) {
            context.bikeService().create(modelField.getText(), type, availableCheck.isSelected(), price);
        } else {
            context.bikeService().update(editing.getId(), modelField.getText(), type, availableCheck.isSelected(), price);
        }
    }

    @Override
    protected Map<String, Control> fields() {
        return Map.of(
                "model", modelField,
                "type", typeBox,
                "price", priceField,
                "available", availableCheck
        );
    }
}
