package com.brohkahn.nasawallpaper;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.brohkahn.loggerlibrary.LogEntry;

import java.util.Locale;

public class FeedListView extends AppCompatActivity {
	public static final String TAG = "FeedListView";

	public FeedListView.FeedListAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feed_list_view);

		CursorLoader cursorLoader = new CursorLoader(getApplicationContext(),
													 Uri.EMPTY,
													 FeedDBHelper.FeedDBEntry.getAllColumns(),
													 null,
													 null,
													 FeedDBHelper.FeedDBEntry.COLUMN_TITLE + " ASC"
		) {
			@Override
			public Cursor loadInBackground() {
				FeedDBHelper dbHelper = FeedDBHelper.getHelper(getApplicationContext());
				SQLiteDatabase db = dbHelper.getReadableDatabase();
				return db.query(FeedDBHelper.FeedDBEntry.TABLE_NAME,
								getProjection(),
								getSelection(),
								getSelectionArgs(),
								null,
								null,
								this.getSortOrder()
				);
			}
		};

		adapter = new FeedListView.FeedListAdapter(this, cursorLoader.loadInBackground(), 0);

		ListView listView = (ListView) findViewById(R.id.feed_list_view);
		listView.setAdapter(adapter);
	}

	@Override
	protected void onDestroy() {
		adapter.getCursor().close();
		adapter.changeCursor(null);
		adapter = null;
		super.onDestroy();
	}

	public class FeedListAdapter extends CursorAdapter {
		private FeedListAdapter(Context context, Cursor cursor, int flags) {
			super(context, cursor, flags);
			logEvent(String.format(Locale.US, "Displaying %d available feeds.", cursor.getCount()),
					 "FeedListAdapter(Context context, Cursor cursor, int flags)",
					 LogEntry.LogLevel.Trace
			);
		}

		public void bindView(View view, Context context, Cursor cursor) {
			TextView titleTextView = (TextView) view.findViewById(R.id.feed_title);
			final String title = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBHelper.FeedDBEntry.COLUMN_TITLE));
			titleTextView.setText(title);

			boolean enabled = cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBHelper.FeedDBEntry.COLUMN_ENABLED)) == 1;
			view.setEnabled(enabled);
			if (enabled) {
				view.setAlpha(1);
			} else {
				view.setAlpha(33 / 100);
			}

			final int id = cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBHelper.FeedDBEntry._ID));
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					promptSettingFeed(id, title);
				}
			});
		}

		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return LayoutInflater.from(context).inflate(R.layout.feed_list_item, parent, false);
		}
	}

	public void promptSettingFeed(int id, String title) {
		Resources resources = getResources();
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.set_feed_dialog_title)
			   .setMessage(String.format(resources.getString(R.string.set_feed_dialog_message), title))
			   .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				   @Override
				   public void onClick(DialogInterface dialog, int which) {

				   }
			   })
			   .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				   @Override
				   public void onClick(DialogInterface dialog, int which) {

				   }
			   });
		builder.create().show();
	}

	public void setNewFeed(int id) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this)
														   .edit();
		editor.putInt(getResources().getString(R.string.key_current_feed), id);
		editor.apply();
	}

	private void logEvent(String message, String function, LogEntry.LogLevel level) {
		((MyApplication) getApplication()).logEvent(message, function, TAG, level);
	}
}
