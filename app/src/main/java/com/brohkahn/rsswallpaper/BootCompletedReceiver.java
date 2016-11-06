package com.brohkahn.rsswallpaper;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

public class BootCompletedReceiver extends WakefulBroadcastReceiver {
	public static final String TAG = "BootCompletedReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			Intent newIntent = new Intent(context, ScheduleTimerService.class);
			newIntent.setAction(Constants.ACTION_SCHEDULE_ALARMS);
			newIntent.putExtra(Constants.KEY_INTENT_SOURCE, TAG);
			startWakefulService(context, newIntent);
		}
	}
}
