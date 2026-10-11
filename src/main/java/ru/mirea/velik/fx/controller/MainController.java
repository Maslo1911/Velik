package ru.mirea.velik.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import ru.mirea.velik.fx.AppContext;
import ru.mirea.velik.util.DatabaseManager;

import java.util.Map;

/**
 * Главное окно: вкладки разделов, обновление данных и переход к статистике.
 */
public class MainController {

    @FXML
    private TabPane tabPane;
    @FXML
    private Tab rentalsTab;
    @FXML
    private Tab clientsTab;
    @FXML
    private Tab bikesTab;
    @FXML
    private Tab statisticsTab;
    @FXML
    private Label statusLabel;

    // Контроллеры вложенных FXML (fx:include): имя поля = fx:id + "Controller"
    @FXML
    private RentalsController rentalsController;
    @FXML
    private ClientsController clientsController;
    @FXML
    private BikesController bikesController;
    @FXML
    private StatisticsController statisticsController;

    private Map<Tab, Refreshable> sections;

    public MainController(AppContext context) {
        // сервисы главному окну не нужны: с данными работают контроллеры разделов
    }

    @FXML
    private void initialize() {
        sections = Map.of(
                rentalsTab, rentalsController,
                clientsTab, clientsController,
                bikesTab, bikesController,
                statisticsTab, statisticsController
        );
        // При переходе на вкладку данные перечитываются: изменения в одном разделе видны в остальных
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> refresh(newTab));
        statusLabel.setText("База данных: " + DatabaseManager.getUrl());
    }

    @FXML
    private void onRefresh() {
        refresh(tabPane.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void onOpenStatistics() {
        tabPane.getSelectionModel().select(statisticsTab);
    }

    private void refresh(Tab tab) {
        Refreshable section = sections.get(tab);
        if (section != null) {
            section.refresh();
        }
    }
}
