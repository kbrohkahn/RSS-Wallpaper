package com.brohkahn.rsswallpaper;

import android.app.Service;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Display;
import android.view.WindowManager;

import com.brohkahn.loggerlibrary.LogEntry;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class ChangeWallpaperService extends Service {
	private static final String TAG = "ChangeWallpaperService";

	private static final int SCALE_HEIGHT = 0;
	private static final int SCALE_HEIGHT_CROP_CENTER = 1;
	private static final int CROP_CENTER = 2;
	private static final int SCALE_WIDTH_AND_HEIGHT = 3;

	private static final int MS_MINUTE = 1000 * 60;
	private static final int MS_HOUR = MS_MINUTE * 60;

	private int numberToRotate;
	private int cropAndScaleType;
	private boolean shuffle;
	private boolean setHomeWallpaper;
	private boolean setLockWallpaper;

	private int currentFeedId;
	private String imageDirectory;

	private Random random = new Random();
	private Timer timer = new Timer();

	public static boolean isRunning = false;

	public ChangeWallpaperService() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public void onCreate() {
//		Thread.setDefaultUncaughtExceptionHandler(new ErrorHandler(this, false));

		// stop and start main activity if permissions not granted
//		int internetPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
//		int wallpaperPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.SET_WALLPAPER);
//		if (internetPermission != PackageManager.PERMISSION_GRANTED || wallpaperPermission != PackageManager.PERMISSION_GRANTED) {
//			logEvent(
//					"Perrmissions not granted, stopping service and requesting permissions in MainActivity",
//					"onCreate()",
//					LogEntry.LogLevel.Message
//			);
//			startActivity(new Intent(this, MainActivity.class));
//			stopSelf(START_NOT_STICKY);
//		}

		// stop if already running an instance
		if (isRunning) {
			logEvent("Service already started, stopping this instance",
					 "onCreate()",
					 LogEntry.LogLevel.Trace
			);
			stopSelf(START_STICKY);
		}

		logEvent("Creating instance of service.",
				 "onCreate()",
				 LogEntry.LogLevel.Trace
		);
		isRunning = true;

		// get settings
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Resources res = getResources();

		// wallpaper
		numberToRotate = Integer.parseInt(prefs.getString(res.getString(R.string.key_number_to_rotate), "7"));
		shuffle = prefs.getBoolean(res.getString(R.string.key_shuffle), true);
		int changeInterval = Integer.parseInt(prefs.getString(res.getString(R.string.key_change_interval), "30"));
		setHomeWallpaper = prefs.getBoolean(res.getString(R.string.key_set_home_screen), true);
		setLockWallpaper = prefs.getBoolean(res.getString(R.string.key_set_lock_screen), false);
		cropAndScaleType = Integer.parseInt(prefs.getString(res.getString(R.string.key_crop_and_scale_type), "0"));
		// rss feed
		int updateInterval = Integer.parseInt(prefs.getString(res.getString(R.string.key_update_interval), "24"));
		int updateTime = Integer.parseInt(prefs.getString(res.getString(R.string.key_update_time), "3"));
		currentFeedId = Integer.parseInt(prefs.getString(res.getString(R.string.key_current_feed), "0"));

		// app setting
		imageDirectory = prefs.getString(res.getString(R.string.key_image_directory), getFilesDir().getPath() + "/");

		// setup changeWallpaperNow
		// The filter's action is BROADCAST_ACTION
		IntentFilter mStatusIntentFilter = new IntentFilter(Constants.SET_WALLPAPER_ACTION);
		LocalBroadcastManager.getInstance(this)
							 .registerReceiver(changeWallpaperNow, mStatusIntentFilter);

		startRSSDownloadIntent();

//		FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(this);
//		int itemsWithoutImageCount = feedDBHelper.getRecentItemsWithImages(numberToRotate, currentFeedId)
//												 .size();
//		int mostRecentItemCount = feedDBHelper.getRecentItems(1, currentFeedId).size();
//		if (mostRecentItemCount == 0) {
//			// download RSS immediately if no items found
//			startRSSDownloadIntent();
//		} else if (itemsWithoutImageCount > 0){
//			// download image immediately if images needed
//			startImageDownloadIntent();
//		}

		// set download time
		Calendar downloadTime = Calendar.getInstance();
		if (downloadTime.get(Calendar.HOUR_OF_DAY) > updateTime) {
			// set to next day if we've already passed hour
			downloadTime.set(Calendar.DAY_OF_YEAR, downloadTime.get(Calendar.DAY_OF_YEAR) + 1);
		}
		downloadTime.set(Calendar.HOUR_OF_DAY, updateTime);
		downloadTime.set(Calendar.MINUTE, 0);
		downloadTime.set(Calendar.SECOND, 0);
		downloadTime.set(Calendar.MILLISECOND, 0);

		// schedule update task
		timer.scheduleAtFixedRate(downloadRSSTask,
								  downloadTime.getTime(),
								  updateInterval * MS_HOUR
		);

		// schedule change task
		timer.scheduleAtFixedRate(changeWallpaperTask, 0, changeInterval * MS_MINUTE);

		super.onCreate();
	}

//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        return super.onStartCommand(intent, flags, startId);
//    }

	@Override
	public void onDestroy() {
		logEvent("Service has been destroyed, removing changeWallpaperNow broadcast receiver, " +
						 "setting isRunning to false, and cancelling and purging timer.",
				 "onCreate()",
				 LogEntry.LogLevel.Trace
		);

		LocalBroadcastManager.getInstance(this).unregisterReceiver(changeWallpaperNow);

		isRunning = false;

		timer.cancel();
		timer.purge();
		timer = null;
		super.onDestroy();
	}

	private final BroadcastReceiver changeWallpaperNow = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			logEvent("Received changeWallpaperNow broadcast.",
					 "onReceive(Context context, Intent intent)",
					 LogEntry.LogLevel.Trace
			);
			setNewWallpaper();
		}
	};

	private final TimerTask changeWallpaperTask = new TimerTask() {
		@Override
		public void run() {
			setNewWallpaper();
		}
	};

	private void setNewWallpaper() {
		logEvent("Setting new wallpaper.", "setNewWallpaper()", LogEntry.LogLevel.Message);

		String keyCurrentItem = getResources().getString(R.string.key_current_item);
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		int currentItemId = settings.getInt(keyCurrentItem, 0);

		FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(this);
		List<FeedItem> itemsToShuffle = feedDBHelper.getRecentItemsWithImages(numberToRotate, currentFeedId);
		feedDBHelper.close();

		int availableItems = itemsToShuffle.size();
		if (availableItems == 0) {
			logEvent("No available images to set as wallpaper",
					 "setNewWallpaper()",
					 LogEntry.LogLevel.Warning
			);
			return;
		}

		// get new random item
		int newItemIndex = 0;
		if (shuffle) {
			newItemIndex = random.nextInt(availableItems);
		} else {
			for (int i = 0; i < availableItems; i++) {
				FeedItem item = itemsToShuffle.get(i);
				if (item.id == currentItemId && i + 1 < availableItems) {
					newItemIndex = i + 1;
				}
			}
		}

		FeedItem newItem = itemsToShuffle.get(newItemIndex);

		logEvent(String.format(Locale.US,
							   "Setting wallpaper to %s with id of %d.",
							   newItem.title,
							   newItem.id
				 ),
				 "setNewWallpaper()",
				 LogEntry.LogLevel.Message
		);
		try {
			String imagePath = imageDirectory + newItem.getImageName();
			if (!new File(imagePath).exists()) {
				logEvent(String.format(Locale.US, "File %s not found.", imagePath),
						 "setNewWallpaper()",
						 LogEntry.LogLevel.Warning
				);
				DownloadImageService.startDownloadImageAction(this);
			} else {
				// get screen height (output wallpaper height)
				Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
				Point size = new Point();
				display.getSize(size);
				int screenHeight = size.y;
				int screenWidth = size.x;


				// get image dimensions
				BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
				bitmapOptions.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(imagePath, bitmapOptions);

				// Calculate inSampleSize
				int imageHeight = bitmapOptions.outHeight;
				int imageWidth = bitmapOptions.outWidth;

				int inSampleSize = 1;
				while (cropAndScaleType != CROP_CENTER && imageHeight > screenHeight) {
					imageHeight /= 2;
					imageWidth /= 2;
					inSampleSize *= 2;
				}

				// Decode bitmap with inSampleSize set
				bitmapOptions.inJustDecodeBounds = false;
				bitmapOptions.inSampleSize = inSampleSize;
				Bitmap inputBitmap = BitmapFactory.decodeFile(imagePath, bitmapOptions);
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

					logEvent("Successfully set wallpaper.",
							 "setNewWallpaper()",
							 LogEntry.LogLevel.Message
					);
				}

				if (setLockWallpaper && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					myWallpaperManager.setBitmap(outputBitmap, null, true, WallpaperManager.FLAG_LOCK);
					logEvent("Successfully set lock screen wallpaper.",
							 "setNewWallpaper()",
							 LogEntry.LogLevel.Message
					);
				}

				inputBitmap.recycle();
				outputBitmap.recycle();
			}
		} catch (IOException e) {
			e.printStackTrace();
			logException(e, "setNewWallpaper()");
		} finally {
			SharedPreferences.Editor editor = settings.edit();
			editor.putInt(keyCurrentItem, newItem.id);
			editor.apply();

			Intent localIntent = new Intent(Constants.WALLPAPER_UPDATED);
			LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
		}
	}

	private TimerTask downloadRSSTask = new TimerTask() {
		@Override
		public void run() {
			startRSSDownloadIntent();
		}
	};

	private void startRSSDownloadIntent() {
		DownloadRSSService.startDownloadRSSAction(this);
	}

	private void logEvent(String message, String function, LogEntry.LogLevel level) {
		((MyApplication) getApplication()).logEvent(message, function, TAG, level);
	}

	private void logException(Exception e, String function) {
		((MyApplication) getApplication()).logException(e, function, TAG);
	}
}
