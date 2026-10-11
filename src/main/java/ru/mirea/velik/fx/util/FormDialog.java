package ru.mirea.velik.fx.util;

import javafx.event.ActionEvent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;
import ru.mirea.velik.fx.AppContext;
import ru.mirea.velik.fx.ViewLoader;
import ru.mirea.velik.fx.controller.FormController;

import java.util.function.Consumer;

/**
 * Модальное окно с формой добавления или редактирования записи.
 */
public final class FormDialog {

    private FormDialog() {
    }

    /**
     * Показывает форму из FXML (корневой элемент — DialogPane).
     * Окно закрывается кнопкой «Сохранить», только если форма прошла проверку и данные записаны.
     *
     * @param init настройка контроллера формы перед показом (например, передача редактируемого объекта)
     * @return true, если данные сохранены
     */
    public static <C extends FormController> boolean show(Window owner, AppContext context, String fxmlName,
                                                          String title, Consumer<C> init) {
        ViewLoader.Loaded<DialogPane, C> loaded = context.viewLoader().loaded(fxmlName);
        DialogPane pane = loaded.root();
        init.accept(loaded.controller());

        ButtonType save = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().setAll(save, cancel);
        pane.getStylesheets().add(ViewLoader.stylesheet());
        pane.lookupButton(save).getStyleClass().add("button-primary");
        pane.lookupButton(save).addEventFilter(ActionEvent.ACTION, event -> {
            if (!loaded.controller().submit()) {
                event.consume(); // форма остаётся открытой, пока ошибка не исправлена
            }
        });

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setDialogPane(pane);
        return dialog.showAndWait().filter(button -> button == save).isPresent();
    }
}
