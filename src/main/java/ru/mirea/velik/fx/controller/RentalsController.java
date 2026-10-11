package ru.mirea.velik.fx.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.velik.fx.AppContext;
import ru.mirea.velik.fx.util.Alerts;
import ru.mirea.velik.fx.util.Cells;
import ru.mirea.velik.fx.util.Combos;
import ru.mirea.velik.fx.util.FormDialog;
import ru.mirea.velik.fx.util.UiActions;
import ru.mirea.velik.model.Rental;
import ru.mirea.velik.model.RentalStatus;
import ru.mirea.velik.model.RentalView;
import ru.mirea.velik.service.RentalFilter;
import ru.mirea.velik.util.Formats;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Раздел «Заявки на аренду»: таблица основной сущности, CRUD, смена статуса,
 * поиск, фильтры, сортировка, просмотр связанных данных и экспорт.
 */
public class RentalsController implements Refreshable {

    private static final Logger log = LoggerFactory.getLogger(RentalsController.class);
    private static final String ERROR_STYLE = "field-error";

    private final AppContext context;

    // Исходный список -> отфильтрованный -> отсортированный (его показывает таблица)
    private final ObservableList<RentalView> rentals = FXCollections.observableArrayList();
    private final FilteredList<RentalView> filtered = new FilteredList<>(rentals);

    @FXML
    private TableView<RentalView> table;
    @FXML
    private TableColumn<RentalView, Integer> idColumn;
    @FXML
    private TableColumn<RentalView, String> clientColumn;
    @FXML
    private TableColumn<RentalView, String> phoneColumn;
    @FXML
    private TableColumn<RentalView, String> bikeColumn;
    @FXML
    private TableColumn<RentalView, String> typeColumn;
    @FXML
    private TableColumn<RentalView, LocalDateTime> startColumn;
    @FXML
    private TableColumn<RentalView, LocalDateTime> endColumn;
    @FXML
    private TableColumn<RentalView, RentalStatus> statusColumn;
    @FXML
    private TableColumn<RentalView, Double> costColumn;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<RentalStatus> statusFilter;
    @FXML
    private ComboBox<String> typeFilter;
    @FXML
    private DatePicker fromPicker;
    @FXML
    private DatePicker toPicker;
    @FXML
    private ComboBox<SortOption> sortBox;
    @FXML
    private Label filterErrorLabel;
    @FXML
    private Label countLabel;

    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button issueButton;
    @FXML
    private Button completeButton;
    @FXML
    private Button cancelButton;

    @FXML
    private Label detailsTitle;
    @FXML
    private Label detailsStatus;
    @FXML
    private Label detailsClient;
    @FXML
    private Label detailsPhone;
    @FXML
    private Label detailsEmail;
    @FXML
    private Label detailsBike;
    @FXML
    private Label detailsType;
    @FXML
    private Label detailsPrice;
    @FXML
    private Label detailsStart;
    @FXML
    private Label detailsEnd;
    @FXML
    private Label detailsCost;

    public RentalsController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        configureColumns();

        // SortedList повторяет порядок, выбранный в заголовках таблицы
        SortedList<RentalView> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.setPlaceholder(new Label("Заявки не найдены"));
        table.setRowFactory(view -> editOnDoubleClick());

        Combos.setup(statusFilter, "Все статусы", RentalStatus::getTitle);
        statusFilter.getItems().setAll(RentalStatus.values());
        Combos.setup(typeFilter, "Все типы", type -> type);
        configureSorting();

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        statusFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        typeFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        fromPicker.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        toPicker.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter());

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> showDetails(newValue));
        showDetails(null);
        refresh();
    }

    private void configureColumns() {
        idColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().id()));
        clientColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().clientName()));
        phoneColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().clientPhone()));
        bikeColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().bikeModel()));
        typeColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().bikeType()));
        startColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().startTime()));
        startColumn.setCellFactory(Cells.formatted(Formats::dateTime));
        endColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().endTime()));
        endColumn.setCellFactory(Cells.formatted(Formats::dateTime));
        statusColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().status()));
        statusColumn.setCellFactory(Cells.status());
        costColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().totalCost()));
        costColumn.setCellFactory(Cells.formatted(Formats::money));
    }

    private void configureSorting() {
        sortBox.getItems().setAll(
                new SortOption("Сначала новые", startColumn, TableColumn.SortType.DESCENDING),
                new SortOption("Сначала старые", startColumn, TableColumn.SortType.ASCENDING),
                new SortOption("Стоимость: по убыванию", costColumn, TableColumn.SortType.DESCENDING),
                new SortOption("Стоимость: по возрастанию", costColumn, TableColumn.SortType.ASCENDING),
                new SortOption("Клиент: А–Я", clientColumn, TableColumn.SortType.ASCENDING),
                new SortOption("Статус", statusColumn, TableColumn.SortType.ASCENDING)
        );
        Combos.setup(sortBox, "Сортировка", SortOption::title);
        sortBox.valueProperty().addListener((obs, oldValue, option) -> {
            if (option != null) {
                option.column().setSortType(option.type());
                table.getSortOrder().clear();
                table.getSortOrder().add(option.column());
            }
        });
        sortBox.getSelectionModel().selectFirst();
    }

    private TableRow<RentalView> editOnDoubleClick() {
        TableRow<RentalView> row = new TableRow<>();
        row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !row.isEmpty() && !editButton.isDisabled()) {
                onEdit();
            }
        });
        return row;
    }

    @Override
    public void refresh() {
        RentalView selected = selected();
        UiActions.run(() -> {
            rentals.setAll(context.rentalService().getAllViews());
            refreshTypeFilter(context.bikeService().getTypes());
        });
        if (selected != null) {
            // после перезагрузки выделяем ту же заявку по её номеру
            table.getItems().stream()
                    .filter(view -> view.id() == selected.id())
                    .findFirst()
                    .ifPresent(view -> table.getSelectionModel().select(view));
        }
        updateCount();
    }

    private void refreshTypeFilter(List<String> types) {
        String current = typeFilter.getValue();
        typeFilter.getItems().setAll(types);
        typeFilter.setValue(current != null && types.contains(current) ? current : null);
    }

    // ---------- Поиск и фильтрация ----------

    private void applyFilter() {
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();
        boolean periodInvalid = from != null && to != null && from.isAfter(to);

        setErrorStyle(fromPicker, periodInvalid);
        setErrorStyle(toPicker, periodInvalid);
        filterErrorLabel.setVisible(periodInvalid);
        filterErrorLabel.setManaged(periodInvalid);
        if (periodInvalid) {
            // некорректный период не применяется, остальные условия продолжают работать
            from = null;
            to = null;
        }

        filtered.setPredicate(new RentalFilter(searchField.getText(), statusFilter.getValue(), typeFilter.getValue(), from, to));
        updateCount();
    }

    private void setErrorStyle(DatePicker picker, boolean error) {
        picker.getStyleClass().remove(ERROR_STYLE);
        if (error) {
            picker.getStyleClass().add(ERROR_STYLE);
        }
    }

    @FXML
    private void onResetFilters() {
        searchField.clear();
        statusFilter.setValue(null);
        typeFilter.setValue(null);
        fromPicker.setValue(null);
        toPicker.setValue(null);
    }

    private void updateCount() {
        countLabel.setText("Показано заявок: " + filtered.size() + " из " + rentals.size());
    }

    // ---------- Добавление, изменение, удаление ----------

    @FXML
    private void onAdd() {
        openForm(null, "Новая заявка на аренду");
    }

    @FXML
    private void onEdit() {
        RentalView view = selected();
        if (view != null) {
            openForm(view, "Изменение заявки №" + view.id());
        }
    }

    private void openForm(RentalView view, String title) {
        // UiActions перехватывает ошибку базы данных при загрузке списков клиентов и велосипедов для формы
        boolean[] saved = new boolean[1];
        UiActions.run(() -> saved[0] = FormDialog.<RentalFormController>show(window(), context, "rental-form.fxml", title,
                form -> form.setRental(view)));
        if (saved[0]) {
            refresh();
        }
    }

    @FXML
    private void onDelete() {
        RentalView view = selected();
        if (view == null) {
            return;
        }
        boolean confirmed = Alerts.confirm("Удалить заявку №" + view.id() + "?",
                "Клиент: " + view.clientName() + "\nВелосипед: " + view.bikeModel()
                        + "\n\nЗаявка будет удалена без возможности восстановления.", "Удалить");
        if (confirmed && UiActions.run(() -> context.rentalService().delete(view.id()))) {
            refresh();
        }
    }

    // ---------- Смена статуса ----------

    @FXML
    private void onIssue() {
        changeStatus(RentalStatus.ACTIVE);
    }

    @FXML
    private void onComplete() {
        RentalView view = selected();
        if (view == null || !Alerts.confirm("Завершить аренду №" + view.id() + "?",
                "Велосипед «" + view.bikeModel() + "» будет принят, стоимость рассчитается автоматически.", "Завершить")) {
            return;
        }
        Rental[] completed = new Rental[1];
        if (UiActions.run(() -> completed[0] = context.rentalService().changeStatus(view.id(), RentalStatus.COMPLETED))) {
            refresh();
            Alerts.info("Аренда №" + view.id() + " завершена", "К оплате: " + Formats.money(completed[0].getTotalCost()));
        }
    }

    @FXML
    private void onCancel() {
        RentalView view = selected();
        if (view != null && Alerts.confirm("Отменить заявку №" + view.id() + "?",
                "Велосипед «" + view.bikeModel() + "» снова станет доступным для аренды.", "Отменить заявку")) {
            changeStatus(RentalStatus.CANCELLED);
        }
    }

    private void changeStatus(RentalStatus status) {
        RentalView view = selected();
        if (view != null && UiActions.run(() -> context.rentalService().changeStatus(view.id(), status))) {
            refresh();
        }
    }

    // ---------- Экспорт ----------

    @FXML
    private void onExport() {
        List<RentalView> current = List.copyOf(table.getItems());
        if (current.isEmpty()) {
            Alerts.warning("Нет данных для экспорта: измените условия поиска или фильтры.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Экспорт заявок");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Книга Excel (*.xlsx)", "*.xlsx"),
                new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        chooser.setInitialFileName("rentals_" + LocalDate.now() + ".xlsx");

        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return; // пользователь закрыл окно выбора файла
        }
        file = withExtension(file, chooser.getSelectedExtensionFilter());
        try {
            context.exportService().export(current, file.toPath());
            Alerts.info("Экспорт выполнен", "Сохранено заявок: " + current.size() + "\nФайл: " + file.getAbsolutePath());
        } catch (IOException e) {
            log.error("Ошибка экспорта в файл {}", file, e);
            Alerts.error("Не удалось сохранить файл",
                    "Проверьте, что файл не открыт в другой программе и папка доступна для записи.\n" + e.getMessage());
        }
    }

    /** Дописывает расширение выбранного формата, если пользователь ввёл имя файла без него. */
    private File withExtension(File file, FileChooser.ExtensionFilter filter) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".xlsx") || name.endsWith(".csv")) {
            return file;
        }
        String extension = filter != null ? filter.getExtensions().get(0).substring(1) : ".xlsx";
        return new File(file.getParentFile(), file.getName() + extension);
    }

    // ---------- Просмотр выбранной заявки и связанных данных ----------

    private void showDetails(RentalView view) {
        RentalStatus status = view != null ? view.status() : null;
        boolean open = status == RentalStatus.CREATED || status == RentalStatus.ACTIVE;

        // Кнопки доступны только для тех действий, которые разрешены бизнес-правилами
        editButton.setDisable(!open);
        deleteButton.setDisable(view == null || status == RentalStatus.ACTIVE);
        issueButton.setDisable(status != RentalStatus.CREATED);
        completeButton.setDisable(status != RentalStatus.ACTIVE);
        cancelButton.setDisable(!open);

        detailsStatus.getStyleClass().removeIf(name -> name.startsWith("status-"));
        if (view == null) {
            detailsTitle.setText("Выберите заявку в таблице");
            for (Label label : List.of(detailsStatus, detailsClient, detailsPhone, detailsEmail, detailsBike,
                    detailsType, detailsPrice, detailsStart, detailsEnd, detailsCost)) {
                label.setText("—");
            }
            return;
        }
        detailsTitle.setText("Заявка №" + view.id());
        detailsStatus.setText(status.getTitle());
        detailsStatus.getStyleClass().add(Cells.statusStyle(status));
        detailsClient.setText(view.clientName());
        detailsPhone.setText(view.clientPhone());
        detailsEmail.setText(view.clientEmail());
        detailsBike.setText(view.bikeModel());
        detailsType.setText(view.bikeType());
        detailsPrice.setText(view.bike() != null ? Formats.money(view.bike().getPricePerHour()) + " в час" : "—");
        detailsStart.setText(Formats.dateTime(view.startTime()));
        detailsEnd.setText(Formats.dateTime(view.endTime()));
        detailsCost.setText(status == RentalStatus.COMPLETED ? Formats.money(view.totalCost()) : "—");
    }

    private RentalView selected() {
        return table.getSelectionModel().getSelectedItem();
    }

    private Window window() {
        return table.getScene().getWindow();
    }

    /** Вариант сортировки: столбец таблицы и направление. */
    private record SortOption(String title, TableColumn<RentalView, ?> column, TableColumn.SortType type) {
    }
}
