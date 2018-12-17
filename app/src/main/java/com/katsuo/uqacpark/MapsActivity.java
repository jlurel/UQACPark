package com.katsuo.uqacpark;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.katsuo.uqacpark.dao.ParkingDAO;
import com.katsuo.uqacpark.dao.ReservationDAO;
import com.katsuo.uqacpark.dao.SpotDAO;
import com.katsuo.uqacpark.dao.UserDAO;
import com.katsuo.uqacpark.models.Parking;
import com.katsuo.uqacpark.models.Reservation;
import com.katsuo.uqacpark.models.Spot;
import com.katsuo.uqacpark.profile.ProfileActivity;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    @BindView(R.id.main_activity_coordinator_layout)
    CoordinatorLayout coordinatorLayout;
    @BindView(R.id.fab)
    FloatingActionButton fabExpand;
    @BindView(R.id.fab_collapse)
    FloatingActionButton fabCollapse;
    @BindView(R.id.bottom_sheet)
    LinearLayout bottomSheet;
    @BindView(R.id.button_reservations)
    Button buttonReservations;
    @BindView(R.id.button_login)
    Button buttonLogin;
    @BindView(R.id.button_email)
    Button buttonEmail;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = MapsActivity.class.getSimpleName();
    //Identifiant pour la Sign-in Activity
    private static final int RC_SIGN_IN = 123;

    //Authentication providers
    List<AuthUI.IdpConfig> providers = Arrays.asList(
            new AuthUI.IdpConfig.EmailBuilder().build(),
            new AuthUI.IdpConfig.GoogleBuilder().build(),
            new AuthUI.IdpConfig.FacebookBuilder().build(),
            new AuthUI.IdpConfig.PhoneBuilder().build(),
            new AuthUI.IdpConfig.AnonymousBuilder().build()
    );

    private GoogleMap mMap;
    private boolean mPermissionDenied = false;
    private int color = Color.GREEN;
    private Map<String, Spot> mSpotMap = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.drawable.ic_parking_white);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        ButterKnife.bind(this);

        scheduleAlarm();

        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setPeekHeight(50);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED: {
                        fabCollapse.show();
                        fabCollapse.animate()
                                .scaleX(1)
                                .scaleY(1)
                                .setDuration(0)
                                .start();

                        fabExpand.hide();
                    }
                    break;
                    case BottomSheetBehavior.STATE_COLLAPSED: {
                        fabExpand.show();
                        fabExpand.animate()
                                .scaleX(1)
                                .scaleY(1)
                                .setDuration(0)
                                .start();
                        fabCollapse.hide();

                    }
                    break;
                    case BottomSheetBehavior.STATE_DRAGGING: {
                        fabExpand.animate()
                                .scaleX(0)
                                .scaleY(0)
                                .setDuration(0)
                                .start();
                        fabCollapse.animate()
                                .scaleX(0)
                                .scaleY(0)
                                .setDuration(0)
                                .start();
                    }
                    break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });

        fabExpand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        fabCollapse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        this.updateUIWhenResuming();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) { // Successfully signed in
                this.createUserInFirestore();
                showSnackBar(this.coordinatorLayout, getString(R.string.connection_succeed));
            } else { // ERRORS
                if (response == null) {
                    showSnackBar(this.coordinatorLayout, getString(R.string.error_authentication_canceled));
                } else if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                    showSnackBar(this.coordinatorLayout, getString(R.string.error_no_internet));
                } else if (response.getError().getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                    showSnackBar(this.coordinatorLayout, getString(R.string.error_unknown_error));
                }
            }
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near uqac, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        enableMyLocation();

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style_json));

            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }

        // Add a marker in uqac and move the camera
        LatLng uqac = new LatLng(48.420535, -71.052661);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(uqac));
        mMap.setMinZoomPreference(15);
        mMap.setMaxZoomPreference(20);
//        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        addMarkersToMap(mMap);
        showReservationOnMap(mMap);

        mMap.setOnCircleClickListener(new GoogleMap.OnCircleClickListener() {
            @Override
            public void onCircleClick(Circle circle) {
                Spot spot = mSpotMap.get(circle.getId());
                Toast.makeText(getApplicationContext(), Double.toString(spot.getLatitude()) + "," + Double.toString(spot.getLongitude()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        addMarkersToMap(mMap);
    }


    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            Log.i(TAG,
                    "Location permissions have already been granted." +
                            " Displaying Google Maps.");
            mMap.setMyLocationEnabled(true);
        }
    }



    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                Log.i(TAG, "Received response for Location permission request.");
                if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // Enable the my location layer if the permission has been granted.
                    enableMyLocation();
                } else {
                    // Display the missing permission error dialog when the fragments resume.
                    mPermissionDenied = true;
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    private void addMarkersToMap(final GoogleMap googleMap) {
        ParkingDAO.getParkingCollection()
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }
                        googleMap.clear();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Parking parking = document.toObject(Parking.class);
                            final LatLng latLng = new LatLng(parking.getLatitude(), parking.getLongitude());
                            String name = parking.getName();
                            int nbSpotsAvailable = parking.getNbSpotsAvailable();
                            int nbSpotsTotal = parking.getNbSpotsTotal();
                            googleMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(name)
                                    .snippet(nbSpotsAvailable + " " + getString(R.string.ratio_spots_available) + nbSpotsTotal)
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_parking)));

                            addSpotsToMap(googleMap, parking.getParkingId());
                        }
                    }


                });
    }

    private void addSpotsToMap(final GoogleMap googleMap, final String parkingId) {
        SpotDAO.getAllSpotsForParking(parkingId)
            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots,
                                    @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }
                    int nbSpotsAvailable = 0;
                    int nbSpotsTotal = 0;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Spot spot = document.toObject(Spot.class);
                        LatLng latLng1 = new LatLng(spot.getLatitude(), spot.getLongitude());
                        if (!spot.isAvailable()) {
                            color = Color.RED;
                            nbSpotsTotal += 1;
                        } else {
                            color = Color.GREEN;
                            nbSpotsTotal += 1;
                            nbSpotsAvailable += 1;
                        }

                        Circle circle = googleMap.addCircle(new CircleOptions()
                                .center(latLng1)
                                .radius(1.1)
                                .strokeColor(color)
                                .fillColor(color)
                                .clickable(true));
                        mSpotMap.put(circle.getId(), spot);
                    }
                    ParkingDAO.updateNbSpotsAvailable(parkingId, nbSpotsAvailable);
                    ParkingDAO.updateNbSpotsTotal(parkingId, nbSpotsTotal);
                }
            });
    }

    private void showReservationOnMap(final GoogleMap googleMap) {
        if (!isCurrentUserLogged()) {
            return;
        }
        final Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();
        String todayToString = DateFormat.format("dd/MM/yyyy", today).toString();
        ReservationDAO.getNextReservationForUser(this.getCurrentUser().getUid(), todayToString).addSnapshotListener(
                new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }
                        if (queryDocumentSnapshots.getDocuments().size() == 0) {
                            return;
                        }
                        Reservation reservation = queryDocumentSnapshots
                                .getDocuments().get(0).toObject(Reservation.class);
                        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
                        int currentMinutes = calendar.get(Calendar.MINUTE);
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
                        String currentTimeToString = String.format("%02d:%02d", currentHour, currentMinutes);
                        try {
                            Date currentTime = simpleDateFormat.parse(currentTimeToString);
                            Date reservationEndTime = simpleDateFormat.parse(reservation.getEndHour());
                            if (currentTime.before(reservationEndTime)) {
                                String spotId = reservation.getSpotId();
                                String parkingId = "XjCkiiACt3GLnNlEjRds";
                                SpotDAO.getSpot(parkingId, spotId).addSnapshotListener(new EventListener<QuerySnapshot>() {
                                    @Override
                                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots,
                                                        @Nullable FirebaseFirestoreException e) {
                                        if (e != null) {
                                            Log.w(TAG, "Listen failed.", e);
                                            return;
                                        }
                                        if (queryDocumentSnapshots.getDocuments().size() == 0) {
                                            return;
                                        }
                                        Spot spot = queryDocumentSnapshots
                                                .getDocuments().get(0).toObject(Spot.class);
                                        LatLng latLng = new LatLng(spot.getLatitude(), spot.getLongitude());
                                        googleMap.addMarker(new MarkerOptions()
                                                .position(latLng)
                                                .title("Ma place réservée"));
                                    }
                                });
                            }
                        } catch (ParseException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
        );

    }

    private void updateUIWhenResuming(){
        this.buttonLogin.setText(this.isCurrentUserLogged() ?
                getString(R.string.button_login_text_logged) : getString(R.string.button_login_text_not_logged));
        this.buttonReservations.setVisibility(this.isCurrentUserLogged() ?
                View.VISIBLE : View.GONE);
    }

    @OnClick(R.id.button_login)
    public void onClickButtonLogin() {
        if (this.isCurrentUserLogged()){
            this.startProfileActivity();
        } else {
            this.startSignInActivity();
        }
    }

    @OnClick(R.id.button_reservations)
    public void onClickButtonReservations() {
        this.startReservationActivity();
    }

    @OnClick(R.id.button_Payment)
    public void onClickButtonPayment() {
        Paiement();
    }

    @OnClick(R.id.button_email)
    public void onClickButtonEmail() {
        startEmailActivity();
    }

    @Nullable
    protected FirebaseUser getCurrentUser(){ return FirebaseAuth.getInstance().getCurrentUser(); }

    protected Boolean isCurrentUserLogged(){ return (this.getCurrentUser() != null); }

    private void startSignInActivity() {
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .enableAnonymousUsersAutoUpgrade()
                        .setTheme(R.style.LoginTheme)
                        .setAvailableProviders(providers)
                        .setIsSmartLockEnabled(false, true)
                        .setLogo(R.drawable.parking)
                        .build(),
                RC_SIGN_IN
        );
    }

    private void startProfileActivity(){
        Intent intent = new Intent(this, ProfileActivity.class);
        startActivity(intent);
    }

    private void Paiement(){
        TextView m_response;

        PayPalConfiguration m_configuration;
        String m_paypalClientId="AcWNG1uTnKBgeex8lr8sT6eOmGKI19xGCHcflJiTlG04FvDPN46Wd89ci_hiy3f6DlWf3P8v-9HI__cB";
        Intent m_service;
        int m_paypalrequestcode = 999;

        m_configuration = new PayPalConfiguration().environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)
                .clientId(m_paypalClientId);

        m_service=new Intent(this, PayPalService.class);
        m_service.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, m_configuration);
        startService(m_service);

        PayPalPayment payment = new PayPalPayment(new
                java.math.BigDecimal(103),"CAD", "Test payment with paypal",
                PayPalPayment.PAYMENT_INTENT_SALE);
        startPaiementActivity(m_configuration,m_paypalrequestcode,payment );

    }

    private void startPaiementActivity(PayPalConfiguration m_configuration,int m_paypalrequestcode,PayPalPayment pay ){
        Intent intent = new Intent(this,PaymentActivity.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION,m_configuration);
        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, pay);
        startActivity(intent);
    }

    private void startEmailActivity() {
        Intent intent = new Intent(this, EmailActivity.class);
        startActivity(intent);
    }

    private void startReservationActivity() {
        Intent intent = new Intent(this, ReservationActivity.class);
        startActivity(intent);
    }

    private void showSnackBar(CoordinatorLayout coordinatorLayout, String message){
        Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_SHORT).show();
    }

    private void createUserInFirestore(){

        if (this.getCurrentUser() != null){

            String urlPicture = (this.getCurrentUser().getPhotoUrl() != null) ? this.getCurrentUser().getPhotoUrl().toString() : null;
            String username = this.getCurrentUser().getDisplayName();
            String userId = this.getCurrentUser().getUid();

            UserDAO.createUser(userId, username, urlPicture, null).addOnFailureListener(this.onFailureListener());
        }
    }

    private OnFailureListener onFailureListener() {
        return new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), getString(R.string.error_unknown_error), Toast.LENGTH_LONG).show();
            }
        };
    }

    public void scheduleAlarm() {
        Intent intent = new Intent(getApplicationContext(), MyAlarmReceiver.class);
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, MyAlarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long firstMillis = System.currentTimeMillis(); // alarm is set right away
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        long interval = 1* 60 * 1000;
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, firstMillis,
                interval, pIntent);
        Log.i(TAG, "Schedule Alarm");
    }
}