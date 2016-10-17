package com.brohkahn.nasawallpaper;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.brohkahn.loggerlibrary.ErrorHandler;
import com.brohkahn.loggerlibrary.LogViewList;

public class MainActivity extends AppCompatActivity {
    private final int REQUEST_INTERNET_PERMISSION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new ErrorHandler(this, true));

        setContentView(R.layout.activity_main);

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            showPermissionDialog();
        }

        final String keyCurrentItem = getResources().getString(R.string.key_current_item);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        int currentItemId = settings.getInt(keyCurrentItem, 0);

        FeedItem currentItem = FeedDBHelper.getFeedItem(this, currentItemId);
        if (currentItem != null) {
            ((TextView) findViewById(R.id.current_item_title)).setText(currentItem.title);
            ((TextView) findViewById(R.id.current_item_published)).setText(currentItem.published.toString());
            ((TextView) findViewById(R.id.current_item_link)).setText(currentItem.link);

            Bitmap image = BitmapFactory.decodeFile(getFilesDir().getPath() + "/" + currentItem.imageName);

            ((ImageView) findViewById(R.id.current_item_image)).setImageBitmap(image);
        }

    }

    public void restartServiceButtonClick(View view) {
        restartService();
    }

    public void restartService() {
        Intent serviceIntent = new Intent(this, ChangeWallpaperService.class);
        stopService(serviceIntent);
        startService(serviceIntent);
    }

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
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, REQUEST_INTERNET_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_INTERNET_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    restartService();
                } else {
                    showPermissionDialog();
                }
            }
        }
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
            case R.id.action_settings:
                startActivity(new Intent(this, LogViewList.class));
                return true;
            case R.id.action_about:
                startActivity(new Intent(this, About.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
