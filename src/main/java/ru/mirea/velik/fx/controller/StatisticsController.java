package ru.mirea.velik.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import ru.mirea.velik.fx.AppContext;
import ru.mirea.velik.fx.util.UiActions;
import ru.mirea.velik.model.RentalStatus;
import ru.mirea.velik.model.Statistics;
import ru.mirea.velik.util.Formats;

import java.util.List;
import java.util.Locale;

/**
 * Раздел «Статистика»: показатели считает StatisticsService, контроллер только отображает их.
 */
public class StatisticsController implements Refreshable {

    private final AppContext context;

    @FXML
    private DatePicker fromPicker;
    @FXML
    private DatePicker toPicker;

    @FXML
    private Label totalRentalsLabel;
    @FXML
    private Label revenueLabel;
    @FXML
    private Label averageCheckLabel;
    @FXML
    private Label minMaxCheckLabel;
    @FXML
    private Label averageDurationLabel;
    @FXML
    private Label completedShareLabel;
    @FXML
    private Label clientsLabel;
    @FXML
    private Label bikesLabel;
    @FXML
    private Label popularTypeLabel;
    @FXML
    private Label periodRentalsLabel;
    @FXML
    private Label periodRevenueLabel;

    @FXML
    private BarChart<String, Number> statusChart;

    public StatisticsController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        refresh();
    }

    @FXML
    private void onCalculate() {
        refresh();
    }

    @FXML
    private void onResetPeriod() {
        fromPicker.setValue(null);
        toPicker.setValue(null);
        refresh();
    }

    @Override
    public void refresh() {
        UiActions.run(() -> show(context.statisticsService().calculate(fromPicker.getValue(), toPicker.getValue())));
    }

    private void show(Statistics stats) {
        totalRentalsLabel.setText(String.valueOf(stats.totalRentals()));
        revenueLabel.setText(Formats.money(stats.revenue()));
        averageCheckLabel.setText(Formats.money(stats.averageCheck()));
        minMaxCheckLabel.setText(Formats.money(stats.minCheck()) + " / " + Formats.money(stats.maxCheck()));
        averageDurationLabel.setText(Formats.number(stats.averageDurationHours()) + " ч");
        completedShareLabel.setText(String.format(Locale.ROOT, "%.0f %%", stats.completedSharePercent()));
        clientsLabel.setText(String.valueOf(stats.totalClients()));
        bikesLabel.setText(stats.availableBikes() + " из " + stats.totalBikes());
        popularTypeLabel.setText(stats.mostPopularBikeType());
        periodRentalsLabel.setText(String.valueOf(stats.rentalsInPeriod()));
        periodRevenueLabel.setText(Formats.money(stats.revenueInPeriod()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (RentalStatus status : RentalStatus.values()) {
            series.getData().add(new XYChart.Data<>(status.getTitle(), stats.rentalsByStatus().get(status)));
        }
        statusChart.getData().setAll(List.of(series));
    }
}
