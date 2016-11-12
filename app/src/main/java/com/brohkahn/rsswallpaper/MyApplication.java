package com.brohkahn.rsswallpaper;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.brohkahn.loggerlibrary.ErrorHandler;
import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

public class MyApplication extends Application {
	public static boolean showMessageToasts;
	public static boolean showErrorToasts;

	@Override
	public void onCreate() {
		super.onCreate();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Resources res = getResources();
		showMessageToasts = prefs.getBoolean(res.getString(R.string.key_show_message_toasts), true);
		showErrorToasts = prefs.getBoolean(res.getString(R.string.key_show_error_toasts), true);
	}

	private static class ToastHandler extends Handler {
		public Context context;

		ToastHandler(Context context) {
			this.context = context;
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			if (context != null) {
				Toast.makeText(context, String.valueOf(msg.obj), Toast.LENGTH_SHORT).show();
			}
		}
	}

	private ToastHandler toastHandler = new ToastHandler(this);

	public void logEvent(String message, String function, String tag, LogEntry.LogLevel level) {
		Log.d(tag, function + ": " + message);

		if ((showMessageToasts && level == LogEntry.LogLevel.Message)
				|| (showErrorToasts && level == LogEntry.LogLevel.Warning)) {
			Message messageObject = new Message();
			messageObject.obj = message;
			toastHandler.sendMessage(messageObject);
		}

		LogDBHelper helper = LogDBHelper.getHelper(this);
		helper.saveLogEntry(message, null, tag, function, level);
		helper.close();
	}

	public void logException(Exception e, String function, String tag) {
		Log.d(tag, function + ": " + e.getLocalizedMessage());
		LogDBHelper helper = LogDBHelper.getHelper(this);
		helper.saveLogEntry(e.getLocalizedMessage(),
				ErrorHandler.getStackTraceString(e),
				tag,
				function,
				LogEntry.LogLevel.Error
		);
		helper.close();
	}
}

