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

import com.brohkahn.loggerlibrary.LogEntry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Locale;

public class DownloadIconService extends IntentService {
	private static final String TAG = "DownloadIconService";

	private String imageDirectory;
	private float iconSize;

	public DownloadIconService() {
		super("DownloadIconService");
	}

	public static void startDownloadIconAction(Context context) {
		Intent intent = new Intent(context, DownloadIconService.class);
		intent.setAction(Constants.ACTION_DOWNLOAD_ICONS);
		context.startService(intent);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// make sure it's the right intent
		if (intent != null && intent.getAction().equals(Constants.ACTION_DOWNLOAD_ICONS)) {

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
			Resources resources = getResources();
			boolean wifiOnly = preferences.getBoolean(resources.getString(R.string.key_update_wifi_only), false);
			imageDirectory = preferences.getString(resources.getString(R.string.key_image_directory),
					Helpers.getDefaultFolder(this));
			int currentFeedId = Integer.parseInt(preferences.getString(resources.getString(R.string.key_current_feed), "-1"));
			boolean downloadIcons = preferences.getBoolean(resources.getString(R.string.key_store_icons), true);
			iconSize = resources.getDimension(R.dimen.icon_size);

			if (!downloadIcons) {
				stopSelf();
				return;
			}

			// check if we can download anything based on internet connection
			ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

			BooleanMessage response = Helpers.canDownload(activeNetwork, wifiOnly);

			if (!response.booleanValue) {
				logEvent(response.message, "onHandleIntent()", LogEntry.LogLevel.Message);
			} else {
				logEvent(response.message, "onHandleIntent()", LogEntry.LogLevel.Trace);

				// get items in current feed and all items
				FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(this);
				List<FeedItem> allItems = feedDBHelper.getAllItems();
				feedDBHelper.close();

				// make sure icon directory exists
				File iconDirectory = new File(imageDirectory + Constants.ICONS_FOLDER);
				if (!iconDirectory.exists()) {
					if (!iconDirectory.mkdir()) {
						logEvent("Unable to create icon directory " + iconDirectory.getAbsolutePath(),
								"onHandleIntent",
								LogEntry.LogLevel.Error);
						return;
					}
				}

				for (FeedItem item : allItems) {
					File iconFile = new File(imageDirectory + Constants.ICONS_FOLDER + item.getIconName());
					if (!iconFile.exists()) {
						if (item.imageLink == null) {
							logEvent(String.format(Locale.US, "No image link for %s found.", item.title),
									"startIconDownload()",
									LogEntry.LogLevel.Warning
							);
						} else {
							downloadFeedIcon(item);
						}
					}
				}

				logEvent("Icon download complete", "startImageDownload()", LogEntry.LogLevel.Trace);

			}
		}
	}


	private void downloadFeedIcon(FeedItem entry) {
		logEvent(String.format(Locale.US, "Downloading feed icon for %s.", entry.title),
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
			String outputFilePath = imageDirectory + Constants.ICONS_FOLDER + entry.getIconName();
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

	private void logEvent(String message, String function, LogEntry.LogLevel level) {
		((MyApplication) getApplication()).logEvent(message, function, TAG, level);
	}

	private void logException(Exception e, String function) {
		((MyApplication) getApplication()).logException(e, function, TAG);
	}
}
