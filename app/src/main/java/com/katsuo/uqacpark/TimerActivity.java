package com.katsuo.uqacpark;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.TextView;

import com.katsuo.uqacpark.base.BaseActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;

public class TimerActivity extends BaseActivity {

    private static final long START_TIME_IN_MILLIS = 600000;
    private static final String TAG = TimerActivity.class.getSimpleName();

    @BindView(R.id.text_view_countdown)
    TextView mTextViewCountDown;

    private CountDownTimer mCountDownTimer;

    private long mTimeLeftInMillis = START_TIME_IN_MILLIS;

    private String endTime;
    private long milliseconds;
    private long diff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startTimer();
    }

    @Override
    public int getFragmentLayout() {
        return R.layout.activity_timer;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCountDownTimer.cancel();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCountDownTimer.cancel();
    }

    private void startTimer() {

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        endTime = extras.getString("end");
        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
        String todayToString = simpleDateFormat.format(today);
        simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Log.i(TAG, "endTime : " + endTime);
        try {
            Date date = simpleDateFormat.parse(todayToString + " " + endTime);
            Log.i(TAG, "date : " + date);
            long startMillis = calendar.getTimeInMillis();
            calendar.setTime(date);
            long endMillis = calendar.getTimeInMillis();
            milliseconds = endMillis - startMillis;
            mCountDownTimer = new CountDownTimer(milliseconds, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    long secondsInMilli = 1000;
                    long minutesInMilli = secondsInMilli * 60;
                    long hoursInMilli = minutesInMilli * 60;

                    long elapsedHours = millisUntilFinished / hoursInMilli;
                    millisUntilFinished = millisUntilFinished % hoursInMilli;

                    long elapsedMinutes = millisUntilFinished / minutesInMilli;
                    millisUntilFinished = millisUntilFinished % minutesInMilli;

                    long elapsedSeconds = millisUntilFinished / secondsInMilli;

                    String timeLeftFormatted = String.format("%02d:%02d", elapsedHours, elapsedMinutes);
                    Log.i(TAG, "minutesLeft : " + elapsedMinutes);
                    Log.i(TAG, "hoursLeft : " + elapsedHours);
                    mTextViewCountDown.setText(timeLeftFormatted);
                }

                @Override
                public void onFinish() {
                    String notificationTitle = "Fin de votre réservation !";
                    String notificationText = "Votre réservation est terminée, n'oubliez pas de déplacer votre véhicule !";
                    showNotification(notificationTitle, notificationText);
                    mTextViewCountDown.setText("00:00");
                }
            }.start();
        } catch (ParseException e) {
            e.printStackTrace();
        }
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
}
