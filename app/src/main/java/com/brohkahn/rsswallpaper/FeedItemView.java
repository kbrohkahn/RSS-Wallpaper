package com.brohkahn.rsswallpaper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
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

	private String imageDirectory;

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

		// set icon
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

		Resources resources = getResources();
		imageDirectory = Helpers.getStoragePath(this,
				preferences.getString(resources.getString(R.string.key_image_storage), "LOCAL"));

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

		((TextView) findViewById(R.id.feed_item_title)).setText(item.title);

		String publishedText = getResources().getString(R.string.downloaded_on, item.creationDate.toString());
		TextView publishedTextView = (TextView) findViewById(R.id.feed_item_published);
		publishedTextView.setText(publishedText);

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.feed_item_view_fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				setAsCurrentWallpaper();
			}
		});

		// TODO
		fab.setEnabled(false);
		fab.setVisibility(View.GONE);

		enabledCheckBox = (CheckBox) findViewById(R.id.feed_item_enabled);
		enabledCheckBox.setChecked(item.enabled);

		CheckBox downloadedCheckBox = (CheckBox) findViewById(R.id.feed_item_downloaded);
		downloadedCheckBox.setChecked(item.imageIsDownloaded(imageDirectory));

		// load imageView, scale bitmap, and set image
		String imagePath = imageDirectory + item.getImageName();
		ImageView imageView = (ImageView) findViewById(R.id.feed_item_image);
		imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath));

	}

//	public void openInFileExplorer() {
//		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//		Uri uri = Uri.parse(Environment.getExternalStorageDirectory().getPath()
//				+ "/myFolder/");
//		intent.setDataAndType(uri, "text/csv");
//		startActivity(Intent.createChooser(intent, "Show file on device"));
//
//	}

	public void openLink(View view) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(item.link));
		startActivity(intent);
	}

	private void setAsCurrentWallpaper() {
		enabledCheckBox.setChecked(true);

		if (!item.imageIsDownloaded(imageDirectory)) {
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

			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
			Resources resources = getResources();
			int currentFeedId = Integer.parseInt(settings.getString(resources.getString(R.string.key_current_feed),
					"-1"));
			if (item.id == currentFeedId) {
				Intent newIntent = new Intent(this, ChangeWallpaperService.class);
				newIntent.setAction(Constants.ACTION_CHANGE_WALLPAPER);
				startService(newIntent);
			}

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
