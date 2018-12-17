package com.katsuo.uqacpark;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.katsuo.uqacpark.dao.ParkingDAO;
import com.katsuo.uqacpark.dao.ReservationDAO;
import com.katsuo.uqacpark.dao.SpotDAO;
import com.katsuo.uqacpark.models.Parking;
import com.katsuo.uqacpark.models.Reservation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class MyIntentService extends JobIntentService {
    public static final String TAG = MyIntentService.class.getSimpleName();
    private static final int JOB_ID = 1000;

    private Date today;
    private Calendar calendar;
    private int currentHour;
    private int currentMinutes;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, MyIntentService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.i(TAG, "Service running");
        handleAction();
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleAction() {
        calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
        today = calendar.getTime();
        final String todayToString = simpleDateFormat.format(today);
        ParkingDAO.getParkingByName("Parking Ouest")
            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Parking parking = documentSnapshot.toObject(Parking.class);
                        final String parkingId = parking.getParkingId();

                        ReservationDAO.getAllReservationsForDate(todayToString)
                            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                @Override
                                public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots,
                                                    @Nullable FirebaseFirestoreException e) {
                                if (queryDocumentSnapshots != null) {
                                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
                                    currentHour = calendar.get(Calendar.HOUR_OF_DAY);
                                    currentMinutes = calendar.get(Calendar.MINUTE);
                                    String currentTimeToString = String.format("%02d:%02d", currentHour, currentMinutes);
                                    try {
                                        Date currentTime = simpleDateFormat.parse(currentTimeToString);
                                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                            Reservation reservation = documentSnapshot.toObject(Reservation.class);
                                            Date reservationStartTime = simpleDateFormat.parse(reservation.getStartHour());
                                            Date reservationEndTime = simpleDateFormat.parse(reservation.getEndHour());
                                            final String spotId = reservation.getSpotId();
                                            Log.i(TAG, "Current time : " + currentTimeToString);
                                            Log.i(TAG, "Start time : " + reservation.getStartHour());
                                            if ((currentTime.after(reservationStartTime) || currentTime.compareTo(reservationStartTime) == 0)
                                                    && (currentTime.before(reservationEndTime ) || currentTime.compareTo(reservationEndTime) == 0)
                                                    ) {
                                                SpotDAO.updateIsAvailable(parkingId, spotId, false)
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                Log.i(TAG, "Update Spot " + spotId + ", available : false");
                                                            }
                                                        });
                                            } else {
                                                SpotDAO.updateIsAvailable(parkingId, spotId, true)
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                Log.i(TAG, "Update Spot " + spotId + ", available : true");
                                                            }
                                                        });
                                            }
                                        }
                                    } catch (ParseException pe) {
                                        pe.printStackTrace();
                                    }
                                }
                                }
                            });

                    }
                }
            });
    }
}
