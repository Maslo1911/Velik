package ru.mirea.velik.fx.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import ru.mirea.velik.fx.AppContext;
import ru.mirea.velik.fx.util.Cells;
import ru.mirea.velik.fx.util.FormDialog;
import ru.mirea.velik.model.Bike;
import ru.mirea.velik.model.RentalView;
import ru.mirea.velik.util.Formats;

import java.util.List;

/**
 * Раздел «Велосипеды».
 */
public class BikesController extends DirectoryController<Bike> {

    @FXML
    private TableColumn<Bike, String> modelColumn;
    @FXML
    private TableColumn<Bike, String> typeColumn;
    @FXML
    private TableColumn<Bike, Double> priceColumn;
    @FXML
    private TableColumn<Bike, String> availableColumn;

    public BikesController(AppContext context) {
        super(context);
    }

    @FXML
    private void initialize() {
        modelColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getModel()));
        typeColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getType()));
        priceColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getPricePerHour()));
        priceColumn.setCellFactory(Cells.formatted(Formats::money));
        availableColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().isAvailable() ? "Свободен" : "Занят"));
        initDirectory();
    }

    @Override
    protected List<Bike> loadItems() {
        return context.bikeService().getAll();
    }

    @Override
    protected boolean matches(Bike bike, String query) {
        return contains(bike.getModel(), query) || contains(bike.getType(), query);
    }

    @Override
    protected String describe(Bike bike) {
        return bike.getModel() + " (" + bike.getType() + ")";
    }

    @Override
    protected void delete(Bike bike) {
        context.bikeService().delete(bike.getId());
    }

    @Override
    protected boolean openForm(Bike bike) {
        String title = bike == null ? "Новый велосипед" : "Изменение велосипеда";
        return FormDialog.<BikeFormController>show(window(), context, "bike-form.fxml", title,
                form -> form.setBike(bike));
    }

    @Override
    protected List<RentalView> rentalsOf(Bike bike) {
        return context.rentalService().getViewsByBike(bike.getId());
    }
}
