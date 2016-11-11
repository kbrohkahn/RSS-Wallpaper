package com.brohkahn.rsswallpaper;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

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
	private static final String TAG = "DownloadImageService";

	private String imageDirectory;

	public DownloadImageService() {
		super("DownloadImageService");
	}

	public static void startDownloadImageAction(Context context) {
		Intent intent = new Intent(context, DownloadImageService.class);
		intent.setAction(Constants.ACTION_DOWNLOAD_IMAGES);
		context.startService(intent);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null && intent.getAction().equals(Constants.ACTION_DOWNLOAD_IMAGES)) {
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
			Resources resources = getResources();
			boolean wifiOnly = preferences.getBoolean(resources.getString(R.string.key_update_wifi_only), false);
			imageDirectory = preferences.getString(resources.getString(R.string.key_image_directory), getFilesDir()
					.getPath() + "/");
			int numberToDownload = Integer.parseInt(preferences.getString(resources.getString(R.string.key_number_to_rotate), "7"));
			int currentFeedId = Integer.parseInt(preferences.getString(resources.getString(R.string.key_current_feed), "-1"));
			int currentItemId = preferences.getInt(resources.getString(R.string.key_current_item), -1);
			boolean purgeUnusedImages = preferences.getBoolean(resources.getString(R.string.key_purge_unused_images),
					false);

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

			if (!canDownload) {
				logEvent(message, "startImageDownload()", LogEntry.LogLevel.Message);
			} else {
				logEvent(message, "startImageDownload()", LogEntry.LogLevel.Trace);
				List<Integer> feedItemIdsInUse = new ArrayList<>();

				// get list of recent entries and all entries
				FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(this);
				List<FeedItem> recentItems = feedDBHelper.getRecentItems(numberToDownload, currentFeedId);
				List<FeedItem> allItems = feedDBHelper.getAllItems();
				feedDBHelper.close();

				for (int i = 0; i < recentItems.size(); i++) {
					FeedItem item = recentItems.get(i);

					// add id to list of recent items
					feedItemIdsInUse.add(item.id);

					// see if we need to download
					if (!item.isDownloaded(imageDirectory)) {
						if (item.imageLink != null) {
							// imagelink is valid
							downloadFeedImage(item);
						} else {
							logEvent(String.format(Locale.US, "No image link for %s found.", item.title),
									"startImageDownload()",
									LogEntry.LogLevel.Warning
							);
						}

						if (currentItemId == -1) {
							// immediately set wallpaper to new image
							logEvent("New image downloaded, setting as wallpaper.",
									"startImageDownload()",
									LogEntry.LogLevel.Trace
							);

							currentItemId = item.id;
							sendBroadcast(new Intent(Constants.ACTION_CHANGE_WALLPAPER));
						}
					}
				}

				// purge any other images not in list
				if (purgeUnusedImages) {
					feedDBHelper = FeedDBHelper.getHelper(this);
					for (FeedItem item : allItems) {
						if (item.isDownloaded(imageDirectory) && !feedItemIdsInUse.contains(item.id)) {
							File file = new File(imageDirectory + item.getImageName());
							if (!file.delete()) {
								logEvent(String.format(Locale.US, "Unable to delete image %s.", item.getImageName()),
										"startImageDownload()",
										LogEntry.LogLevel.Warning
								);
							}
						}
					}
					feedDBHelper.close();
				}

				logEvent("Image download complete", "startImageDownload()", LogEntry.LogLevel.Trace);

			}
		}
	}

	private void downloadFeedImage(FeedItem entry) {
		logEvent(String.format(Locale.US, "Downloading feed image for %s.", entry.title),
				"downloadFeedImage(FeedItem entry)",
				LogEntry.LogLevel.Trace
		);

		try {
			URL url = new URL(entry.imageLink);
			URLConnection connection = url.openConnection();
			connection.connect();

			InputStream input = new BufferedInputStream(url.openStream(), 8192);

			String outputFilePath = imageDirectory + entry.getImageName();
			OutputStream output = new FileOutputStream(outputFilePath);

			byte data[] = new byte[1024];

			int count;
			while ((count = input.read(data)) != -1) {
				output.write(data, 0, count);
			}

			output.flush();
			output.close();
			input.close();

			logEvent(String.format(Locale.US, "Successfully downloaded and saved feed image for %s.", entry.title),
					"downloadFeedImage(FeedItem entry)",
					LogEntry.LogLevel.Trace
			);

		} catch (Exception e) {
			Log.e("Error: ", e.getMessage());
			logException(e, "downloadFeedImage(FeedItem entry)");
		}
	}

	private void logEvent(String message, String function, LogEntry.LogLevel level) {
		((MyApplication) getApplication()).logEvent(message, function, TAG, level);
	}

	private void logException(Exception e, String function) {
		((MyApplication) getApplication()).logException(e, function, TAG);
	}
}
