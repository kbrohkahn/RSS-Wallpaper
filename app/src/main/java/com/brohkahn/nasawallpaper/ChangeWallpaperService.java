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
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.brohkahn.loggerlibrary.ErrorHandler;
import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class ChangeWallpaperService extends Service {
    private static final String TAG = "ChangeWallpaperService";

    private static final String NASA_RSS_LINK_A = "http://www.nasa.gov/rss/dyn/lg_image_of_the_day.rss";
//    private static final String NASA_RSS_LINK_B = "http://apod.nasa.gov/apod.rss";

    private List<FeedItem> itemsToShuffle;
    private int currentFileIndex = 0;

    private int numberToShuffle;
    private boolean setHomeWallpaper;
    private boolean setLockWallpaper;

    private static final int changeWallpaperInterval = 60 * 60 * 1000;
    private static final int downloadWallpaperInterval = 24 * 60 * 60 * 1000;
    private Random random = new Random();
    private Timer timer;
    ;

    public ChangeWallpaperService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Thread.setDefaultUncaughtExceptionHandler(new ErrorHandler(this, false));

        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();

        int internetPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        if (internetPermissionCheck != PackageManager.PERMISSION_GRANTED) {
            startActivity(new Intent(this, MainActivity.class));
            return START_NOT_STICKY;
        }

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final Resources resources = getResources();

        numberToShuffle = preferences.getInt(resources.getString(R.string.key_number_to_shuffle), 7);
        setHomeWallpaper = preferences.getBoolean(resources.getString(R.string.key_set_home_screen), true);
        setLockWallpaper = preferences.getBoolean(resources.getString(R.string.key_set_home_screen), false);

        // instantiate file list
        updateItemList();

        // immediately update wallpaper if we have images
        if (itemsToShuffle.size() > 0) {
            startTimerAndRunNow();
        }

        // setup receiver
        // The filter's action is BROADCAST_ACTION
        IntentFilter mStatusIntentFilter = new IntentFilter(Constants.DOWNLOAD_COMPLETE_BROADCAST);

        // Adds a data filter for the HTTP scheme
        mStatusIntentFilter.addDataScheme("http");

        // Registers the DownloadStateReceiver and its intent filters
        ResponseReceiver mDownloadStateReceiver = new ResponseReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(mDownloadStateReceiver, mStatusIntentFilter);

        // start download
        startDownloadIntent();

        // download wallpaper daily at 0300
        Calendar downloadTime = Calendar.getInstance();
        downloadTime.set(Calendar.DAY_OF_YEAR, downloadTime.get(Calendar.DAY_OF_YEAR) + 1);
        downloadTime.set(Calendar.HOUR_OF_DAY, 3);
        downloadTime.set(Calendar.MINUTE, 0);
        downloadTime.set(Calendar.SECOND, 0);
        downloadTime.set(Calendar.MILLISECOND, 0);
        timer.scheduleAtFixedRate(downloadRSSTask, downloadTime.getTime(), downloadWallpaperInterval);

        return super.onStartCommand(intent, flags, startId);
    }

    public void startTimerAndRunNow() {
        logEvent("Starting wallpaper timer.", "startTimerAndRunNow()");

        timer.scheduleAtFixedRate(changeWallpaperTask, 0, changeWallpaperInterval);
    }

    private TimerTask changeWallpaperTask = new TimerTask() {
        @Override
        public void run() {
            setNewWallpaper();
        }
    };

    private void setNewWallpaper() {
        logEvent("Setting new wallpaper.", "setNewWallpaper()");

        int newIndex = currentFileIndex;
        int fileCount = itemsToShuffle.size();
        while (fileCount > 1 && newIndex == currentFileIndex) {
            newIndex = random.nextInt(fileCount);
        }
        currentFileIndex = newIndex;

        WallpaperManager myWallpaperManager = WallpaperManager.getInstance(getApplicationContext());
        String fileName = itemsToShuffle.get(newIndex).imageName;
        Bitmap newWallpaper = BitmapFactory.decodeFile(getFilesDir().getPath() + "/" + fileName);

        try {
            logEvent(String.format("Setting wallpaper to %s.", itemsToShuffle.get(newIndex)), "setNewWallpaper()");

            if (setHomeWallpaper) {
                myWallpaperManager.setBitmap(newWallpaper);
            }

            if (setLockWallpaper && Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
                myWallpaperManager.setBitmap(newWallpaper, null, true, WallpaperManager.FLAG_LOCK);
            }

        } catch (IOException e) {
            e.printStackTrace();
            logException(e, "setNewWallpaper()");
        }
    }

    private void logEvent(String message, String function) {
        LogDBHelper.saveLogEntry(this, message, null, TAG, function, LogEntry.LogLevel.Trace);
    }

    private void logException(Exception e, String function) {
        LogDBHelper.saveLogEntry(this, e.getLocalizedMessage(), ErrorHandler.getStackTraceString(e), TAG, function, LogEntry.LogLevel.Error);
    }

    private void updateItemList() {
        logEvent("Updating list of items.", "updateItemList()");
        itemsToShuffle = FeedDBHelper.getRecentItems(this, numberToShuffle);
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
        timer.cancel();
        super.onDestroy();
    }

    // Broadcast receiver for receiving status updates from the IntentService
    private class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean noFilesInitially = itemsToShuffle.size() == 0;

            updateItemList();

            if (noFilesInitially) {
                startTimerAndRunNow();
            }
        }
    }
}
