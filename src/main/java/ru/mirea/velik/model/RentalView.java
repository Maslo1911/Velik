package ru.mirea.velik.model;

import java.time.LocalDateTime;

/**
 * Заявка на аренду вместе со связанными клиентом и велосипедом (для отображения в интерфейсе).
 */
public record RentalView(Rental rental, Client client, Bike bike) {

    public int id() {
        return rental.getId();
    }

    public String clientName() {
        return client != null ? client.getFullName() : "—";
    }

    public String clientPhone() {
        return client != null ? client.getPhone() : "—";
    }

    public String clientEmail() {
        return client != null && client.getEmail() != null && !client.getEmail().isBlank() ? client.getEmail() : "—";
    }

    public String bikeModel() {
        return bike != null ? bike.getModel() : "—";
    }

    public String bikeType() {
        return bike != null ? bike.getType() : "—";
    }

    public RentalStatus status() {
        return rental.getStatus();
    }

    public LocalDateTime startTime() {
        return rental.getStartTime();
    }

    public LocalDateTime endTime() {
        return rental.getEndTime();
    }

    public double totalCost() {
        return rental.getTotalCost();
    }
}
