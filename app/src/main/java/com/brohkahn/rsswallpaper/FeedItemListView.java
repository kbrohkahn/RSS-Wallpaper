package com.brohkahn.rsswallpaper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Locale;

public class FeedItemListView extends AppCompatActivity {
//	public static final String TAG = "FeedItemListView";

	public FeedItemListAdapter adapter;
	private int currentFeedId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feed_item_list_view);

		Toolbar toolbar = (Toolbar) findViewById(R.id.feed_item_list_view_toolbar);
		setSupportActionBar(toolbar);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		Resources resources = getResources();
		currentFeedId = Integer.parseInt(settings.getString(resources.getString(R.string.key_current_feed), "-1"));

		CursorLoader cursorLoader = getCursorLoader();

		adapter = new FeedItemListAdapter(this, cursorLoader.loadInBackground(), 0);

		ListView listView = (ListView) findViewById(R.id.feed_item_list_view);
		listView.setAdapter(adapter);
	}

	private CursorLoader getCursorLoader() {

		return new CursorLoader(getApplicationContext(),
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
	}

	@Override
	protected void onPause() {
		adapter.getCursor().close();

		DownloadImageService.startDownloadImageAction(this);

		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		adapter.changeCursor(getCursorLoader().loadInBackground());
//		adapter.notifyDataSetChanged();
	}

//	@Override
//	protected void onDestroy() {
//		adapter.changeCursor(null);
//		adapter = null;
//		super.onDestroy();
//	}

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

		private FeedItemListAdapter(Context context, Cursor cursor, int flags) {
			super(context, cursor, flags);

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			Resources resources = getResources();
			imageDirectory = preferences.getString(resources.getString(R.string.key_image_directory),
					Helpers.getDefaultFolder(context));
		}

		public void bindView(View view, Context context, Cursor cursor) {
			final int id = cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBHelper.FeedItemDBEntry._ID));
			String title = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBHelper.FeedItemDBEntry.COLUMN_TITLE));
			String imageLink = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBHelper.FeedItemDBEntry.COLUMN_IMAGE_LINK));
			boolean enabled = cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBHelper.FeedItemDBEntry.COLUMN_ENABLED)) == 1;

			final FeedItem item = new FeedItem(id, -1, title, null, null, imageLink, enabled, null);

			TextView titleTextView = (TextView) view.findViewById(R.id.feed_item_title);
			titleTextView.setText(title);

			CheckBox checkBox = (CheckBox) view.findViewById(R.id.feed_item_enabled);
			checkBox.setChecked(enabled);
			checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					updateItemEnabled(id, b);
				}
			});

			ImageView imageView = (ImageView) view.findViewById(R.id.feed_item_icon);
			String imagePath = imageDirectory + Constants.ICONS_FOLDER + item.getIconName();
			Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
			if (bitmap == null) {
				imageView.setVisibility(View.GONE);
				imageView.setImageBitmap(null);
			} else {
				imageView.setVisibility(View.VISIBLE);
				imageView.setImageBitmap(bitmap);
			}

			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					displayFeedItem(id);
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

		if (itemId == currentFeedId) {
			DownloadImageService.startDownloadImageAction(this);
		}

	}

	public void displayFeedItem(int ID) {
		Intent intent = new Intent(this, FeedItemView.class);
		intent.putExtra(FeedItemView.EXTRA_KEY_FEED_ITEM_ID, ID);
		startActivity(intent);
	}

//	private void logEvent(String message, String function, LogEntry.LogLevel level) {
//		((MyApplication) getApplication()).logEvent(message, function, TAG, level);
//	}
}
