package com.katsuo.uqacpark;

import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.katsuo.uqacpark.base.BaseActivity;
import com.katsuo.uqacpark.dao.ParkingDAO;
import com.katsuo.uqacpark.dao.ReservationDAO;
import com.katsuo.uqacpark.dao.SpotDAO;
import com.katsuo.uqacpark.models.Spot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class ReservationActivity extends BaseActivity {

    @BindView(R.id.edit_reservation_start_date)
    EditText editTextReservationStartDate;

    @BindView(R.id.edit_reservation_end_date)
    EditText editTextReservationEndDate;

    @BindView(R.id.coordinatorLayout)
    CoordinatorLayout coordinatorLayout;

    @BindView(R.id.text_view_duree_reservation)
    TextView textViewDureeReservation;

    private static final int EDIT_START_DATE = 1;
    private static final int EDIT_END_DATE = 2;

    private static final String TAG = ReservationActivity.class.getSimpleName();

    TimePickerDialog timePickerDialog;
    Calendar calendar;
    int currentHour;
    int currentMinute;
    int mMinStartHour;
    int mMinStartMinute;
    Date currentDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.configureToolbar();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reserverSpot();
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        calendar = Calendar.getInstance();
        currentDay = calendar.getTime();
        currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        currentMinute = calendar.get(Calendar.MINUTE);
        mMinStartHour = currentHour + 1;
        mMinStartMinute = currentMinute + 30;
    }

    @Override
    public int getFragmentLayout() {
        return R.layout.activity_reservation;
    }

    @Nullable
    protected FirebaseUser getCurrentUser(){ return FirebaseAuth.getInstance().getCurrentUser(); }

    @OnClick(R.id.edit_reservation_start_date)
    public void onClickEditReservationStartDate() {
        timePickerDialog = new TimePickerDialog(ReservationActivity.this, android.R.style.Theme_Holo_Light_Dialog,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int hourOfDay, int minutes) {
                        int validTime = validateStartDate(hourOfDay, minutes);
                        switch (validTime) {
                            case 0 :
                                editTextReservationStartDate.setText(String.format("%02d:%02d", hourOfDay, minutes));
                                break;
                            case 1 :
                                timePickerDialog.updateTime(currentHour, mMinStartMinute);
                                editTextReservationStartDate.setText(String.format("%02d:%02d", mMinStartHour, currentMinute));
                                Snackbar snackbar1 = Snackbar
                                        .make(coordinatorLayout,
                                                "Vous ne pouvez réserver une place qu'entre 30 min et 1 heure à l'avance !",
                                                Snackbar.LENGTH_LONG);
                                snackbar1.setActionTextColor(Color.RED);
                                snackbar1.show();
                                break;
                            case 2 :
                                timePickerDialog.updateTime(currentHour, mMinStartMinute);
                                editTextReservationStartDate.setText(String.format("%02d:%02d", mMinStartHour, currentMinute));
                                Snackbar snackbar2 = Snackbar
                                        .make(coordinatorLayout,
                                                "Vous ne pouvez réserver une place qu'entre 7h et 22h !",
                                                Snackbar.LENGTH_LONG);
                                snackbar2.setActionTextColor(Color.RED);
                                snackbar2.show();
                                break;
                            default :
                                break;
                        }
                    }
                }, currentHour, mMinStartMinute, true);
        timePickerDialog.show();
    }


    @OnClick(R.id.edit_reservation_end_date)
    public void onClickEditReservationEndDate() {
        timePickerDialog = new TimePickerDialog(ReservationActivity.this, android.R.style.Theme_Holo_Light_Dialog,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(final TimePicker timePicker, int hourOfDay, int minutes) {
                        boolean validTime = validateEndDate(editTextReservationStartDate.getText().toString(),
                                String.format("%02d:%02d", hourOfDay, minutes));
                        if (validTime) {
                            timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                                @Override
                                public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                                    if (hourOfDay < 7) {
                                        view.setHour(7);
                                    }
                                    else if (hourOfDay > 22) {
                                        view.setHour(22);
                                    }
                                }
                            });
                            editTextReservationEndDate.setText(String.format("%02d:%02d", hourOfDay, minutes));
                            String dureeReservation = calculerDureeReservation(
                                    editTextReservationStartDate.getText().toString(),
                                    String.format("%02d:%02d", hourOfDay, minutes)
                            );
                            textViewDureeReservation.setText(String.valueOf(dureeReservation));
                        } else {
                            Snackbar snackbar = Snackbar
                                    .make(coordinatorLayout,
                                            "L'horaire sélectionnée n'est pas valide !",
                                            Snackbar.LENGTH_LONG);
                            snackbar.setActionTextColor(Color.RED);
                            snackbar.show();
                        }
                    }
                }, mMinStartHour + 1, mMinStartMinute, true);
        timePickerDialog.show();
    }

    private int validateStartDate(int hourOfDay, int minutes) {
        int resultCode = 0;
        if (((hourOfDay < currentHour ) || (hourOfDay == currentHour && minutes < mMinStartMinute))
                || ((hourOfDay > mMinStartHour))) {
            resultCode = 1;
        } else if ((hourOfDay < 7) || (hourOfDay > 22)) {
            resultCode = 2;
        }
        return resultCode;
    }

    private boolean validateEndDate(String startDate, String endDate) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        boolean validTime = false;
        try {
            Date startTime = simpleDateFormat.parse(startDate);
            Date endTime = simpleDateFormat.parse(endDate);
            Date maxTime = simpleDateFormat.parse(startDate + "04:00");

            if (endTime.before(maxTime) && endTime.after(startTime))
                validTime = true;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return validTime;
    }

    private String calculerDureeReservation(String startDate, String endDate) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        String dureeToString = null;
        try {
            Date startTime = simpleDateFormat.parse(startDate);
            Date endTime = simpleDateFormat.parse(endDate);
            Long dureeReservation = endTime.getTime() - startTime.getTime();

            long secondsInMilli = 1000;
            long minutesInMilli = secondsInMilli * 60;
            long hoursInMilli = minutesInMilli * 60;
            long daysInMilli = hoursInMilli * 24;

            long elapsedHours = dureeReservation / hoursInMilli;
            dureeReservation %= hoursInMilli;

            long elapsedMinutes = dureeReservation / minutesInMilli;
            dureeReservation %= minutesInMilli;

            dureeToString = String.format(" %d heure(s), %d minute(s)",
                    elapsedHours, elapsedMinutes);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return dureeToString;
    }

    public void reserverSpot() {
        ParkingDAO.getParkingByName("Parking Ouest")
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List<String> parkings = new ArrayList<>();
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            parkings.add(documentSnapshot.getId());
                        }
                        Log.d(TAG, parkings.toString());
                        String parking = parkings.get(0);

                        SpotDAO.getAllAvailableSpotsForParking(parking)
                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        Spot spot = task.getResult().getDocuments().get(0).toObject(Spot.class);
                                        Long currentTime = System.currentTimeMillis() / 1000;
                                        String timestamp = currentTime.toString();
                                        String userId = getCurrentUser().getUid();
                                        String reservationId = userId + timestamp;
                                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
                                        String today = simpleDateFormat.format(currentDay);
                                        String startHour = editTextReservationStartDate.getText().toString();
                                        String endHour = editTextReservationEndDate.getText().toString();
                                        ReservationDAO.createReservation(reservationId, userId, spot.getSpotId(),
                                                today, startHour, endHour, "en cours");
                                    }
                                    else {
                                        Log.d(TAG, "Error getting documents: ", task.getException());
                                    }

                                }
                            });
                    }
                });
    }

}