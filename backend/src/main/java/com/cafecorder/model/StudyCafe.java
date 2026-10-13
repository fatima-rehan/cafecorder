package com.cafecorder.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

// A cafe visited to study. Still "is a" Cafe, stored as a cafe_visits row with visit_type = 'STUDY'.
@Entity
@DiscriminatorValue("STUDY")
public class StudyCafe extends Cafe {

    @Column(name = "has_wifi")
    private Boolean hasWifi;

    @Column(name = "has_outlets")
    private Boolean hasOutlets;

    // STRING stores "GOOD", not 0. Reordering the enum later can't silently change old rows.
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Seating seating;

    @Column(name = "laptop_policy", length = 100)
    private String laptopPolicy;

    @Enumerated(EnumType.STRING)
    @Column(name = "noise_level", length = 10)
    private NoiseLevel noiseLevel;

    protected StudyCafe() {
        // JPA needs a no-arg constructor
    }

    public StudyCafe(String name, String neighbourhood, LocalDate visitDate, String order, BigDecimal price, int rating,
                     boolean hasWifi, boolean hasOutlets, Seating seating, String laptopPolicy, NoiseLevel noiseLevel) {
        super(name, neighbourhood, visitDate, order, price, rating);
        this.hasWifi = hasWifi;
        this.hasOutlets = hasOutlets;
        this.seating = seating;
        this.laptopPolicy = laptopPolicy;
        this.noiseLevel = noiseLevel;
    }

    @Override
    public boolean isStudySpot() {
        return true;
    }

    public Boolean getHasWifi() { return hasWifi; }
    public Boolean getHasOutlets() { return hasOutlets; }
    public Seating getSeating() { return seating; }
    public String getLaptopPolicy() { return laptopPolicy; }
    public NoiseLevel getNoiseLevel() { return noiseLevel; }

    public void setHasWifi(Boolean hasWifi) { this.hasWifi = hasWifi; }
    public void setHasOutlets(Boolean hasOutlets) { this.hasOutlets = hasOutlets; }
    public void setSeating(Seating seating) { this.seating = seating; }
    public void setLaptopPolicy(String laptopPolicy) { this.laptopPolicy = laptopPolicy; }
    public void setNoiseLevel(NoiseLevel noiseLevel) { this.noiseLevel = noiseLevel; }

    @Override
    public String toString() {
        return super.toString() + " [Study - Wifi: " + yesNo(hasWifi) + ", Outlets: " + yesNo(hasOutlets)
                + ", Seating: " + seating + ", Laptops: " + laptopPolicy + ", Noise: " + noiseLevel + "]";
    }

    private static String yesNo(Boolean b) {
        return Boolean.TRUE.equals(b) ? "Yes" : "No";
    }
}
