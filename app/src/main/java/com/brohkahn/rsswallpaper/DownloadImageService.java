package com.brohkahn.rsswallpaper;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.brohkahn.loggerlibrary.ErrorHandler;
import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DownloadImageService extends IntentService {
	private static final String TAG = "DownloadRSSService";

	private static final String ACTION_DOWNLOAD_IMAGES = "com.brohkahn.nasawallpaper.action.download_images";


	private static LogDBHelper logDBHelper;
	private static FeedDBHelper feedDBHelper;

	private String imageDirectory;

	public DownloadImageService() {
		super("DownloadImageService");
	}

	public static void startDownloadImageAction(Context context) {
		Intent intent = new Intent(context, DownloadImageService.class);
		intent.setAction(ACTION_DOWNLOAD_IMAGES);
		context.startService(intent);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			String action = intent.getAction();
			if (ACTION_DOWNLOAD_IMAGES.equals(action)) {
				startImageDownload();
			}
		}
	}

	protected void startImageDownload() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		Resources resources = getResources();
		boolean wifiOnly = preferences.getBoolean(resources.getString(R.string.key_update_wifi_only), false);
		imageDirectory = preferences.getString(resources.getString(R.string.key_image_directory), getFilesDir()
				.getPath() + "/");
		int numberToDownload = Integer.parseInt(preferences.getString(resources.getString(R.string.key_number_to_rotate), "7"));
		int currentFeedId = Integer.parseInt(preferences.getString(resources.getString(R.string.key_current_feed), "0"));

		// check if we can download anything based on internet connection
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

		String message;
		boolean canDownload;
		if (activeNetwork == null) {
			message = "Not connected to internet, unable to download images.";
			canDownload = false;
		} else {
			boolean wifiConnection = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
			if (wifiOnly && !wifiConnection) {
				message = "Not connected to Wifi, unable to download images.";
				canDownload = false;
			} else {
				canDownload = true;
				if (wifiOnly) {
					message = "Connected to Wifi, starting download of images.";
				} else {
					message = "Connected to internet, starting download of images.";
				}
			}
		}

		logDBHelper = LogDBHelper.getHelper(this);
		feedDBHelper = FeedDBHelper.getHelper(this);

		logEvent(message, "startDownloadIntent()", LogEntry.LogLevel.Message);

		if (canDownload) {
			List<FeedItem> recentEntries = feedDBHelper.getRecentItems(numberToDownload, currentFeedId);
			for (int i = 0; i < recentEntries.size(); i++) {
				FeedItem entry = recentEntries.get(i);
				if (!entry.downloaded) {
					downloadFeedImage(entry);
				}

				if (i == 0) {
					// immediately set wallpaper to first image
					Intent intent = new Intent(Constants.SET_WALLPAPER_ACTION);
					LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
				}
			}

			// get IDs of items in use
			List<Integer> feedItemIdsInUse = new ArrayList<>();
			for (FeedItem item : recentEntries) {
				feedItemIdsInUse.add(item.id);
			}

			// purge any other images not in list
			List<FeedItem> allItems = feedDBHelper.getAllItems();
			for (FeedItem item : allItems) {
				if (item.downloaded && !feedItemIdsInUse.contains(item.id)) {
					File file = new File(imageDirectory + item.imageName);
					if (file.delete()) {
						feedDBHelper.updateImageDownload(item.id, false);
					} else {
						logEvent(String.format(Locale.US, "Unable to delete image %s.", item.imageName),
								 "saveFeedItems(String urlString)",
								 LogEntry.LogLevel.Warning
						);
					}
				}
			}
		}
		downloadComplete();
	}


	private void downloadComplete() {
		if (feedDBHelper != null) {
			feedDBHelper.close();
			feedDBHelper = null;
		}

		if (logDBHelper != null) {
			logDBHelper.close();
			logDBHelper = null;
		}

		stopSelf();
	}


	private void downloadFeedImage(FeedItem entry) {
		try {
			if (entry.imageLink == null) {
				logEvent(String.format(Locale.US, "No image link for %s found.", entry.title),
						 "downloadFeedItem(FeedItem entry)",
						 LogEntry.LogLevel.Warning
				);
				return;
			}

			logEvent(String.format(Locale.US, "Downloading feed image for %s.", entry.title),
					 "downloadFeedItem(FeedItem entry)",
					 LogEntry.LogLevel.Message
			);


			String outputFilePath = imageDirectory + entry.imageName;

			URL url = new URL(entry.imageLink);
			URLConnection connection = url.openConnection();
			connection.connect();

			// download the file
			InputStream input = new BufferedInputStream(url.openStream(), 8192);

			// Output stream
			OutputStream output = new FileOutputStream(outputFilePath);

			byte data[] = new byte[1024];

			int count;
			while ((count = input.read(data)) != -1) {
				output.write(data, 0, count);
			}

			output.flush();
			output.close();
			input.close();

			feedDBHelper.updateImageDownload(entry.id, true);

			logEvent(String.format(Locale.US, "Successfully downloaded and saved feed image for %s.", entry.title),
					 "downloadFeedItem(FeedItem entry)",
					 LogEntry.LogLevel.Message
			);

		} catch (Exception e) {
			Log.e("Error: ", e.getMessage());
			logException(e, "downloadFeedItem(FeedItem entry)");
		}
	}

	private static void logEvent(String message, String function, LogEntry.LogLevel level) {
		logDBHelper.saveLogEntry(message, null, TAG, function, level);
	}

	private static void logException(Exception e, String function) {
		logDBHelper.saveLogEntry(e.getLocalizedMessage(), ErrorHandler.getStackTraceString(e), TAG, function, LogEntry.LogLevel.Error);
	}
}
