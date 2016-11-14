package com.brohkahn.rsswallpaper;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

public class ChangeWallpaperReceiver extends WakefulBroadcastReceiver {
	public static final String TAG = "ChangeWallpaperReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent != null && intent.getAction().equals(Constants.ACTION_CHANGE_WALLPAPER)) {
			LogDBHelper helper = LogDBHelper.getHelper(context);
			helper.saveLogEntry("Received " + Constants.ACTION_CHANGE_WALLPAPER + " broadcast.",
					null,
					"onReceive",
					TAG,
					LogEntry.LogLevel.Trace);
			helper.close();

			Intent newIntent = new Intent(context, ChangeWallpaperService.class);
			newIntent.setAction(Constants.ACTION_CHANGE_WALLPAPER);
			newIntent.putExtra(ChangeWallpaperService.KEY_INTENT_SOURCE, TAG);
			startWakefulService(context, newIntent);
		}
	}
}
