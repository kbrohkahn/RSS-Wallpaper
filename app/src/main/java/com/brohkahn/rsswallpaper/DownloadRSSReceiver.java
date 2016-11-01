package com.brohkahn.rsswallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DownloadRSSReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Intent newIntent = new Intent(context, DownloadRSSService.class);
		newIntent.setAction(Constants.ACTION_DOWNLOAD_RSS);
		context.startService(newIntent);
	}
}
