package com.katsuo.uqacpark;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyAlarmReceiver extends BroadcastReceiver {
    public static final int REQUEST_CODE = 12345;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(MyIntentService.TAG, "My intent receiver");
        Intent i = new Intent(context, MyIntentService.class);
        MyIntentService.enqueueWork(context, i);
    }
}
