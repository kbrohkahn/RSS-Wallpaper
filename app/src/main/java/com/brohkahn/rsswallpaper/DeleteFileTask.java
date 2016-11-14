package com.brohkahn.rsswallpaper;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class DeleteFileTask extends AsyncTask<Void, Void, Void> {
	private static final String TAG = "DeleteFileTask";

	private String imageDirectory;

	boolean deleteAllItems;
	boolean purgeOldImages;
	boolean deleteAllImages;
	boolean deleteAllIcons;

	private List<FeedItem> itemImagesToDelete;
	private List<FeedItem> itemIconsToDelete;

	private Context context;
	private ProgressDialog dialog;

	DeleteFileTask(Context context, String imageDirectory) {
		this.context = context;
		this.imageDirectory = imageDirectory;

	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Resources res = context.getResources();
		int numberToRotate = Integer.parseInt(prefs.getString(res.getString(R.string.key_number_to_rotate), "7"));
		int currentFeedId = Integer.parseInt(prefs.getString(res.getString(R.string.key_current_feed), "-1"));


		FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(context);
		List<FeedItem> allItems = feedDBHelper.getAllItems();
		List<FeedItem> recentItems = feedDBHelper.getRecentItems(numberToRotate, currentFeedId);

		itemImagesToDelete = new ArrayList<>();

		if (deleteAllImages) {
			itemImagesToDelete.addAll(allItems);
		} else if (purgeOldImages) {
			for (FeedItem item : allItems) {
				if (!recentItems.contains(item)) {
					itemImagesToDelete.add(item);
				}
			}
		}

		itemIconsToDelete = new ArrayList<>();
		if (deleteAllIcons) {
			itemIconsToDelete.addAll(allItems);
		}

		String which;
		if (deleteAllIcons) {
			if (deleteAllImages) {
				which = "all icons and images";
			} else if (purgeOldImages) {
				which = "icons and old images";
			} else {
				which = "icons";
			}
		} else if (purgeOldImages) {
			which = "old images";
		} else {
			which = "no";
		}
		String message = "Deleting " + which + " files";

		dialog = new ProgressDialog(context);
		dialog.setProgress(0);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setMax(itemImagesToDelete.size() + itemIconsToDelete.size());
		dialog.setTitle(message);

		logEvent(message, LogEntry.LogLevel.Trace);

	}

	@Override
	protected void onProgressUpdate(Void... values) {
		super.onProgressUpdate(values);
	}

	@Override
	protected Void doInBackground(Void... voids) {
		for (FeedItem item : itemIconsToDelete) {
			String iconPath = imageDirectory + Constants.ICONS_FOLDER + item.getIconName();

			File iconFile = new File(iconPath);
			if (!iconFile.delete()) {
				logEvent("Unable to delete icon " + iconFile, LogEntry.LogLevel.Trace);
			}
		}

		for (FeedItem item : itemImagesToDelete) {
			String iconPath = imageDirectory + item.getImageName();

			File iconFile = new File(iconPath);
			if (!iconFile.delete()) {
				logEvent("Unable to delete icon " + iconFile, LogEntry.LogLevel.Trace);
			}
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
		context = null;
		super.onPostExecute(aVoid);
	}

	private void logEvent(String message, LogEntry.LogLevel level) {
		LogDBHelper helper = LogDBHelper.getHelper(context);
		helper.saveLogEntry(message, null, TAG, "doInBackground", level);
		helper.close();
	}
}
