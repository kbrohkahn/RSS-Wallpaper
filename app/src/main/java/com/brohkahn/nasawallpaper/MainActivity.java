package com.brohkahn.nasawallpaper;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;
import com.brohkahn.loggerlibrary.LogViewList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private final int REQUEST_PERMISSIONS = 0;

    private int currentItemId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        Thread.setDefaultUncaughtExceptionHandler(new ErrorHandler(this, true));

        setContentView(R.layout.activity_main);

        int internetPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        int wallpaperPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.SET_WALLPAPER);
        if (internetPermission != PackageManager.PERMISSION_GRANTED || wallpaperPermission != PackageManager.PERMISSION_GRANTED) {
            logEvent("Missing permissions.", "onCreate(Bundle savedInstanceState)", LogEntry.LogLevel.Message);

            showPermissionDialog();
        } else {
            updateCurrentItem(true);
        }
    }

    @Override
    protected void onDestroy() {
        if (ChangeWallpaperService.currentImage != null) {
            ChangeWallpaperService.currentImage.recycle();
            ChangeWallpaperService.currentImage = null;
        }
        super.onDestroy();
    }

    public void updateCurrentItem(boolean reloadImage) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Resources resources = getResources();
        currentItemId = settings.getInt(resources.getString(R.string.key_current_item), 0);
        String imageDirectory = settings.getString(resources.getString(R.string.key_image_directory), getFilesDir().getPath() + "/");

        FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(getApplicationContext(), false);
        FeedItem currentItem = feedDBHelper.getFeedItem(currentItemId);
        feedDBHelper.close();

        if (currentItem != null) {
            ((TextView) findViewById(R.id.current_item_title)).setText(currentItem.title);
            ((TextView) findViewById(R.id.current_item_published)).setText(currentItem.published.toString());
            ((TextView) findViewById(R.id.current_item_link)).setText(currentItem.link);

            if (ChangeWallpaperService.currentImage == null) {
                reloadImage = true;
            } else if (ChangeWallpaperService.currentImage.isRecycled()) {
                reloadImage = true;
            }

            if (reloadImage) {
                ChangeWallpaperService.currentImage = BitmapFactory.decodeFile(imageDirectory + currentItem.imageName);
            }

            ((ImageView) findViewById(R.id.current_item_image)).setImageBitmap(ChangeWallpaperService.currentImage);
        } else {
            ((TextView) findViewById(R.id.current_item_title)).setText("");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter mStatusIntentFilter = new IntentFilter(Constants.WALLPAPER_UPDATED);
        LocalBroadcastManager.getInstance(this).registerReceiver(wallpaperUpdated, mStatusIntentFilter);
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(wallpaperUpdated);
        super.onPause();
    }

    private BroadcastReceiver wallpaperUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            logEvent("Received wallpaperUpdated broadcast.",
                    "onReceive(Context context, Intent intent)",
                    LogEntry.LogLevel.Trace);

            updateCurrentItem(false);
        }
    };

    public void showPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.permission_request_title)
                .setMessage(R.string.permission_request_message)
                .setPositiveButton(R.string.permission_request_positive_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        requestInternetPermission();
                    }
                })
                .setNegativeButton(R.string.permission_request_negative_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Uri packageUri = Uri.parse("package:" + getApplicationContext().getPackageName());
                        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
                        startActivity(uninstallIntent);
                    }
                });
        builder.create().show();
    }

    private void requestInternetPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET, Manifest.permission.SET_WALLPAPER}, REQUEST_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    logEvent("All permissions received :)",
                            "onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)",
                            LogEntry.LogLevel.Message);

                    restartService();
                } else {
                    showPermissionDialog();
                }
            }
        }
    }

    public void restartService() {
        logEvent("Restarting service.", "restartService()", LogEntry.LogLevel.Message);
        Intent serviceIntent = new Intent(this, ChangeWallpaperService.class);
        stopService(serviceIntent);
        startService(serviceIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_view_items_all:
                startActivity(new Intent(this, FeedItemListView.class));
                return true;
            case R.id.action_view_logs:
                startActivity(new Intent(this, LogViewList.class));
                return true;
            case R.id.action_about:
                startActivity(new Intent(this, About.class));
                return true;
            case R.id.action_restart:
                restartService();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void getNewWallpaper(View view) {
        getNewWallpaper();
    }

    public void blockCurrentWallpaper(View view) {
        logEvent("Disabling current item", "onOptionsItemSelected(MenuItem item)", LogEntry.LogLevel.Trace);

        FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(getApplicationContext(), true);
        feedDBHelper.updateImageEnabled(currentItemId, false);
        feedDBHelper.close();

        getNewWallpaper();
    }

    private void getNewWallpaper() {
        logEvent("Sending set wallpaper broadcast", "onOptionsItemSelected(MenuItem item)", LogEntry.LogLevel.Trace);

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Constants.SET_WALLPAPER_ACTION);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

            }
        });

    }

//    private void showToast(String message) {
//        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
//    }

    private void logEvent(String message, String function, LogEntry.LogLevel level) {
        LogDBHelper logDBHelper = LogDBHelper.getHelper(this, true);
        logDBHelper.saveLogEntry(message, null, TAG, function, level);
        logDBHelper.close();
    }
}
