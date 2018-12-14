package com.katsuo.uqacpark.dao;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.katsuo.uqacpark.models.Reservation;

import java.util.Date;

public class ReservationDAO {
    private static final String COLLECTION_NAME = "reservations";

    public static CollectionReference getReservationCollection() {
        return FirebaseFirestore.getInstance().collection(COLLECTION_NAME);
    }

    public static Query getAllReservationsForUser(String userId) {
        return ReservationDAO.getReservationCollection().whereEqualTo("user", userId)
                .limit(20);
    }

    public static Query getNextReservationForUser(String userId, String date) {
        return ReservationDAO.getReservationCollection().whereEqualTo("user", userId)
                .orderBy("startHour", Query.Direction.DESCENDING)
                .whereEqualTo("date", date).limit(1);
    }

    public static Query getAllReservationsForDate(String date, String startHour, String endHour) {
        return ReservationDAO.getReservationCollection()
                .whereEqualTo("date", date)
                .whereGreaterThanOrEqualTo("startHour", startHour)
                .whereLessThanOrEqualTo("endHour", endHour);
    }

    public static Task<Void> createReservation(String reservationId, String userId, String spotId,
                                        String date, String startHour, String endHour, String status) {
        Reservation reservation = new Reservation(reservationId, date, startHour, endHour, status, userId, spotId);
        return ReservationDAO.getReservationCollection().document(reservationId).set(reservation);
    }

    public static Task<Void> deleteReservation(String reservationId) {
        return ReservationDAO.getReservationCollection().document(reservationId).delete();
    }
}
