package com.brohkahn.rsswallpaper;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.util.Locale;

public class FeedItemView extends AppCompatActivity {
	public static final String TAG = "FeedItemView";
	public static final String EXTRA_KEY_FEED_ITEM_ID = "feedItemID";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.feed_item_view);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		final int ID = getIntent().getIntExtra(EXTRA_KEY_FEED_ITEM_ID, -1);

		FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(getApplicationContext());
		FeedItem item = feedDBHelper.getFeedItem(ID);
		feedDBHelper.close();

		if (item == null) {
			LogDBHelper logDBHelper = LogDBHelper.getHelper(getApplicationContext());
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

		setTitle(item.title);

		TextView linkTextView = (TextView) findViewById(R.id.feed_item_link);
		linkTextView.setText(item.link);

		TextView publishedTextView = (TextView) findViewById(R.id.feed_item_published);
		publishedTextView.setText(item.creationDate.toString());

		CheckBox enabledCheckBox = (CheckBox) findViewById(R.id.feed_item_enabled);
		enabledCheckBox.setChecked(item.enabled);
		enabledCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
				updateItemEnabled(ID, b);

			}
		});

		CheckBox downloadedCheckBox = (CheckBox) findViewById(R.id.feed_item_downloaded);
		downloadedCheckBox.setChecked(item.downloaded);
	}

	@Override
	protected void onPause() {
		DownloadImageService.startDownloadImageAction(this);

		super.onPause();
	}

	private void updateItemEnabled(int itemId, boolean enabled) {
		FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(getApplicationContext());
		feedDBHelper.updateImageEnabled(itemId, enabled);
		feedDBHelper.close();

		DownloadImageService.startDownloadImageAction(this);
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
}