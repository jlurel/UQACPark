package com.katsuo.uqacpark;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.katsuo.uqacpark.base.BaseActivity;
import com.katsuo.uqacpark.dao.ParkingDAO;
import com.katsuo.uqacpark.dao.ReservationDAO;
import com.katsuo.uqacpark.dao.SpotDAO;
import com.katsuo.uqacpark.models.Reservation;
import com.katsuo.uqacpark.models.Spot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.OnClick;

public class ReservationActivity extends BaseActivity
    implements ActivityCompat.OnRequestPermissionsResultCallback{

    @BindView(R.id.edit_reservation_start_date)
    EditText editTextReservationStartDate;

    @BindView(R.id.edit_reservation_end_date)
    EditText editTextReservationEndDate;

    @BindView(R.id.coordinatorLayout)
    CoordinatorLayout coordinatorLayout;

    @BindView(R.id.text_view_duree_reservation)
    TextView textViewDureeReservation;

    @BindView(R.id.recyclerView)
    RecyclerView recyclerViewReservations;

    private static final int EDIT_START_DATE = 1;
    private static final int EDIT_END_DATE = 2;

    private static final int WRITE_CALENDAR_PERMISSION_REQUEST_CODE = 2;

    private static final String TAG = ReservationActivity.class.getSimpleName();

    private final String userId = getCurrentUser().getUid();
    private boolean mPermissionDenied = false;

    private TimePickerDialog timePickerDialog;
    private Calendar calendar;
    private int currentHour;
    private int currentMinute;
    private int mMinStartHour;
    private int mMinStartMinute;
    private Date currentDay;
    private List<Spot> reservableSpots = new ArrayList<>();

    private FirestoreRecyclerAdapter adapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.configureToolbar();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reserverSpot();
            }
        });

        requestCalendarPermission();

        calendar = Calendar.getInstance();
        currentDay = calendar.getTime();
        currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        currentMinute = calendar.get(Calendar.MINUTE);
        mMinStartHour = currentHour + 1;
        mMinStartMinute = currentMinute + 30;

        mLayoutManager = new LinearLayoutManager(this);
        recyclerViewReservations.setLayoutManager(mLayoutManager);

        FirestoreRecyclerOptions<Reservation> options =
                new FirestoreRecyclerOptions.Builder<Reservation>()
                        .setQuery(ReservationDAO.getAllReservationsForUser(userId), Reservation.class)
                        .build();

        adapter = new FirestoreRecyclerAdapter<Reservation, ReservationViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ReservationViewHolder holder, int position, @NonNull Reservation model) {
                holder.bind(model);
            }

            @Override
            public ReservationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.reservation_card, parent, false);
                return new ReservationViewHolder(view);
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
        recyclerViewReservations.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
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
            resultCode = 0;
        } else if ((hourOfDay < 0) || (hourOfDay > 24)) {
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
        reservableSpots.clear();
        ParkingDAO.getParkingByName("Parking Ouest")
            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                List<String> parkings = new ArrayList<>();
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    parkings.add(documentSnapshot.getId());
                }
                Log.d(TAG, parkings.toString());
                final String parking = parkings.get(0);
                SpotDAO.getAllAvailableSpotsForParking(parking)
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots,
                                            @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }
                        if (queryDocumentSnapshots != null) {
                            for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                Spot spot = documentSnapshot.toObject(Spot.class);
                                Log.i(TAG, "Spot : " + spot.getSpotId());
                                reservableSpots.add(spot);
                            }
                        }
                        }
                    });
                Long currentTime = System.currentTimeMillis() / 1000;
                String timestamp = currentTime.toString();
                final String reservationId = userId + timestamp;
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
                final String today = simpleDateFormat.format(currentDay);
                final String startHour = editTextReservationStartDate.getText().toString();
                final String endHour = editTextReservationEndDate.getText().toString();
                ReservationDAO.getAllReservationsForDate(today)
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots,
                                            @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }
                        if (queryDocumentSnapshots != null) {
                            for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                Reservation reservation = documentSnapshot.toObject(Reservation.class);
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
                                String reservationUserId = reservation.getUser();
                                try {
                                    Date startHourFromString = simpleDateFormat.parse(startHour);
                                    Date endHourFromString = simpleDateFormat.parse(endHour);
                                    Date reservationStartHour = simpleDateFormat.parse(reservation.getStartHour());
                                    Date reservationEndHour = simpleDateFormat.parse(reservation.getEndHour());

                                    if (userId.equals(reservationUserId)
                                            && startHourFromString.compareTo(reservationStartHour) == 0
                                            && endHourFromString.compareTo(reservationEndHour) == 0) {
                                        Snackbar snackbar = Snackbar
                                                .make(coordinatorLayout,
                                                        "Vous avez déjà une réservation pour cette horaire !",
                                                        Snackbar.LENGTH_LONG);
                                        snackbar.setActionTextColor(Color.RED);
                                        snackbar.show();
                                        return;
                                    }

                                    if ((startHourFromString.after(reservationStartHour) || startHourFromString.compareTo(reservationStartHour) == 0)
                                            && (endHourFromString.before(reservationEndHour) || endHourFromString.compareTo(reservationEndHour) == 0)) {
                                        SpotDAO.getSpot(parking, reservation.getSpotId())
                                                .addSnapshotListener(
                                            new EventListener<QuerySnapshot>() {
                                                @Override
                                                public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots,
                                                                    @Nullable FirebaseFirestoreException e) {
                                                    Log.i(TAG, "Taille reservableSpots : " + reservableSpots.size());
                                                    if (queryDocumentSnapshots != null) {
                                                        Spot spot1 = queryDocumentSnapshots.getDocuments()
                                                                .get(0).toObject(Spot.class);
                                                        for (int i = 0; i < reservableSpots.size(); i++) {
                                                            Spot spot = reservableSpots.get(i);
                                                            String spotId = spot.getSpotId();
                                                            if (spotId.equals(spot1.getSpotId())) {
                                                                reservableSpots.remove(spot);
                                                            }
                                                        }
                                                    }

                                                }
                                            });
                                    }
                                } catch (ParseException pe) {
                                    pe.printStackTrace();
                                }

                            }
                            if (reservableSpots.isEmpty()) {
                                Snackbar snackbar = Snackbar
                                        .make(coordinatorLayout,
                                                "Aucune place disponible !",
                                                Snackbar.LENGTH_LONG);
                                snackbar.setActionTextColor(Color.RED);
                                snackbar.show();
                            } else {
                                Spot spot = reservableSpots.get(0);
                                ReservationDAO.createReservation(reservationId, userId, spot.getSpotId(),
                                        today, startHour, endHour, "à venir");
                                Snackbar snackbar = Snackbar
                                        .make(coordinatorLayout,
                                                "Votre réservation a bien été enregistrée !",
                                                Snackbar.LENGTH_LONG);
                                snackbar.setActionTextColor(Color.RED);
                                snackbar.show();

                                addEventToCalendar(startHour, endHour);

                                String notificationTitle = "Nouvelle réservation !";
                                String notificationText = "Votre réservation a bien été enregistrée !";
                                showNotification(notificationTitle, notificationText);
                                return;
                            }
                        }
                        }
                    });
                }
            });
    }

    private void showNotification(String title, String content) {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("default",
                    "UQAC & Park",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("UQAC & Park");
            mNotificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext(), "default")
                    .setSmallIcon(R.mipmap.ic_launcher) // notification icon
                    .setContentTitle(title) // title for notification
                    .setContentText(content)// message for notification
                    .setAutoCancel(true) // clear notification after click
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        mNotificationManager.notify(0, mBuilder.build());
    }

    private void addEventToCalendar(String reservationStartTime, String reservationEndTime) {
        List<String> start = Arrays.asList(reservationStartTime.split(":"));
        List<String> end = Arrays.asList(reservationEndTime.split(":"));
        int startHour = Integer.valueOf(start.get(0));
        int startMinutes = Integer.valueOf(start.get(1));

        int endHour = Integer.valueOf(end.get(0));
        int endMinutes = Integer.valueOf(end.get(1));

        long calID = 3;
        long startMillis = 0;
        long endMillis = 0;
        Calendar beginTime = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int date = calendar.get(Calendar.DATE);
        beginTime.set(year, month, date, startHour, startMinutes);
        startMillis = beginTime.getTimeInMillis();
        Calendar endTime = Calendar.getInstance();
        endTime.set(year, month, date, endHour, endMinutes);
        endMillis = endTime.getTimeInMillis();

        TimeZone timeZone = TimeZone.getDefault();

        ContentResolver cr = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(Events.DTSTART, startMillis);
        values.put(Events.DTEND, endMillis);
        values.put(Events.TITLE, "UQAC & Park");
        values.put(Events.DESCRIPTION, "Ma réservation");
        values.put(Events.CALENDAR_ID, calID);
        values.put(Events.EVENT_TIMEZONE, timeZone.getID());
        Uri uri = cr.insert(Events.CONTENT_URI, values);

        // get the event ID that is the last element in the Uri
        long eventID = Long.parseLong(uri.getLastPathSegment());
        values = new ContentValues();
        values.put(Reminders.MINUTES, 30);
        values.put(Reminders.EVENT_ID, eventID);
        values.put(Reminders.METHOD, Reminders.METHOD_ALERT);
        cr.insert(Reminders.CONTENT_URI, values);
    }

    private void requestCalendarPermission() {
        Log.i(TAG, "Calendar permission has not been granted. Requesting permission");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            PermissionUtils.requestPermission(this, WRITE_CALENDAR_PERMISSION_REQUEST_CODE,
                    Manifest.permission.WRITE_CALENDAR, true);
        } else {
            Log.i(TAG,
                    "Calendar permissions have already been granted.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case WRITE_CALENDAR_PERMISSION_REQUEST_CODE:
                Log.i(TAG, "Received response for Calendar permission request.");
                if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                        Manifest.permission.WRITE_CALENDAR)) {
                    Snackbar.make(coordinatorLayout, "La permission d'écrire dans le calendrier a été accordée.",
                            Snackbar.LENGTH_SHORT)
                            .show();
                } else {
                    mPermissionDenied = true;
                }
                break;
            default:
                break;
        }
    }

    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

}