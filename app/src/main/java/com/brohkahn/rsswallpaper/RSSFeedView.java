package com.brohkahn.rsswallpaper;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.util.Locale;

public class RSSFeedView extends AppCompatActivity {
	public static final String TAG = "FeedItemView";
	public static final String EXTRA_KEY_FEED_ID = "feedID";

	private String title;
	private int id;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rss_feed_view);

		Toolbar toolbar = (Toolbar) findViewById(R.id.rss_feed_view_toolbar);
		setSupportActionBar(toolbar);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.rss_feed_view_fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				promptSettingFeed();
			}
		});

		id = getIntent().getIntExtra(EXTRA_KEY_FEED_ID, -1);

		FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(this);
		RSSFeed feed = feedDBHelper.getFeed(id);
		feedDBHelper.close();

		if (feed == null) {
			LogDBHelper logDBHelper = LogDBHelper.getHelper(getApplicationContext());
			logDBHelper.saveLogEntry(String.format(Locale.US, "Unable to find feed item with id of %d", id),
									 null,
									 TAG,
									 "onCreate(Bundle savedInstanceState)",
									 LogEntry.LogLevel.Warning
			);
			logDBHelper.close();
			finish();
			return;
		}


		title = feed.title;
		((TextView) findViewById(R.id.feed_title)).setText(title);
		((TextView) findViewById(R.id.feed_description)).setText(feed.description);
		((TextView) findViewById(R.id.feed_link)).setText(feed.link);

	}


	public void promptSettingFeed() {
		Resources resources = getResources();
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.set_feed_dialog_title)
			   .setMessage(String.format(resources.getString(R.string.set_feed_dialog_message), title))
			   .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				   @Override
				   public void onClick(DialogInterface dialog, int which) {
					   setNewFeed(id);
					   dialog.dismiss();
				   }
			   })
			   .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				   @Override
				   public void onClick(DialogInterface dialog, int which) {
					   dialog.dismiss();
				   }
			   });
		builder.create().show();
	}

	public void setNewFeed(int id) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this)
														   .edit();
		editor.putString(getResources().getString(R.string.key_current_feed), String.valueOf(id));
		editor.apply();

		// immediately get new images and icons
		DownloadImageService.startDownloadImageAction(this);
		DownloadIconService.startDownloadIconAction(this);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

}
