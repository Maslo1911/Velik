package ru.mirea.velik.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ru.mirea.velik.fx.AppContext;
import ru.mirea.velik.fx.util.Combos;
import ru.mirea.velik.fx.util.InputParser;
import ru.mirea.velik.model.Bike;
import ru.mirea.velik.model.Client;
import ru.mirea.velik.model.RentalStatus;
import ru.mirea.velik.model.RentalView;
import ru.mirea.velik.util.Formats;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Форма заявки на аренду. Клиент и велосипед выбираются из списков, а не вводятся по идентификатору.
 */
public class RentalFormController extends FormController {

    private final AppContext context;

    @FXML
    private ComboBox<Client> clientBox;
    @FXML
    private ComboBox<Bike> bikeBox;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private TextField startTimeField;
    @FXML
    private Label hintLabel;

    /** Редактируемая заявка или null при создании новой. */
    private RentalView editing;

    public RentalFormController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        Combos.setup(clientBox, "Выберите клиента", client -> client.getFullName() + " (" + client.getPhone() + ")");
        Combos.setup(bikeBox, "Выберите велосипед",
                bike -> bike.getModel() + " — " + bike.getType() + ", " + Formats.money(bike.getPricePerHour()) + "/ч");
    }

    public void setRental(RentalView view) {
        this.editing = view;
        clientBox.getItems().setAll(context.clientService().getAll());

        // В списке только свободные велосипеды; при редактировании добавляется велосипед самой заявки
        List<Bike> bikes = new ArrayList<>(context.bikeService().filterAvailable());
        if (view != null && view.bike() != null && !bikes.contains(view.bike())) {
            bikes.add(0, view.bike());
        }
        bikeBox.getItems().setAll(bikes);

        LocalDateTime start = view != null ? view.startTime() : LocalDateTime.now().plusMinutes(5);
        startDatePicker.setValue(start.toLocalDate());
        startTimeField.setText(start.format(Formats.TIME));

        if (view == null) {
            hintLabel.setText("Заявка создаётся в статусе «Создана». Время аренды отсчитывается с момента выдачи велосипеда.");
            return;
        }
        clientBox.setValue(view.client());
        bikeBox.setValue(view.bike());
        if (view.status() == RentalStatus.ACTIVE) {
            // велосипед уже выдан: фактическое время начала изменить нельзя
            startDatePicker.setDisable(true);
            startTimeField.setDisable(true);
            hintLabel.setText("Велосипед уже выдан, поэтому время начала изменить нельзя.");
        } else {
            hintLabel.setText("При смене велосипеда прежний снова станет доступным для аренды.");
        }
    }

    @Override
    protected void save() {
        Integer clientId = clientBox.getValue() != null ? clientBox.getValue().getId() : null;
        Integer bikeId = bikeBox.getValue() != null ? bikeBox.getValue().getId() : null;

        if (editing == null) {
            context.rentalService().create(clientId, bikeId, readStart());
        } else {
            LocalDateTime start = editing.status() == RentalStatus.CREATED ? readStart() : null;
            context.rentalService().update(editing.id(), clientId, bikeId, start);
        }
    }

    private LocalDateTime readStart() {
        return InputParser.dateTime("startDate", startDatePicker.getValue(), "startTime", startTimeField.getText());
    }

    @Override
    protected Map<String, Control> fields() {
        return Map.of(
                "client", clientBox,
                "bike", bikeBox,
                "startDate", startDatePicker,
                "startTime", startTimeField
        );
    }
}
