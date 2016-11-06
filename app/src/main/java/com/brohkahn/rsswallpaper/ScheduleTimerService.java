package com.brohkahn.rsswallpaper;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
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

public class ScheduleTimerService extends IntentService {
	private static final String TAG = "BootCompletedReceiver";

	private static final int MS_MINUTE = 1000 * 60;
	private static final int MS_HOUR = MS_MINUTE * 60;

	public ScheduleTimerService() {
		super("ScheduleTimerService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null && intent.getAction().equals(Constants.ACTION_SCHEDULE_ALARMS)) {
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
			Resources resources = getResources();
			int rssUpdateInterval = Integer.parseInt(preferences.getString(resources.getString(R.string.key_update_interval), "24"));
			int rssUpdateTime = Integer.parseInt(preferences.getString(resources.getString(R.string.key_update_time), "3"));
			int changeWallpaperInterval = Integer.parseInt(preferences.getString(resources.getString(R.string.key_change_interval), "30"));


			// create intent and pending intent for ChangeWallpaperReceiver
			Intent changeWallpaperIntent = new Intent(this, ChangeWallpaperReceiver.class);
			changeWallpaperIntent.setAction(Constants.ACTION_CHANGE_WALLPAPER);
			PendingIntent wallpaperScheduleIntent = PendingIntent.getBroadcast(this, 0, changeWallpaperIntent, 0);

			// cancel alarm (if already running), and reschedule
			AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
			alarmManager.cancel(wallpaperScheduleIntent);
			alarmManager.setInexactRepeating(AlarmManager.RTC, 0, changeWallpaperInterval * MS_MINUTE,
					wallpaperScheduleIntent);

			// set RSS update time
			Calendar downloadTime = Calendar.getInstance();
			downloadTime.setTimeInMillis(System.currentTimeMillis());
			if (downloadTime.get(Calendar.HOUR_OF_DAY) >= rssUpdateTime) {
				downloadTime.set(Calendar.DAY_OF_YEAR, downloadTime.get(Calendar.DAY_OF_YEAR) + 1);
			}
			downloadTime.set(Calendar.HOUR_OF_DAY, rssUpdateTime);

			// create intent and pending intent for DownloadRSSService
			Intent downloadRSSIntent = new Intent(this, DownloadRSSService.class);
			downloadRSSIntent.setAction(Constants.ACTION_DOWNLOAD_RSS);
			PendingIntent rssScheduleIntent = PendingIntent.getService(this, 0, downloadRSSIntent, PendingIntent.FLAG_CANCEL_CURRENT);


			// cancel alarm (if already running), and reschedule
			alarmManager.cancel(rssScheduleIntent);
			alarmManager.setInexactRepeating(AlarmManager.RTC, downloadTime.getTimeInMillis(), rssUpdateInterval
					* MS_HOUR, rssScheduleIntent);


			String message = String.format(Locale.US,
					"Scheduled downloadRSSIntent every %d hours and changeWallpaperIntent every %d minutes",
					rssUpdateInterval,
					changeWallpaperInterval);
			Log.d(TAG, "onReceive(Context context, Intent intent): " + message);

			LogDBHelper helper = LogDBHelper.getHelper(this);
			helper.saveLogEntry(message, null, TAG, "onReceive(Context context, Intent intent)", LogEntry.LogLevel.Trace);
			helper.close();

			BootCompletedReceiver.completeWakefulIntent(intent);

		}
	}
}
