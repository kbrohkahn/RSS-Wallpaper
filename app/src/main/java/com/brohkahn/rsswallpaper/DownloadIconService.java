package com.brohkahn.rsswallpaper;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import com.brohkahn.loggerlibrary.ErrorHandler;
import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DownloadIconService extends IntentService {
	private static final String TAG = "DownloadIconService";

	private static final String ACTION_DOWNLOAD_ICONS = "com.brohkahn.nasawallpaper.action.download_icons";

	private static LogDBHelper logDBHelper;
	private static FeedDBHelper feedDBHelper;

	private String imageDirectory;
	private float iconSize;

	public DownloadIconService() {
		super("DownloadIconService");
	}

	public static void startDownloadIconAction(Context context) {
		Intent intent = new Intent(context, DownloadIconService.class);
		intent.setAction(ACTION_DOWNLOAD_ICONS);
		context.startService(intent);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			String action = intent.getAction();
			if (ACTION_DOWNLOAD_ICONS.equals(action)) {
				startIconDownload();
			}
		}
	}


	protected void startIconDownload() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		Resources resources = getResources();
		boolean wifiOnly = preferences.getBoolean(resources.getString(R.string.key_update_wifi_only), false);
		imageDirectory = preferences.getString(resources.getString(R.string.key_image_directory), getFilesDir()
				.getPath() + "/");
		int currentFeedId = Integer.parseInt(preferences.getString(resources.getString(R.string.key_current_feed), "0"));
		iconSize = resources.getDimension(R.dimen.icon_size);

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

		logEvent(message, "startIconDownload()", LogEntry.LogLevel.Message);

		if (canDownload) {
			List<FeedItem> recentEntries = feedDBHelper.getAllItemsInFeed(currentFeedId);
			for (int i = 0; i < recentEntries.size(); i++) {
				FeedItem entry = recentEntries.get(i);

				File iconFile = new File(imageDirectory + Constants.ICON_BITMAP_PREFIX + entry.imageName);
				if (!iconFile.exists()) {
					if (entry.imageLink == null) {
						logEvent(String.format(Locale.US, "No image link for %s found.", entry.title),
								 "downloadFeedIcon(FeedItem entry)",
								 LogEntry.LogLevel.Warning
						);
					} else {
						downloadFeedIcon(entry);
					}
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
					File file = new File(imageDirectory + Constants.ICON_BITMAP_PREFIX + item.imageName);
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

//		downloadComplete();

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


	private void downloadFeedIcon(FeedItem entry) {
		logEvent(String.format(Locale.US, "Downloading feed image for %s.", entry.title),
				 "downloadFeedIcon(FeedItem entry)",
				 LogEntry.LogLevel.Trace
		);
		try {
			URL url = new URL(entry.imageLink);
			URLConnection connection = url.openConnection();
			connection.connect();

			// get file dimensions first
			BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
			bitmapOptions.inJustDecodeBounds = true;

			InputStream inputStreamToGetSize = url.openStream();
			BitmapFactory.decodeStream(inputStreamToGetSize, null, bitmapOptions);
			inputStreamToGetSize.close();

			int imageHeight = bitmapOptions.outHeight;
			int imageWidth = bitmapOptions.outWidth;

			// calculate inSampleSize
			int inSampleSize = 1;
			while (imageWidth > iconSize && imageHeight > iconSize) {
				imageHeight /= 2;
				imageWidth /= 2;
				inSampleSize *= 2;
			}


			// download the file
			bitmapOptions.inJustDecodeBounds = false;
			bitmapOptions.inSampleSize = inSampleSize;

			InputStream inputStreamToDownload = url.openStream();
			Bitmap bitmap = BitmapFactory.decodeStream(inputStreamToDownload, null, bitmapOptions);
			inputStreamToDownload.close();

			// write bitmap to file
			String outputFilePath = imageDirectory + Constants.ICON_BITMAP_PREFIX + entry.imageName;
			OutputStream output = new FileOutputStream(outputFilePath);
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);

			// clean up
			output.flush();
			output.close();
			bitmap.recycle();

			logEvent(String.format(Locale.US, "Successfully downloaded and saved icon for %s.", entry.title),
					 "downloadFeedIcon(FeedItem entry)",
					 LogEntry.LogLevel.Trace
			);

		} catch (Exception e) {
			Log.e("Error: ", e.getMessage());
			logException(e, "downloadFeedIcon(FeedItem entry)");
		}
	}

	private static void logEvent(String message, String function, LogEntry.LogLevel level) {
		logDBHelper.saveLogEntry(message, null, TAG, function, level);
	}

	private static void logException(Exception e, String function) {
		logDBHelper.saveLogEntry(e.getLocalizedMessage(), ErrorHandler.getStackTraceString(e), TAG, function, LogEntry.LogLevel.Error);
	}
}
