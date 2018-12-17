package com.katsuo.uqacpark;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.katsuo.uqacpark.dao.ReservationDAO;
import com.katsuo.uqacpark.models.Reservation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ReservationViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.text_view_reservation_card_date)
    TextView textViewReservationDate;

    @BindView(R.id.text_view_reservation_card_hour)
    TextView textViewReservationHour;

    @BindView(R.id.button_cancel_reservation)
    ImageButton buttonCancelReservation;

    private Calendar calendar = Calendar.getInstance();
    private Date currentDay = calendar.getTime();
    private int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
    private int currentMinutes = calendar.get(Calendar.MINUTE);

    public ReservationViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void bind(final Reservation reservation) {
        String currentTimeToString = String.format("%02d:%02d", currentHour, currentMinutes);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        try {
            Date currentTime = simpleDateFormat.parse(currentTimeToString);
            final Date reservationStartTime = simpleDateFormat.parse(reservation.getStartHour());
            final Date reservationEndTime = simpleDateFormat.parse(reservation.getEndHour());
            simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
            String today = simpleDateFormat.format(currentDay);
            if (reservation.getDate().equals(today)
                    && (currentTime.after(reservationStartTime) || currentTime.equals(reservationStartTime))
                    && (currentTime.before(reservationEndTime) || currentTime.equals(reservationEndTime))) {
                textViewReservationDate.setTypeface(null, Typeface.BOLD);
                textViewReservationDate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Context context = v.getContext();
                        Bundle extras = new Bundle();
                        extras.putString("start", reservation.getStartHour());
                        extras.putString("end", reservation.getEndHour());
                        Intent intent = new Intent(context, TimerActivity.class);
                        intent.putExtras(extras);
                        context.startActivity(intent);
                    }
                });
            }
            if (reservation.getDate().equals(today) && currentTime.before(reservationStartTime)) {
                buttonCancelReservation.setVisibility(View.VISIBLE);
                final String reservationId = reservation.getReservationId();
                buttonCancelReservation.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                        builder.setMessage("Voulez-vous vraiment annuler cette votre réservation ?")
                                .setTitle("Annuler la réservation");
                        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                ReservationDAO.deleteReservation(reservationId);
                                dialog.dismiss();
                            }
                        });
                        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                });
            } else {
                buttonCancelReservation.setVisibility(View.INVISIBLE);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        textViewReservationDate.setText(reservation.getDate());
        String reservationHour = reservation.getStartHour() + " - " + reservation.getEndHour();
        textViewReservationHour.setText(reservationHour);
    }

}
