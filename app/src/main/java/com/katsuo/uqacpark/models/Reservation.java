package com.katsuo.uqacpark.models;

import java.util.Date;

public class Reservation {
    private String reservationId;
    private Date startDate;
    private Date endDate;
    private String status;
    private User user;
    private Spot spot;

    public Reservation() {
    }

    public Reservation(String reservationId, Date startDate, Date endDate, String status, User user, Spot spot) {
        this.reservationId = reservationId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.user = user;
        this.spot = spot;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Spot getSpot() {
        return spot;
    }

    public void setSpot(Spot spot) {
        this.spot = spot;
    }
}
