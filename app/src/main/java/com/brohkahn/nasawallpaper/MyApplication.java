package com.brohkahn.nasawallpaper;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

public class MyApplication extends Application {
	public static boolean showToasts;

	@Override
	public void onCreate() {
		super.onCreate();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Resources res = getResources();
		showToasts = prefs.getBoolean(res.getString(R.string.key_update_wifi_only), true);

	}

	private static class ToastHandler extends Handler {
		public Context context;

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			if (context != null) {
				Toast.makeText(context, String.valueOf(msg.obj), Toast.LENGTH_SHORT).show();
			}
		}
	}

	private ToastHandler toastHandler = new ToastHandler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
		}
	};

	public void logEvent(String message, String function, String tag, LogEntry.LogLevel level) {
		Log.d(tag, function + ": " + message);

		if (showToasts && level == LogEntry.LogLevel.Message) {
			Message messageObject = new Message();
			messageObject.obj = message;
			toastHandler.sendMessage(messageObject);
		}

		LogDBHelper helper = LogDBHelper.getHelper(this, true);
		helper.saveLogEntry(message, null, tag, function, level);
		helper.close();
	}
}

