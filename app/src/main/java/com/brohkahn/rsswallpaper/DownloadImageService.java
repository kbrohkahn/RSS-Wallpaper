package com.brohkahn.rsswallpaper;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.brohkahn.loggerlibrary.LogEntry;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DownloadImageService extends IntentService {
	private static final String TAG = "DownloadImageService";


	enum ImageCompressFormat {
		PNG,
		JPEG,
		NONE
	}

	private ImageCompressFormat imageCompressFormat;
	private String imageDirectory;
	private boolean resizeImages;

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
			imageDirectory = Helpers.getStoragePath(this, preferences.getString(resources.getString(R.string.key_image_storage), "LOCAL"));
			resizeImages = preferences.getBoolean(resources.getString(R.string.key_resize_images), false);
			imageCompressFormat = ImageCompressFormat.valueOf(preferences.getString(resources.getString(R.string.key_compression_type), "PNG"));
			int numberToDownload = Integer.parseInt(preferences.getString(resources.getString(R.string.key_number_to_rotate), "7"));
			int currentFeedId = Integer.parseInt(preferences.getString(resources.getString(R.string.key_current_feed), "-1"));
			int currentItemId = preferences.getInt(resources.getString(R.string.key_current_item), -1);

			boolean purgeUnusedImages = preferences.getBoolean(resources.getString(R.string.key_purge_unused_images), false);

			// check if we can download anything based on internet connection
			ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

			BooleanMessage response = Helpers.canDownload(activeNetwork, wifiOnly);

			if (!response.booleanValue) {
				logEvent(response.message, "onHandleIntent()", LogEntry.LogLevel.Message);
			} else {
				logEvent(response.message, "onHandleIntent()", LogEntry.LogLevel.Trace);
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
					if (!item.imageIsDownloaded(imageDirectory)) {
						if (item.imageLink != null) {
							// imagelink is valid
							downloadFeedImage(item);
						} else {
							logEvent(String.format(Locale.US, "No image link for %s found.", item.title),
									"onHandleIntent()",
									LogEntry.LogLevel.Warning
							);
						}

						if (currentItemId == -1) {
							// immediately set wallpaper to new image
							logEvent("New image downloaded, setting as wallpaper.",
									"onHandleIntent()",
									LogEntry.LogLevel.Trace
							);

							currentItemId = item.id;

							Intent newIntent = new Intent(this, ChangeWallpaperService.class);
							newIntent.setAction(Constants.ACTION_CHANGE_WALLPAPER);
							startService(newIntent);
						}
					}
				}

				// purge any other images not in list
				if (purgeUnusedImages) {
					feedDBHelper = FeedDBHelper.getHelper(this);
					for (FeedItem item : allItems) {
						if (item.imageIsDownloaded(imageDirectory) && !feedItemIdsInUse.contains(item.id)) {
							File file = new File(imageDirectory + item.getImageName());
							if (!file.delete()) {
								logEvent(String.format(Locale.US, "Unable to delete image %s.", item.getImageName()),
										"onHandleIntent()",
										LogEntry.LogLevel.Warning
								);
							}
						}
					}
					feedDBHelper.close();
				}

				logEvent("Image download complete", "onHandleIntent()", LogEntry.LogLevel.Trace);

			}
		}
	}

	private void downloadFeedImage(FeedItem entry) {
		logEvent(String.format(Locale.US, "Downloading feed image for %s.", entry.title),
				"downloadFeedImage(FeedItem entry)",
				LogEntry.LogLevel.Trace
		);

		boolean successfulDownload = false;
		for (int tryCount = 0; tryCount < 3 && !successfulDownload; tryCount++) {
			try {
				URL url = new URL(entry.imageLink.replace("http:", "https:"));
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestProperty("Accept-Encoding", "identity");
				connection.connect();

				if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
					logEvent("Server returned HTTP " + connection.getResponseCode(), "downloadFeedImage", LogEntry.LogLevel.Warning);
				}
				if (connection.getContentLength() <= 0) {
					logEvent("Invalid content length for " + entry.imageLink, "downloadFeedImage", LogEntry.LogLevel.Warning);
				}

				String outputFilePath = imageDirectory + entry.getImageName();
				OutputStream output = new FileOutputStream(outputFilePath);

				if (imageCompressFormat == ImageCompressFormat.NONE) {
					InputStream input = new BufferedInputStream(connection.getInputStream(), 8192);
					byte data[] = new byte[8192];

					int count;
					while ((count = input.read(data)) != -1) {
						output.write(data, 0, count);
					}

					output.flush();
					output.close();
					input.close();

					Bitmap bitmap = BitmapFactory.decodeFile(outputFilePath);
					if (bitmap != null && bitmap.getHeight() > 0 && bitmap.getWidth() > 0) {
						successfulDownload = true;
					} else {
						logEvent("Bitmap is null for " + entry.title + ", deleting and trying again.",
								"downloadFeedImage(FeedItem entry)",
								LogEntry.LogLevel.Warning);

						File outputFile = new File(outputFilePath);
						if (!outputFile.delete()) {
							logEvent("Unable to delete file at path " + outputFilePath,
									"downloadFeedImage()",
									LogEntry.LogLevel.Error);
						}
					}
				} else {
					Bitmap bitmap = BitmapFactory.decodeStream(connection.getInputStream());

					if (resizeImages) {
						Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
						Point size = new Point();
						display.getSize(size);
						int screenHeight = size.y;

						int bitmapHeight = bitmap.getHeight();
						int bitmapWidth = bitmap.getWidth();
						while (bitmapHeight / 2 > screenHeight) {
							bitmapHeight /= 2;
							bitmapWidth /= 2;
						}

						bitmap = Bitmap.createScaledBitmap(bitmap, bitmapWidth, bitmapHeight, false);
					}

					Bitmap.CompressFormat compressFormat;
					if (imageCompressFormat == ImageCompressFormat.JPEG) {
						compressFormat = Bitmap.CompressFormat.JPEG;
					} else if (imageCompressFormat == ImageCompressFormat.PNG) {
						compressFormat = Bitmap.CompressFormat.PNG;
					} else {
						compressFormat = null;
					}

					bitmap.compress(compressFormat, 100, output);

					bitmap.recycle();
				}

				logEvent("Successfully downloaded and saved feed image for " + entry.title,
						"downloadFeedImage(FeedItem entry)",
						LogEntry.LogLevel.Trace
				);

//				BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
//				bitmapOptions.inJustDecodeBounds = true;

				connection.disconnect();

			} catch (Exception e) {
				Log.e("Error: ", e.getMessage());
				logException(e, "downloadFeedImage(FeedItem entry)");
			}
		}
	}

	private void logEvent(String message, String function, LogEntry.LogLevel level) {
		((MyApplication) getApplication()).logEvent(message, function, TAG, level);
	}

	private void logException(Exception e, String function) {
		((MyApplication) getApplication()).logException(e, function, TAG);
	}
}
