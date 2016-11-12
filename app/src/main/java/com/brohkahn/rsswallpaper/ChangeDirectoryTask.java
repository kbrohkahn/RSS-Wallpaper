package com.brohkahn.rsswallpaper;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.io.File;

class ChangeDirectoryTask extends AsyncTask<String, Void, Boolean> {
	private static final String TAG = "ChangeDirectoryTask";

	private Context context;

	ChangeDirectoryTask(Context context) {
		this.context = context;
	}

	@Override

	protected void onPostExecute(Boolean success) {
		if (!success) {
			logEvent("Failed to copy all files to new folder", LogEntry.LogLevel.Warning);
		} else {
			logEvent("Successfully moved all files", LogEntry.LogLevel.Message);
		}

		super.onPostExecute(success);
	}

	@Override
	protected Boolean doInBackground(String... params) {
		String oldPath = params[0];
		String newPath = params[1];

		if (oldPath == null) {
			logEvent("Original file directory is invalid", LogEntry.LogLevel.Error);
			return false;
		} else if (newPath == null) {
			logEvent("New file directory is invalid", LogEntry.LogLevel.Error);
			return false;
		} else {
			boolean success = true;

			File oldDirectory = new File(oldPath);
			for (File file : oldDirectory.listFiles()) {
				File newFile = new File(newPath + file.getName());
				if (!file.renameTo(newFile)) {
					logEvent("Error moving file " + file.getAbsolutePath(), LogEntry.LogLevel.Error);
					success = false;
				}
			}

			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
			editor.putString(context.getResources().getString(R.string.key_image_directory), newPath);
			editor.apply();

			return success;
		}
	}

	private void logEvent(String message, LogEntry.LogLevel level) {
		LogDBHelper helper = LogDBHelper.getHelper(context);
		helper.saveLogEntry(message, null, TAG, "doInBackground", level);
		helper.close();
	}
}
