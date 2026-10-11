package ru.mirea.velik.model;

/**
 * Статус заявки на аренду велосипеда.
 */
public enum RentalStatus {
    CREATED("Создана"),       // создана, ожидает выдачи
    ACTIVE("Активна"),        // велосипед выдан, аренда идёт
    COMPLETED("Завершена"),   // аренда успешно завершена
    CANCELLED("Отменена");    // заявка отменена

    private final String title;

    RentalStatus(String title) {
        this.title = title;
    }

    /** Название статуса для отображения пользователю. */
    public String getTitle() {
        return title;
    }
}
