package com.katsuo.uqacpark.models;

public class Parking {
    private String parkingId;
    private String name;
    private int nbSpotsAvailable;
    private double latitude;
    private double longitude;

    public Parking() {
    }

    public Parking(String parkingId, String name, int nbSpotsAvailable, double latitude, double longitude) {
        this.parkingId = parkingId;
        this.name = name;
        this.nbSpotsAvailable = nbSpotsAvailable;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getParkingId() {
        return parkingId;
    }

    public void setParkingId(String parkingId) {
        this.parkingId = parkingId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNbSpotsAvailable() {
        return nbSpotsAvailable;
    }

    public void setNbSpotsAvailable(int nbSpotsAvailable) {
        this.nbSpotsAvailable = nbSpotsAvailable;
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
}
