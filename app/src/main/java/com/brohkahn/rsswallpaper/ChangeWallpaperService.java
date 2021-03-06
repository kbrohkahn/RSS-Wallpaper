package com.brohkahn.rsswallpaper;

import android.app.IntentService;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.WindowManager;

import com.brohkahn.loggerlibrary.LogEntry;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class ChangeWallpaperService extends IntentService {
	private static final String TAG = "ChangeWallpaperService";

	public static final String KEY_INTENT_SOURCE = "intentSource";
	public static final String KEY_ITEM_ID_TO_SET = "idToSet";

	private static final int SCALE_HEIGHT = 0;
	private static final int SCALE_HEIGHT_CROP_CENTER = 1;
	private static final int CROP_CENTER = 2;
	private static final int SCALE_WIDTH_AND_HEIGHT = 3;

	public ChangeWallpaperService() {
		super("ChangeWallpaperService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null && intent.getAction().equals(Constants.ACTION_CHANGE_WALLPAPER)) {

//			int itemIdToSet = intent.getIntExtra(KEY_ITEM_ID_TO_SET, -1);

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			Resources res = getResources();

			// wallpaper
			int numberToRotate = Integer.parseInt(prefs.getString(res.getString(R.string.key_number_to_rotate), "7"));
			boolean shuffle = prefs.getBoolean(res.getString(R.string.key_shuffle), true);
			boolean setHomeWallpaper = prefs.getBoolean(res.getString(R.string.key_set_home_screen), false);
			boolean setLockWallpaper = prefs.getBoolean(res.getString(R.string.key_set_lock_screen), false);
			int cropAndScaleType = Integer.parseInt(prefs.getString(res.getString(R.string.key_crop_and_scale_type), "0"));
			// rss feed
			int currentFeedId = Integer.parseInt(prefs.getString(res.getString(R.string.key_current_feed), "-1"));
			// app setting
			String imageDirectory = Helpers.getStoragePath(this,
					prefs.getString(res.getString(R.string.key_image_storage), "LOCAL"));

			String keyCurrentItem = getResources().getString(R.string.key_current_item);
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
			int currentItemId = settings.getInt(keyCurrentItem, -1);

			FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(this);
			List<FeedItem> itemsToShuffle = feedDBHelper.getRecentItems(numberToRotate, currentFeedId);
			feedDBHelper.close();

			int availableItems = itemsToShuffle.size();
			if (availableItems == 0) {
				logEvent("No items found!", "onHandleIntent()", LogEntry.LogLevel.Warning);

				Intent downloadRSSIntent = new Intent(this, DownloadRSSService.class);
				downloadRSSIntent.setAction(Constants.ACTION_DOWNLOAD_RSS);
				startService(downloadRSSIntent);
			} else {
				int newItemIndex;
				if (shuffle) {
					// get new random item
					Random random = new Random();
					newItemIndex = random.nextInt(availableItems);
				} else {
					// get old item index
					int oldItemIndex = -1;
					for (int i = 0; i < availableItems; i++) {
						FeedItem item = itemsToShuffle.get(i);
						if (item.id == currentItemId && i + 1 < availableItems) {
							oldItemIndex = i;
							break;
						}
					}

					// get item that is immediately after old item
					newItemIndex = (oldItemIndex + 1) % availableItems;
				}

				FeedItem newItem = itemsToShuffle.get(newItemIndex);

				if (!newItem.imageIsDownloaded(imageDirectory)) {
					logEvent("Item " + newItem.title + " not downloaded!", "onHandleIntent()", LogEntry.LogLevel.Warning);

//					Intent downloadRSSIntent = new Intent(this, DownloadImageService.class);
//					downloadRSSIntent.setAction(Constants.ACTION_DOWNLOAD_IMAGES);
//					startService(downloadRSSIntent);
				} else {
					logEvent("Setting wallpaper(s) to " + newItem.title, "onHandleIntent()", LogEntry.LogLevel.Message);

					String imagePath = imageDirectory + newItem.getImageName();

					// get screen height (output wallpaper height)
					Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
					Point size = new Point();
					display.getSize(size);
					int screenHeight = size.y;
					int screenWidth = size.x;

					// get image dimensions
					int inSampleSize;
					if (cropAndScaleType == CROP_CENTER) {
						inSampleSize = 1;
					} else {
						inSampleSize = Helpers.getImageScale(imagePath, 0, screenHeight);
					}

					// Decode bitmap with inSampleSize set
					BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
					bitmapOptions.inJustDecodeBounds = false;
					bitmapOptions.inSampleSize = inSampleSize;

					Bitmap inputBitmap = BitmapFactory.decodeFile(imagePath, bitmapOptions);

					if (inputBitmap == null) {
						File imageFile = new File(imagePath);
						logEvent("Invalid image file " + imagePath, "onHandleIntent()", LogEntry.LogLevel
								.Warning);
						if (!imageFile.delete()) {
							logEvent("Unable to delete Bitmap " + imagePath, "onHandleIntent()", LogEntry.LogLevel
									.Warning);
						}

						Intent downloadRSSIntent = new Intent(this, DownloadImageService.class);
						downloadRSSIntent.setAction(Constants.ACTION_DOWNLOAD_IMAGES);
						startService(downloadRSSIntent);

					} else {
						try {
							int imageHeight = inputBitmap.getHeight();
							int imageWidth = inputBitmap.getWidth();

							Bitmap outputBitmap;
							int x, y;

							switch (cropAndScaleType) {
								case SCALE_HEIGHT:
									outputBitmap = inputBitmap;
									break;
								case SCALE_HEIGHT_CROP_CENTER:
									x = imageWidth / 2 - screenWidth / 2;
									if (x > 0) {
										outputBitmap = Bitmap.createBitmap(inputBitmap, x, 0, screenWidth, imageHeight);
									} else {
										outputBitmap = inputBitmap;
									}
									break;
								case SCALE_WIDTH_AND_HEIGHT:
									outputBitmap = Bitmap.createScaledBitmap(inputBitmap, screenWidth, screenHeight, false);
									break;
								case CROP_CENTER:

									x = imageWidth / 2 - screenWidth / 2;
									y = imageHeight / 2 - screenHeight / 2;
									if (x > 0 && y > 0) {
										outputBitmap = Bitmap.createBitmap(inputBitmap, x, y, screenWidth, screenHeight);
									} else {
										outputBitmap = inputBitmap;
									}
									break;
								default:
									outputBitmap = inputBitmap;
							}

							WallpaperManager myWallpaperManager = WallpaperManager.getInstance(this);
							if (setHomeWallpaper) {
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
									myWallpaperManager.setBitmap(outputBitmap, null, true, WallpaperManager.FLAG_SYSTEM);
								} else {
									myWallpaperManager.setBitmap(outputBitmap);
								}

								logEvent(String.format("Successfully set wallpaper to %s.", newItem.title),
										"onHandleIntent()",
										LogEntry.LogLevel.Trace
								);
							}

							if (setLockWallpaper && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
								myWallpaperManager.setBitmap(outputBitmap, null, true, WallpaperManager.FLAG_LOCK);
								logEvent(String.format("Successfully set lock screen wallpaper to %s.", newItem.title),
										"onHandleIntent()",
										LogEntry.LogLevel.Trace
								);
							}

							inputBitmap.recycle();
							outputBitmap.recycle();
						} catch (IOException e) {
							e.printStackTrace();
							logException(e, "onHandleIntent()");
						} finally {
							SharedPreferences.Editor editor = settings.edit();
							editor.putInt(keyCurrentItem, newItem.id);
							editor.apply();
						}
					}

					sendBroadcast(new Intent(Constants.ACTION_WALLPAPER_UPDATED));
				}
			}

			String intentSource = intent.getStringExtra(KEY_INTENT_SOURCE);
			if (intentSource != null && intentSource.equals(ChangeWallpaperReceiver.TAG)) {
				ChangeWallpaperReceiver.completeWakefulIntent(intent);
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
