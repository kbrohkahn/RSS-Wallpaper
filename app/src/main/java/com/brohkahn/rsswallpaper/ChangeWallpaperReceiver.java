package com.brohkahn.rsswallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ChangeWallpaperReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Intent newIntent = new Intent(context, ChangeWallpaperService.class);
		newIntent.setAction(Constants.ACTION_CHANGE_WALLPAPER);
		context.startService(newIntent);
	}
}
