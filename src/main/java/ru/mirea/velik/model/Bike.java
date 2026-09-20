package ru.mirea.velik.model;

import java.util.Objects;

public class Bike {

    private int id;
    private String model;
    private String type;       // например: городской, горный, шоссейный
    private boolean available;
    private double pricePerHour;

    public Bike() {
    }

    public Bike(String model, String type, boolean available, double pricePerHour) {
        this.model = model;
        this.type = type;
        this.available = available;
        this.pricePerHour = pricePerHour;
    }

    public Bike(int id, String model, String type, boolean available, double pricePerHour) {
        this.id = id;
        this.model = model;
        this.type = type;
        this.available = available;
        this.pricePerHour = pricePerHour;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public double getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(double pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bike)) return false;
        Bike bike = (Bike) o;
        return id == bike.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Bike{" +
                "id=" + id +
                ", model='" + model + '\'' +
                ", type='" + type + '\'' +
                ", available=" + available +
                ", pricePerHour=" + pricePerHour +
                '}';
    }
}