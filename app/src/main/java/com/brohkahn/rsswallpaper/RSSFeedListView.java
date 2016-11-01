package com.brohkahn.rsswallpaper;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.brohkahn.loggerlibrary.LogEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RSSFeedListView extends AppCompatActivity {
	public static final String TAG = "RSSFeedListView";

	public RSSFeedListView.FeedListAdapter adapter;

	public int currentFeedId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rss_feed_list_view);

		Toolbar toolbar = (Toolbar) findViewById(R.id.rss_feed_list_view_toolbar);
		setSupportActionBar(toolbar);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.rss_feed_list_view_fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				updateFeedsFromJSON();

			}
		});

		adapter = new RSSFeedListView.FeedListAdapter(this, getCursorLoader().loadInBackground(), 0);

		ListView listView = (ListView) findViewById(R.id.feed_list_view);
		listView.setAdapter(adapter);
	}

	private CursorLoader getCursorLoader() {
		return new CursorLoader(getApplicationContext(),
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
	}


	@Override
	protected void onPause() {
		adapter.getCursor().close();

		DownloadImageService.startDownloadImageAction(this, false);

		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		Resources resources = getResources();
		currentFeedId = Integer.parseInt(settings.getString(resources.getString(R.string.key_current_feed), "1"));

		if (adapter.getCursor().isClosed()) {
			adapter.changeCursor(getCursorLoader().loadInBackground());
		}
//		adapter.notifyDataSetChanged();
	}


	@Override
	protected void onDestroy() {
		adapter.getCursor().close();
		adapter.changeCursor(null);
		adapter = null;

		super.onDestroy();
	}

	private void getNewCursor() {
		adapter.getCursor().close();
		adapter.changeCursor(getCursorLoader().loadInBackground());
	}

	public class FeedListAdapter extends CursorAdapter {
		private final int CURRENT_FEED_COLOR;
		private final int DEFAULT_FEED_COLOR;
		private final int CURRENT_FEED_TEXT_COLOR;
		private final int DEFAULT_FEED_TEXT_COLOR;

		private FeedListAdapter(Context context, Cursor cursor, int flags) {
			super(context, cursor, flags);

			CURRENT_FEED_COLOR = ContextCompat.getColor(context, R.color.current_feed);
			DEFAULT_FEED_COLOR = ContextCompat.getColor(context, R.color.transparent);
			CURRENT_FEED_TEXT_COLOR = ContextCompat.getColor(context, android.R.color.white);
			DEFAULT_FEED_TEXT_COLOR = ContextCompat.getColor(context, android.R.color.black);

			logEvent(String.format(Locale.US, "Displaying %d available feeds.", cursor.getCount()),
					"FeedListAdapter(Context context, Cursor cursor, int flags)",
					LogEntry.LogLevel.Trace
			);
		}

		public void bindView(View view, Context context, Cursor cursor) {
			boolean enabled = cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBHelper.FeedDBEntry.COLUMN_ENABLED)) == 1;
			view.setEnabled(enabled);
			if (enabled) {
				view.setAlpha(1);
			} else {
				view.setAlpha(33 / 100);
			}

			TextView titleTextView = (TextView) view.findViewById(R.id.feed_title);
			final String title = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBHelper.FeedDBEntry.COLUMN_TITLE));
			titleTextView.setText(title);

			final int id = cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBHelper.FeedDBEntry._ID));
			if (id == currentFeedId) {
				view.setBackgroundColor(CURRENT_FEED_COLOR);
				titleTextView.setTextColor(CURRENT_FEED_TEXT_COLOR);
			} else {
				view.setBackgroundColor(DEFAULT_FEED_COLOR);
				titleTextView.setTextColor(DEFAULT_FEED_TEXT_COLOR);
			}

			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					displayFeed(id);
				}
			});
		}

		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return LayoutInflater.from(context).inflate(R.layout.rss_feed_list_item, parent, false);
		}
	}

	private void displayFeed(int feedId) {
		Intent intent = new Intent(this, RSSFeedView.class);
		intent.putExtra(RSSFeedView.EXTRA_KEY_FEED_ID, feedId);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.feed_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
			case R.id.action_request_feed:
				requestFeed();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void requestFeed() {
		final EditText editText = new EditText(this);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.request_feed_dialog_title)
				.setMessage(R.string.request_feed_dialog_message)
				.setPositiveButton(R.string.request, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						sendRequestEmail(editText.getText().toString());
						dialog.dismiss();
					}
				})
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.setView(editText);
		builder.create().show();

	}

	private void sendRequestEmail(String requestUrl) {
		Resources resources = getResources();
		String subject = resources.getString(R.string.request_feed_email_subject);
		String body = resources.getString(R.string.request_feed_email_message, requestUrl);
		String developerEmail = resources.getString(R.string.developer_email);
		String chooserTitle = resources.getString(R.string.request_feed_dialog_message);

		Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
				"mailto", developerEmail, null));
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
		emailIntent.putExtra(Intent.EXTRA_TEXT, body);

		startActivity(Intent.createChooser(emailIntent, chooserTitle));
	}

	private void updateFeedsFromJSON() {
		String JSONLocation = "http://kevin.broh-kahn.com/assets/files/availableFeeds.txt";
		new UpdateJSONTask(this).execute(JSONLocation);
	}

	private static class UpdateJSONTask extends AsyncTask<String, Void, Long> {
		private RSSFeedListView containingActivity;

		private UpdateJSONTask(RSSFeedListView containingActivity) {
			this.containingActivity = containingActivity;
		}

		@Override
		protected Long doInBackground(String... params) {
			long result = 0;
			String JSONString = "";
			try {
				URL url = new URL(params[0]);
				URLConnection connection = url.openConnection();
				connection.connect();

				// download the file
				InputStream inputStream = new BufferedInputStream(url.openStream());

				BufferedReader bReader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"), 8);
				StringBuilder sBuilder = new StringBuilder();

				String line;
				while ((line = bReader.readLine()) != null) {
					sBuilder.append(line);
					sBuilder.append('\n');
				}

				inputStream.close();
				JSONString = sBuilder.toString();
			} catch (IOException e) {
				containingActivity.logException(e, "doInBackground(String... params)");
				Log.e("JSONException", "Error: " + e.toString());
			}

			try {
				JSONObject fullObject = new JSONObject(JSONString);
				JSONArray feedItemArray = fullObject.getJSONArray("availableFeeds");

				List<RSSFeed> feedsToUpdate = new ArrayList<>(Constants.SUPPORTED_FEED_COUNT);

				for (int i = 0; i < feedItemArray.length(); i++) {
					JSONObject feedJSONObject = feedItemArray.getJSONObject(i);

					String source = feedJSONObject.getString("source");
					String title = feedJSONObject.getString("title");
					boolean enabled = feedJSONObject.getBoolean("enabled");
					boolean imageOnWebPage = feedJSONObject.getBoolean("imageOnWebPage");
					String entryImageLinkTag = feedJSONObject.getString("entryImageLinkTag");
					String entryImageLinkAttribute = feedJSONObject.getString("entryImageLinkAttribute");

					RSSFeed feed = new RSSFeed(-1,
							source,
							title,
							entryImageLinkTag,
							entryImageLinkAttribute,
							imageOnWebPage,
							enabled
					);

					feedsToUpdate.add(feed);
				}

				FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(containingActivity);
				result = feedDBHelper.saveNewFeeds(feedsToUpdate);
				feedDBHelper.close();
			} catch (JSONException e) {
				containingActivity.logException(e, "doInBackground(String... params)");
				Log.e("JSONException", "Error: " + e.toString());
			}

			return result;
		}

		@Override
		protected void onPostExecute(Long result) {
			String messageString;
			if (result == 0) {
				messageString = "No new feeds found, select \"Request new RSSFeed\" in the menu to send the developer a request for a new feed.";
			} else {
				messageString = String.format(Locale.US, "Found %d new feeds, saving info.", result);

				LocalBroadcastManager.getInstance(containingActivity).sendBroadcast(new Intent(Constants.BROADCAST_DOWNLOAD_RSS));

			}

			containingActivity.logEvent(String.format(Locale.US, messageString, result),
					"onPostExecute(Integer result)",
					LogEntry.LogLevel.Trace
			);
			Toast.makeText(containingActivity, messageString, Toast.LENGTH_SHORT).show();

			containingActivity.getNewCursor();
			containingActivity = null;
			super.onPostExecute(result);
		}
	}

	private void logException(Exception e, String function) {
		((MyApplication) getApplication()).logException(e, function, TAG);
	}

	private void logEvent(String message, String function, LogEntry.LogLevel level) {
		((MyApplication) getApplication()).logEvent(message, function, TAG, level);
	}

}
