package ru.mirea.velik.fx.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import ru.mirea.velik.fx.AppContext;
import ru.mirea.velik.fx.util.Alerts;
import ru.mirea.velik.fx.util.UiActions;
import ru.mirea.velik.model.RentalView;
import ru.mirea.velik.util.Formats;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Общая логика разделов-справочников (клиенты, велосипеды): таблица, поиск,
 * добавление, изменение, удаление с подтверждением и список связанных аренд.
 *
 * @param <T> тип записи справочника
 */
public abstract class DirectoryController<T> implements Refreshable {

    protected final AppContext context;

    private final ObservableList<T> items = FXCollections.observableArrayList();
    private final FilteredList<T> filtered = new FilteredList<>(items);

    @FXML
    protected TableView<T> table;
    @FXML
    protected TextField searchField;
    @FXML
    protected Button editButton;
    @FXML
    protected Button deleteButton;
    @FXML
    protected ListView<String> rentalsList;
    @FXML
    protected Label relatedTitle;
    @FXML
    protected Label countLabel;

    protected DirectoryController(AppContext context) {
        this.context = context;
    }

    /** Все записи справочника из базы данных. */
    protected abstract List<T> loadItems();

    /** Подходит ли запись под строку поиска (строка уже в нижнем регистре). */
    protected abstract boolean matches(T item, String query);

    /** Название записи для сообщений пользователю. */
    protected abstract String describe(T item);

    protected abstract void delete(T item);

    /** Открывает форму; item == null означает добавление. @return true, если данные сохранены */
    protected abstract boolean openForm(T item);

    /** Аренды, связанные с записью. */
    protected abstract List<RentalView> rentalsOf(T item);

    /** Вызывается из initialize() наследника после настройки столбцов. */
    protected final void initDirectory() {
        SortedList<T> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.setPlaceholder(new Label("Записи не найдены"));
        table.setRowFactory(view -> editOnDoubleClick());
        rentalsList.setPlaceholder(new Label("Аренд нет"));

        editButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applySearch());
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> showRelated(newValue));
        refresh();
    }

    private TableRow<T> editOnDoubleClick() {
        TableRow<T> row = new TableRow<>();
        row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !row.isEmpty()) {
                onEdit();
            }
        });
        return row;
    }

    @Override
    public void refresh() {
        T selected = table.getSelectionModel().getSelectedItem();
        UiActions.run(() -> items.setAll(loadItems()));
        if (selected != null) {
            table.getSelectionModel().select(selected); // записи сравниваются по id
        }
        showRelated(table.getSelectionModel().getSelectedItem());
        updateCount();
    }

    private void applySearch() {
        String text = searchField.getText();
        String query = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        filtered.setPredicate(query.isEmpty() ? null : item -> matches(item, query));
        updateCount();
    }

    private void updateCount() {
        countLabel.setText("Показано записей: " + filtered.size() + " из " + items.size());
    }

    private void showRelated(T item) {
        rentalsList.getItems().clear();
        if (item == null) {
            relatedTitle.setText("Выберите запись, чтобы увидеть связанные аренды");
            return;
        }
        relatedTitle.setText("Аренды: " + describe(item));
        UiActions.run(() -> rentalsList.getItems().setAll(
                rentalsOf(item).stream().map(Formats::rentalLine).collect(Collectors.toList())));
    }

    @FXML
    protected void onAdd() {
        showForm(null);
    }

    @FXML
    protected void onEdit() {
        T item = table.getSelectionModel().getSelectedItem();
        if (item != null) {
            showForm(item);
        }
    }

    private void showForm(T item) {
        boolean[] saved = new boolean[1];
        UiActions.run(() -> saved[0] = openForm(item));
        if (saved[0]) {
            refresh();
        }
    }

    @FXML
    protected void onDelete() {
        T item = table.getSelectionModel().getSelectedItem();
        if (item == null) {
            return;
        }
        boolean confirmed = Alerts.confirm("Удалить запись?",
                describe(item) + "\n\nЗапись будет удалена без возможности восстановления.", "Удалить");
        if (confirmed && UiActions.run(() -> delete(item))) {
            refresh();
        }
    }

    @FXML
    protected void onResetSearch() {
        searchField.clear();
    }

    protected Window window() {
        return table.getScene().getWindow();
    }

    protected static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}
