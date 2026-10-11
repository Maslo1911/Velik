package ru.mirea.velik.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.velik.exception.BusinessException;
import ru.mirea.velik.exception.DatabaseException;
import ru.mirea.velik.exception.EntityNotFoundException;
import ru.mirea.velik.exception.ValidationException;

import java.util.Map;

/**
 * Общая часть форм добавления и редактирования: сохранение, показ ошибки
 * и подсветка поля, заполненного неверно.
 */
public abstract class FormController {

    private static final Logger log = LoggerFactory.getLogger(FormController.class);
    private static final String ERROR_STYLE = "field-error";

    @FXML
    protected Label errorLabel;

    /** Передаёт данные формы в сервис; при ошибке сервис выбрасывает исключение. */
    protected abstract void save();

    /** Соответствие «имя поля из ValidationException → элемент управления». */
    protected abstract Map<String, Control> fields();

    /** @return true, если данные сохранены и форму можно закрыть */
    public final boolean submit() {
        clearErrors();
        try {
            save();
            return true;
        } catch (ValidationException e) {
            Control control = fields().get(e.getField());
            if (control != null) {
                control.getStyleClass().add(ERROR_STYLE);
                control.requestFocus();
            }
            showError(e.getMessage());
        } catch (BusinessException | EntityNotFoundException e) {
            showError(e.getMessage());
        } catch (DatabaseException e) {
            log.error("Ошибка сохранения формы", e);
            showError("Не удалось сохранить данные. " + e.getMessage());
        }
        return false;
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearErrors() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        fields().values().forEach(control -> control.getStyleClass().remove(ERROR_STYLE));
    }
}
