package com.brohkahn.nasawallpaper;

import android.Manifest;
import android.app.Service;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.brohkahn.loggerlibrary.ErrorHandler;
import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class ChangeWallpaperService extends Service {
    private static final String TAG = "ChangeWallpaperService";

    private static final String NASA_RSS_LINK_A = "http://www.nasa.gov/rss/dyn/lg_image_of_the_day.rss";
//    private static final String NASA_RSS_LINK_B = "http://apod.nasa.gov/apod.rss";

    private int numberToShuffle;
    private boolean setHomeWallpaper;
    private boolean setLockWallpaper;
    private String imageDirectory;

    private static final int changeWallpaperInterval = 60 * 60 * 1000;
    private static final int downloadWallpaperInterval = 24 * 60 * 60 * 1000;
    private Random random = new Random();
    private Timer timer;

    public static Bitmap currentImage;


    public ChangeWallpaperService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        Thread.setDefaultUncaughtExceptionHandler(new ErrorHandler(this, false));

        int internetPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        int wallpaperPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.SET_WALLPAPER);
        if (internetPermission != PackageManager.PERMISSION_GRANTED || wallpaperPermission != PackageManager.PERMISSION_GRANTED) {
            startActivity(new Intent(this, MainActivity.class));
            return START_NOT_STICKY;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Resources resources = getResources();

        numberToShuffle = preferences.getInt(resources.getString(R.string.key_number_to_shuffle), 7);
        setHomeWallpaper = preferences.getBoolean(resources.getString(R.string.key_set_home_screen), true);
        setLockWallpaper = preferences.getBoolean(resources.getString(R.string.key_set_home_screen), false);
        imageDirectory = preferences.getString(resources.getString(R.string.key_image_directory), getFilesDir().getPath() + "/");

        // setup changeWallpaperNow
        // The filter's action is BROADCAST_ACTION
        IntentFilter mStatusIntentFilter = new IntentFilter(Constants.SET_WALLPAPER_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(changeWallpaperNow, mStatusIntentFilter);

        // start download immediately
        startDownloadIntent();

        // download wallpaper daily at 0300
        Calendar downloadTime = Calendar.getInstance();
        downloadTime.set(Calendar.DAY_OF_YEAR, downloadTime.get(Calendar.DAY_OF_YEAR) + 1);
        downloadTime.set(Calendar.HOUR_OF_DAY, 3);
        downloadTime.set(Calendar.MINUTE, 0);
        downloadTime.set(Calendar.SECOND, 0);
        downloadTime.set(Calendar.MILLISECOND, 0);

        timer = new Timer();
        timer.scheduleAtFixedRate(downloadRSSTask, downloadTime.getTime(), downloadWallpaperInterval);
        timer.scheduleAtFixedRate(changeWallpaperTask, 0, changeWallpaperInterval);

        return super.onStartCommand(intent, flags, startId);
    }

    private TimerTask changeWallpaperTask = new TimerTask() {
        @Override
        public void run() {
            setNewWallpaper();
        }
    };

    private void setNewWallpaper() {
        logEvent("Setting new wallpaper.", "setNewWallpaper()", LogEntry.LogLevel.Message);

        String keyCurrentItem = getResources().getString(R.string.key_current_item);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
//        long currentItemId = settings.getLong(keyCurrentItem, 0);

        FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(this, false);
        List<FeedItem> itemsToShuffle = feedDBHelper.getRecentItems(numberToShuffle);
        feedDBHelper.close();

        int count = itemsToShuffle.size();
        if (count == 0) {
            logEvent("No available images to set as wallpaper", "setNewWallpaper()", LogEntry.LogLevel.Warning);
            return;
        }

        // get new random item
        int newItemIndex = random.nextInt(count);
        FeedItem newItem = itemsToShuffle.get(newItemIndex);

        logEvent(String.format(Locale.US, "Setting wallpaper to %s with id of %d.", newItem.title, newItem.id),
                "setNewWallpaper()",
                LogEntry.LogLevel.Message);
        try {
            // get screen height (output wallpaper height)
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int screenHeight = size.y;

            // First decode with inJustDecodeBounds=true to check dimensions
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageDirectory + newItem.imageName, bitmapOptions);

            // Calculate inSampleSize
            int imageHeight = bitmapOptions.outHeight;
            int inSampleSize = 1;
            while (imageHeight > screenHeight) {
                imageHeight /= 2;
                inSampleSize *= 2;
            }

            // Decode bitmap with inSampleSize set
            bitmapOptions.inSampleSize = inSampleSize;
            bitmapOptions.inJustDecodeBounds = false;
            currentImage = BitmapFactory.decodeFile(imageDirectory + newItem.imageName, bitmapOptions);

            WallpaperManager myWallpaperManager = WallpaperManager.getInstance(this);
            if (setHomeWallpaper) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    myWallpaperManager.setBitmap(currentImage, null, true, WallpaperManager.FLAG_SYSTEM);
                } else {
                    myWallpaperManager.setBitmap(currentImage);
                }

                logEvent("Successfully set wallpaper.", "setNewWallpaper()", LogEntry.LogLevel.Message);
            }

            if (setLockWallpaper) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    myWallpaperManager.setBitmap(currentImage, null, true, WallpaperManager.FLAG_LOCK);
                    logEvent("Successfully set lock screen wallpaper.", "setNewWallpaper()", LogEntry.LogLevel.Message);
                } else {
                    Toast.makeText(this, "Not available before Android 7.0 Nougat.", Toast.LENGTH_SHORT).show();
                }
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

    private void logEvent(String message, String function, LogEntry.LogLevel level) {
        Log.d(TAG, function + ": " + message);
        LogDBHelper helper = LogDBHelper.getHelper(this, true);
        helper.saveLogEntry(message, null, TAG, function, level);
        helper.close();
    }

    private void logException(Exception e, String function) {
        Log.d(TAG, function + ": " + e.getLocalizedMessage());
        LogDBHelper helper = LogDBHelper.getHelper(this, true);
        helper.saveLogEntry(e.getLocalizedMessage(), ErrorHandler.getStackTraceString(e), TAG, function, LogEntry.LogLevel.Error);
        helper.close();
    }

    private TimerTask downloadRSSTask = new TimerTask() {
        @Override
        public void run() {
            startDownloadIntent();
        }
    };

    private void startDownloadIntent() {
        DownloadWallpaperService.startDownloadRSSAction(this, NASA_RSS_LINK_A);
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(changeWallpaperNow);

        timer.cancel();
        timer.purge();
        timer = null;
        super.onDestroy();
    }

    private BroadcastReceiver changeWallpaperNow = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            logEvent("Received changeWallpaperNow broadcast.",
                    "onReceive(Context context, Intent intent)",
                    LogEntry.LogLevel.Trace);
            setNewWallpaper();
        }
    };
}
