package com.brohkahn.rsswallpaper;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

public class ChangeWallpaperReceiver extends WakefulBroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent != null && intent.getAction().equals(Constants.ACTION_CHANGE_WALLPAPER)) {
			Intent newIntent = new Intent(context, ChangeWallpaperService.class);
			newIntent.setAction(Constants.ACTION_CHANGE_WALLPAPER);
			startWakefulService(context, newIntent);
		}
	}
}
