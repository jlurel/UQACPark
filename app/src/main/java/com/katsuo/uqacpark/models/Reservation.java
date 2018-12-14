package com.katsuo.uqacpark.models;

import java.util.Date;

public class Reservation {
    private String reservationId;
    private String date;
    private String startHour;
    private String endHour;
    private String status;
    private String user;
    private String spotId;

    public Reservation() {
    }

    public Reservation(String reservationId, String date, String startHour, String endHour,
                       String status, String user, String spotId) {
        this.reservationId = reservationId;
        this.date = date;
        this.startHour = startHour;
        this.endHour = endHour;
        this.status = status;
        this.user = user;
        this.spotId = spotId;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartHour() {
        return startHour;
    }

    public void setStartHour(String startHour) {
        this.startHour = startHour;
    }

    public String getEndHour() {
        return endHour;
    }

    public void setEndHour(String endHour) {
        this.endHour = endHour;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getSpotId() {
        return spotId;
    }

    public void setSpotId(String spotId) {
        this.spotId = spotId;
    }
}
