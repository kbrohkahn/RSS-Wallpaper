package com.brohkahn.rsswallpaper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.util.Calendar;
import java.util.Locale;

public class MyBroadcastReceiver extends BroadcastReceiver {
	private static final String TAG = "MyBroadcastReceiver";

	private static final int MS_MINUTE = 1000 * 60;
	private static final int MS_HOUR = MS_MINUTE * 60;

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action.equals(Intent.ACTION_BOOT_COMPLETED)
				|| action.equals("com.brohkahn.rsswallpaper.SCHEDULE_ALARMS")) {
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			Resources resources = context.getResources();
			int rssUpdateInterval = Integer.parseInt(preferences.getString(resources.getString(R.string.key_update_interval), "24"));
			int rssUpdateTime = Integer.parseInt(preferences.getString(resources.getString(R.string.key_update_time), "3"));
			int changeWallpaperInterval = Integer.parseInt(preferences.getString(resources.getString(R.string.key_change_interval), "30"));


			// create intent and pending intent for ChangeWallpaperService
			Intent changeWallpaperIntent = new Intent(context, ChangeWallpaperService.class);
			changeWallpaperIntent.setAction(Constants.ACTION_CHANGE_WALLPAPER);

			// create pending intent, cancel (if already running), and reschedule
			PendingIntent wallpaperScheduleIntent = PendingIntent.getService(context, 0, changeWallpaperIntent, 0);
			AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			alarmManager.cancel(wallpaperScheduleIntent);
			alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, 0, changeWallpaperInterval * MS_MINUTE, wallpaperScheduleIntent);


			// set RSS update time
			Calendar downloadTime = Calendar.getInstance();
			if (downloadTime.get(Calendar.HOUR_OF_DAY) > rssUpdateTime) {
				// set to next day if we've already passed hour
				downloadTime.set(Calendar.DAY_OF_YEAR, downloadTime.get(Calendar.DAY_OF_YEAR) + 1);
			}
			downloadTime.set(Calendar.HOUR_OF_DAY, rssUpdateTime);
			downloadTime.set(Calendar.MINUTE, 0);
			downloadTime.set(Calendar.SECOND, 0);
			downloadTime.set(Calendar.MILLISECOND, 0);

			// create intent and pending intent for DownloadRSSService
			Intent downloadRSSIntent = new Intent(context, DownloadRSSService.class);
			downloadRSSIntent.setAction(Constants.ACTION_CHANGE_WALLPAPER);

			// create pending intent, cancel (if already running), and reschedule
			PendingIntent rssScheduleIntent = PendingIntent.getService(context, 0, downloadRSSIntent, 0);
			alarmManager.cancel(rssScheduleIntent);
			alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, downloadTime.getTimeInMillis(), rssUpdateInterval
					* MS_HOUR, rssScheduleIntent);


			String message = String.format(Locale.US,
					"Scheduled downloadRSSIntent every %s hours and changeWallpaperIntent every %d minutes",
					rssUpdateInterval,
					changeWallpaperInterval);
			Log.d(TAG, "onReceive(Context context, Intent intent): " + message);

			LogDBHelper helper = LogDBHelper.getHelper(context);
			helper.saveLogEntry(message, null, TAG, "onReceive(Context context, Intent intent)", LogEntry.LogLevel.Trace);
			helper.close();
		}
	}
}
