package com.brohkahn.rsswallpaper;

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
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.brohkahn.loggerlibrary.LogEntry;

import java.util.Locale;

public class FeedItemListView extends AppCompatActivity {
	public static final String TAG = "FeedItemListView";

	public FeedItemListAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feed_item_list_view);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		Resources resources = getResources();
		int currentFeedId = Integer.parseInt(settings.getString(resources.getString(R.string.key_current_feed), "0"));

		CursorLoader cursorLoader = new CursorLoader(getApplicationContext(),
													 Uri.EMPTY,
													 FeedDBHelper.FeedItemDBEntry.getAllColumns(),
													 String.format(Locale.US, "%s=%d", FeedDBHelper.FeedItemDBEntry.COLUMN_RELATED_FEED, currentFeedId),
													 null,
													 FeedDBHelper.FeedItemDBEntry.COLUMN_CREATION_DATE + " DESC"
		) {
			@Override
			public Cursor loadInBackground() {
				FeedDBHelper dbHelper = FeedDBHelper.getHelper(getApplicationContext());
				SQLiteDatabase db = dbHelper.getReadableDatabase();
				return db.query(FeedDBHelper.FeedItemDBEntry.TABLE_NAME,
								getProjection(),
								getSelection(),
								getSelectionArgs(),
								null,
								null,
								this.getSortOrder(),
								"100"
				);
			}
		};

		adapter = new FeedItemListAdapter(this, cursorLoader.loadInBackground(), 0);

		ListView listView = (ListView) findViewById(R.id.feed_item_list_view);
		listView.setAdapter(adapter);
	}

	@Override
	protected void onPause() {
		DownloadImageService.startDownloadImageAction(this);

		super.onPause();
	}

	@Override
	protected void onDestroy() {
		adapter.getCursor().close();
		adapter.changeCursor(null);
		adapter = null;
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public class FeedItemListAdapter extends CursorAdapter {
		private String imageDirectory;
		private int imageDimension;

		private FeedItemListAdapter(Context context, Cursor cursor, int flags) {
			super(context, cursor, flags);

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			Resources resources = getResources();
			imageDirectory = preferences.getString(resources.getString(R.string.key_image_directory), getFilesDir()
					.getPath() + "/");
			imageDimension = (int) resources.getDimension(R.dimen.icon_size);
		}

		public void bindView(View view, Context context, Cursor cursor) {
			final int itemID = cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBHelper.FeedItemDBEntry._ID));

			TextView titleTextView = (TextView) view.findViewById(R.id.feed_item_title);
			String title = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBHelper.FeedItemDBEntry.COLUMN_TITLE));
			titleTextView.setText(title);

			CheckBox checkBox = (CheckBox) view.findViewById(R.id.feed_item_enabled);
			boolean enabled = cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBHelper.FeedItemDBEntry.COLUMN_ENABLED)) == 1;
			checkBox.setChecked(enabled);
			checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					updateItemEnabled(itemID, b);
				}
			});

			if (cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBHelper.FeedItemDBEntry.COLUMN_DOWNLOADED)) == 1) {
				// get image name and path
				String imageName = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBHelper.FeedItemDBEntry.COLUMN_IMAGE_NAME));
				String imagePath = imageDirectory + imageName;

				// get bitmap scale
				BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
				bitmapOptions.inSampleSize = Constants.getImageScale(imagePath, imageDimension, imageDimension);

				// load imageView, scale bitmap, and set image
				ImageView imageView = (ImageView) view.findViewById(R.id.feed_item_icon);
				imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath, bitmapOptions));
			} else {
				logEvent(String.format(Locale.US, "Item %s is not downloaded.", title),
						 "bindView(View view, Context context, Cursor cursor)",
						 LogEntry.LogLevel.Trace
				);
			}

			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					displayFeedItem(itemID);
				}
			});
		}

		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return LayoutInflater.from(context)
								 .inflate(R.layout.feed_item_list_item, parent, false);
		}
	}

	private void updateItemEnabled(int itemId, boolean enabled) {
		FeedDBHelper helper = FeedDBHelper.getHelper(getApplicationContext());
		helper.updateImageEnabled(itemId, enabled);
		helper.close();
	}

	public void displayFeedItem(int ID) {
		Intent intent = new Intent(this, FeedItemView.class);
		intent.putExtra(FeedItemView.EXTRA_KEY_FEED_ITEM_ID, ID);
		startActivity(intent);
	}

	private void logEvent(String message, String function, LogEntry.LogLevel level) {
		((MyApplication) getApplication()).logEvent(message, function, TAG, level);
	}
}
