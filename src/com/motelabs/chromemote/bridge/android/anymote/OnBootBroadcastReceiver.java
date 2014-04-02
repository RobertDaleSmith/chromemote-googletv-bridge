package com.motelabs.chromemote.bridge.android.anymote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.motelabs.chromemote.bridge.BackgroundService;

public class OnBootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Intent backgroundServiceIntent = null;
        backgroundServiceIntent = new Intent(context, BackgroundService.class);
        SharedPreferences prefs = context.getSharedPreferences("com.chromemote.android.anymote", Context.MODE_PRIVATE);
        boolean isActive = prefs.getBoolean("com.chromemote.android.anymote.active", true);

        if(isActive) context.startService(backgroundServiceIntent);

    }
}