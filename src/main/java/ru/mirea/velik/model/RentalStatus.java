package ru.mirea.velik.model;

/**
 * Статус заявки на аренду велосипеда.
 */
public enum RentalStatus {
    CREATED,    // создана, ожидает выдачи
    ACTIVE,     // велосипед выдан, аренда идёт
    COMPLETED,  // аренда успешно завершена
    CANCELLED   // заявка отменена
}
