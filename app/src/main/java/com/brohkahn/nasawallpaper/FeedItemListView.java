package com.brohkahn.nasawallpaper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.util.Locale;

public class FeedItemListView extends AppCompatActivity {
    public static final String TAG = "FeedItemListView";

    public FeedItemListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feed_item_list_view);

        CursorLoader cursorLoader = new CursorLoader(getApplicationContext(),
                Uri.EMPTY,
                FeedDBHelper.FeedDBEntry.getAllColumns(),
                null,
                null,
                FeedDBHelper.FeedDBEntry.COLUMN_PUBLISHED + " DESC") {
            @Override
            public Cursor loadInBackground() {
                FeedDBHelper dbHelper = FeedDBHelper.getHelper(getApplicationContext(), false);
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                return db.query(FeedDBHelper.FeedDBEntry.TABLE_NAME,
                        getProjection(),
                        getSelection(),
                        getSelectionArgs(),
                        null,
                        null,
                        this.getSortOrder(),
                        "100");
            }
        };

        adapter = new FeedItemListAdapter(this, cursorLoader.loadInBackground(), 0);

        ListView listView = (ListView) findViewById(R.id.feed_item_list_view);
        listView.setAdapter(adapter);

    }


    @Override
    protected void onDestroy() {
        adapter.getCursor().close();
        adapter.changeCursor(null);
        adapter = null;
        super.onDestroy();
    }

    public class FeedItemListAdapter extends CursorAdapter {
        private String imageDirectory;
        private int imageDimension;

        public FeedItemListAdapter(Context context, Cursor cursor, int flags) {
            super(context, cursor, flags);

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            Resources resources = getResources();
            imageDirectory = preferences.getString(resources.getString(R.string.key_image_directory), getFilesDir().getPath() + "/");
            imageDimension = (int) resources.getDimension(R.dimen.icon_size);
        }

        public void bindView(View view, Context context, Cursor cursor) {
            final int itemID = cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBHelper.FeedDBEntry._ID));

            TextView titleTextView = (TextView) view.findViewById(R.id.feed_item_title);
            String title = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBHelper.FeedDBEntry.COLUMN_TITLE));
            titleTextView.setText(title);

            CheckBox checkBox = (CheckBox) view.findViewById(R.id.feed_item_enabled);
            boolean enabled = cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBHelper.FeedDBEntry.COLUMN_ENABLED)) == 1;
            checkBox.setChecked(enabled);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    updateItemEnabled(itemID, b);
                }
            });

            if (cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBHelper.FeedDBEntry.COLUMN_DOWNLOADED)) == 1) {
                // get bitmap
                String imageName = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBHelper.FeedDBEntry.COLUMN_IMAGE_NAME));

                // First decode with inJustDecodeBounds=true to check dimensions
                BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
                bitmapOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(imageDirectory + imageName, bitmapOptions);

                // Calculate inSampleSize
                int imageHeight = bitmapOptions.outHeight;
                int imageWidth = bitmapOptions.outWidth;
                int inSampleSize = 1;
                while (imageHeight > imageDimension && imageWidth > imageDimension) {
                    imageHeight /= 2;
                    imageWidth /= 2;
                    inSampleSize *= 2;
                }

                bitmapOptions.inSampleSize = inSampleSize;
                bitmapOptions.inJustDecodeBounds = false;

                // load imageView, scale bitmap, and set image
                ImageView imageView = (ImageView) view.findViewById(R.id.feed_item_icon);
                imageView.setImageBitmap(BitmapFactory.decodeFile(imageDirectory + imageName, bitmapOptions));
            } else {
                logEvent(String.format(Locale.US, "Item %s is not downloaded.", title),
                        "bindView(View view, Context context, Cursor cursor)",
                        LogEntry.LogLevel.Trace);
            }

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    displayFeedItem(itemID);
                }
            });
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.feed_item_list_item, parent, false);
        }
    }

    private void updateItemEnabled(int itemId, boolean enabled) {
        FeedDBHelper helper = FeedDBHelper.getHelper(getApplicationContext(), true);
        helper.updateImageEnabled(itemId, enabled);
        helper.close();
    }

    public void displayFeedItem(int ID) {
        Intent intent = new Intent(this, FeedItemView.class);
        intent.putExtra(FeedItemView.EXTRA_KEY_FEED_ITEM_ID, ID);
        startActivity(intent);
    }

    private void logEvent(String message, String function, LogEntry.LogLevel level) {
        Log.d(TAG, function + ": " + message);
        LogDBHelper helper = LogDBHelper.getHelper(getApplicationContext(), true);
        helper.saveLogEntry(message, null, TAG, function, level);
        helper.close();
    }
}
