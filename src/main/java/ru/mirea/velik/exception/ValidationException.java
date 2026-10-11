package ru.mirea.velik.exception;

/**
 * Ошибка проверки пользовательского ввода: хранит имя поля, заполненного неверно.
 */
public class ValidationException extends BusinessException {

    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
