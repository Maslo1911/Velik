package ru.mirea.velik.exception;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entityName, int id) {
        super(entityName + " с id=" + id + " не найден(а)");
    }
}
