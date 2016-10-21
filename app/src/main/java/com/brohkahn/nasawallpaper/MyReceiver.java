package com.brohkahn.nasawallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, ChangeWallpaperService.class);
        context.stopService(serviceIntent);
        context.startService(serviceIntent);
    }
}
