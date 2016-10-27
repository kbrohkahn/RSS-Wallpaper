package com.brohkahn.rsswallpaper;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.util.Locale;

public class FeedItemView extends AppCompatActivity {
	public static final String TAG = "FeedItemView";
	public static final String EXTRA_KEY_FEED_ITEM_ID = "feedItemID";

	private boolean initiallyEnabled;
	private FeedItem item;
	private CheckBox enabledCheckBox;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.feed_item_view);

		Toolbar toolbar = (Toolbar) findViewById(R.id.feed_item_view_toolbar);
		setSupportActionBar(toolbar);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		final int ID = getIntent().getIntExtra(EXTRA_KEY_FEED_ITEM_ID, -1);

		FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(this);
		item = feedDBHelper.getFeedItem(ID);
		feedDBHelper.close();

		if (item == null) {
			LogDBHelper logDBHelper = LogDBHelper.getHelper(this);
			logDBHelper.saveLogEntry(String.format(Locale.US, "Unable to find feed item with id of %d", ID),
									 null,
									 TAG,
									 "onCreate(Bundle savedInstanceState)",
									 LogEntry.LogLevel.Warning
			);
			logDBHelper.close();
			finish();
			return;
		}

		initiallyEnabled = item.enabled;

		setTitle(item.title);

		TextView linkTextView = (TextView) findViewById(R.id.feed_item_link);
		linkTextView.setText(item.link);

		TextView publishedTextView = (TextView) findViewById(R.id.feed_item_published);
		publishedTextView.setText(item.creationDate.toString());

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.feed_item_view_fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				setAsCurrentWallpaper();
			}
		});

		enabledCheckBox = (CheckBox) findViewById(R.id.feed_item_enabled);
		enabledCheckBox.setChecked(item.enabled);

		CheckBox downloadedCheckBox = (CheckBox) findViewById(R.id.feed_item_downloaded);
		downloadedCheckBox.setChecked(item.downloaded);
	}

	private void setAsCurrentWallpaper() {
		enabledCheckBox.setChecked(true);

		if (!item.downloaded) {
			if (!item.enabled) {
				FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(getApplicationContext());
				feedDBHelper.updateImageEnabled(item.id, true);
				feedDBHelper.close();
			}

			DownloadImageService.startDownloadImageAction(this);
		}
	}

	@Override
	protected void onPause() {
		boolean isEnabled = enabledCheckBox.isChecked();
		if (isEnabled != initiallyEnabled) {
			FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(getApplicationContext());
			feedDBHelper.updateImageEnabled(item.id, isEnabled);
			feedDBHelper.close();

			DownloadImageService.startDownloadImageAction(this);
		}

		super.onPause();
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
