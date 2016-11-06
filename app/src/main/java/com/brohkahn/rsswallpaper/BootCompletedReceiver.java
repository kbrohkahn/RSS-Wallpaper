package com.brohkahn.rsswallpaper;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

public class BootCompletedReceiver extends WakefulBroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			Intent newIntent = new Intent(context, ScheduleTimerService.class);
			newIntent.setAction(Constants.ACTION_SCHEDULE_ALARMS);
			startWakefulService(context, newIntent);
		}
	}
}
