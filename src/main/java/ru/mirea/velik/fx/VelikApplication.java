package ru.mirea.velik.fx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.velik.exception.DatabaseException;
import ru.mirea.velik.fx.util.Alerts;
import ru.mirea.velik.util.DatabaseManager;

/**
 * JavaFX-приложение системы проката велосипедов.
 */
public class VelikApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(VelikApplication.class);

    @Override
    public void start(Stage stage) {
        // Любая необработанная ошибка в обработчиках интерфейса: пишем в журнал, пользователю показываем понятный текст
        Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> {
            log.error("Необработанная ошибка интерфейса", error);
            Alerts.error("Непредвиденная ошибка", "Операция не выполнена. Повторите действие или перезапустите приложение.");
        });

        try {
            DatabaseManager.checkConnection();
        } catch (DatabaseException e) {
            log.error("База данных недоступна", e);
            Alerts.error("Нет подключения к базе данных", e.getMessage());
            Platform.exit();
            return;
        }

        AppContext context = new AppContext();
        Parent root = context.viewLoader().load("main.fxml");

        Scene scene = new Scene(root, 1280, 760);
        scene.getStylesheets().add(ViewLoader.stylesheet());

        stage.setTitle("Velik — прокат велосипедов");
        stage.setMinWidth(980);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
