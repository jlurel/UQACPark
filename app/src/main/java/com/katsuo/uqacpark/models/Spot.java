package com.katsuo.uqacpark.models;

public class Spot {
    private String spotId;
    private Parking parking;
    private double latitude;
    private double longitude;
    private boolean available;

    public Spot() {
    }

    public Spot(String spotId, Parking parking, double latitude, double longitude, boolean available) {
        this.spotId = spotId;
        this.parking = parking;
        this.latitude = latitude;
        this.longitude = longitude;
        this.available = available;
    }

    public String getSpotId() {
        return spotId;
    }

    public void setSpotId(String spotId) {
        this.spotId = spotId;
    }

    public Parking getParking() {
        return parking;
    }

    public void setParking(Parking parking) {
        this.parking = parking;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
