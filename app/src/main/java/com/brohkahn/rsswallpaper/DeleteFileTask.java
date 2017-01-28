package com.brohkahn.rsswallpaper;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class DeleteFileTask extends AsyncTask<Void, Integer, Void> {
	private static final String TAG = "DeleteFileTask";

	private String imageDirectory;

	boolean deleteAllItems;
	boolean purgeOldImages;
//	boolean deleteAllIcons;

	private List<FeedItem> itemImagesToDelete;
//	private List<FeedItem> itemIconsToDelete;

	private Context context;
	private ProgressDialog dialog;

	DeleteFileTask(Context context) {
		this.context = context;

	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Resources res = context.getResources();
		int numberToRotate = Integer.parseInt(prefs.getString(res.getString(R.string.key_number_to_rotate), "7"));
		int currentFeedId = Integer.parseInt(prefs.getString(res.getString(R.string.key_current_feed), "-1"));
		imageDirectory = prefs.getString(res.getString(R.string.key_image_storage), "LOCAL");


		FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(context);
		List<FeedItem> allItems = feedDBHelper.getAllItems();
		List<FeedItem> recentItems = feedDBHelper.getRecentItems(numberToRotate, currentFeedId);

		itemImagesToDelete = new ArrayList<>();

		if (deleteAllItems) {
			itemImagesToDelete.addAll(allItems);
		} else if (purgeOldImages) {
			for (FeedItem item : allItems) {
				if (!recentItems.contains(item)) {
					itemImagesToDelete.add(item);
				}
			}
		}

		String which;
		if (deleteAllItems) {
			which = "all " + String.valueOf(itemImagesToDelete.size()) + " images";
		} else if (purgeOldImages) {
			which = String.valueOf(itemImagesToDelete.size()) + " old images";
		} else {
			which = "no";
		}
		String message = "Deleting " + which + " files";

		dialog = new ProgressDialog(context);
		dialog.setProgress(0);
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setMax(itemImagesToDelete.size() /*+ itemIconsToDelete.size()*/);
		dialog.setTitle(message);
		dialog.show();

		logEvent(message, LogEntry.LogLevel.Trace);

	}

	@Override
	protected void onProgressUpdate(final Integer... values) {
		dialog.setProgress(values[0]);
	}

	@Override
	protected Void doInBackground(Void... voids) {
		for (int i = 0; i < itemImagesToDelete.size(); i++) {
			FeedItem item = itemImagesToDelete.get(i);

			String iconPath = imageDirectory + item.getImageName();

			File iconFile = new File(iconPath);
			if (!iconFile.delete()) {
				logEvent("Unable to delete image " + iconFile, LogEntry.LogLevel.Trace);
			}

			publishProgress(i);
		}

		return null;
	}

	@Override
	protected void onPostExecute(Void aVoid) {
		if (deleteAllItems) {
			FeedDBHelper feedDbHelper = FeedDBHelper.getHelper(context);
			feedDbHelper.deleteFeedItems();
			feedDbHelper.close();
		}

		dialog.dismiss();

		Toast.makeText(context, String.valueOf(itemImagesToDelete.size()) + " files successfully deleted.", Toast
				.LENGTH_SHORT).show();

		context = null;

		super.onPostExecute(aVoid);
	}

	private void logEvent(String message, LogEntry.LogLevel level) {
		LogDBHelper helper = LogDBHelper.getHelper(context);
		helper.saveLogEntry(message, null, TAG, "doInBackground", level);
		helper.close();
	}
}
