package ru.mirea.velik.fx.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import ru.mirea.velik.fx.ViewLoader;

/**
 * Стандартные диалоговые окна: ошибка, предупреждение, информация, подтверждение.
 */
public final class Alerts {

    private Alerts() {
    }

    public static void error(String header, String message) {
        show(Alert.AlertType.ERROR, "Ошибка", header, message);
    }

    public static void warning(String message) {
        show(Alert.AlertType.WARNING, "Внимание", "Операция не выполнена", message);
    }

    public static void info(String header, String message) {
        show(Alert.AlertType.INFORMATION, "Готово", header, message);
    }

    /** @return true, если пользователь подтвердил действие */
    public static boolean confirm(String header, String message, String confirmText) {
        ButtonType confirm = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, confirm, cancel);
        prepare(alert, "Подтверждение", header);
        return alert.showAndWait().filter(button -> button == confirm).isPresent();
    }

    private static void show(Alert.AlertType type, String title, String header, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        prepare(alert, title, header);
        alert.showAndWait();
    }

    private static void prepare(Alert alert, String title, String header) {
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.getDialogPane().getStylesheets().add(ViewLoader.stylesheet());
        // длинные сообщения переносятся по строкам, а не обрезаются
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
    }
}
