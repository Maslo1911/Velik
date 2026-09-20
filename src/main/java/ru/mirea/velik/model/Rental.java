package ru.mirea.velik.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Rental {

    private int id;
    private int clientId;
    private int bikeId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;      // может быть null, пока аренда не завершена
    private RentalStatus status;
    private double totalCost;           // рассчитывается при завершении аренды

    public Rental() {
    }

    public Rental(int clientId, int bikeId, LocalDateTime startTime, RentalStatus status) {
        this.clientId = clientId;
        this.bikeId = bikeId;
        this.startTime = startTime;
        this.status = status;
    }

    public Rental(int id, int clientId, int bikeId, LocalDateTime startTime,
                  LocalDateTime endTime, RentalStatus status, double totalCost) {
        this.id = id;
        this.clientId = clientId;
        this.bikeId = bikeId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.totalCost = totalCost;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public int getBikeId() {
        return bikeId;
    }

    public void setBikeId(int bikeId) {
        this.bikeId = bikeId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public RentalStatus getStatus() {
        return status;
    }

    public void setStatus(RentalStatus status) {
        this.status = status;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rental)) return false;
        Rental rental = (Rental) o;
        return id == rental.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Rental{" +
                "id=" + id +
                ", clientId=" + clientId +
                ", bikeId=" + bikeId +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", status=" + status +
                ", totalCost=" + totalCost +
                '}';
    }
}