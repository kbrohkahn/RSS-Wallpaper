package com.brohkahn.rsswallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.brohkahn.loggerlibrary.LogEntry;
import com.brohkahn.loggerlibrary.LogViewList;

public class MainActivity extends AppCompatActivity {
	private static final String TAG = "MainActivity";

	private int currentItemId;

	private ImageView imageView;

	private Button blockWallpaperButton;
	private Button nextWallpaperButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//        Thread.setDefaultUncaughtExceptionHandler(new ErrorHandler(this, true));

		setContentView(R.layout.activity_main);

		Toolbar toolbar = (Toolbar) findViewById(R.id.activity_main_toolbar);
		setSupportActionBar(toolbar);

		blockWallpaperButton = (Button) findViewById(R.id.block_wallpaper_button);
		nextWallpaperButton = (Button) findViewById(R.id.next_wallpaper_button);

		imageView = ((ImageView) findViewById(R.id.current_item_image));

		// make sure current feed id is valid
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		Resources resources = getResources();
		int currentFeedId = Integer.parseInt(preferences.getString(resources.getString(R.string.key_current_feed), "-1"));

		// check if any feeds exist, if not save defaults
		FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(this);
		if (feedDBHelper.getFeed(currentFeedId) == null) {
			currentFeedId = feedDBHelper.getAllFeeds().get(0).id;
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString(resources.getString(R.string.key_current_feed), String.valueOf(currentFeedId));
			editor.apply();
		}

		// check if we have no initial items before closing helper
		boolean noInitialItems = feedDBHelper.getAllItems().size() == 0;
		feedDBHelper.close();

		// if no initial items, we need to download and restart timers
		if (noInitialItems) {
			Intent downloadRSSIntent = new Intent(this, DownloadRSSService.class);
			downloadRSSIntent.setAction(Constants.ACTION_DOWNLOAD_RSS);
			startService(downloadRSSIntent);

			Intent newIntent = new Intent(this, ScheduleTimerService.class);
			newIntent.setAction(Constants.ACTION_SCHEDULE_ALARMS);
			startService(newIntent);

		} else {
			updateCurrentItem();
		}

		// listen for wallpaper updates while active
		IntentFilter mStatusIntentFilter = new IntentFilter(Constants.ACTION_WALLPAPER_UPDATED);
		registerReceiver(wallpaperUpdated, mStatusIntentFilter);
	}

	@Override
	protected void onDestroy() {
		recycleCurrentBitmap();

		unregisterReceiver(wallpaperUpdated);
		super.onDestroy();
	}

	public void recycleCurrentBitmap() {
		Drawable drawable = imageView.getDrawable();
		if (drawable instanceof BitmapDrawable) {
			Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
			if (bitmap != null) {
				bitmap.recycle();
			}
		}
		imageView.setImageBitmap(null);
	}

	private BroadcastReceiver wallpaperUpdated = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			logEvent("Received wallpaperUpdated broadcast.",
					"onReceive(Context context, Intent intent)",
					LogEntry.LogLevel.Trace
			);

			updateCurrentItem();
		}
	};

	public void updateCurrentItem() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		Resources resources = getResources();
		currentItemId = settings.getInt(resources.getString(R.string.key_current_item), -1);
		String imageDirectory = settings.getString(resources.getString(R.string.key_image_directory),
				Helpers.getDefaultFolder(this));
		int currentFeedId = Integer.parseInt(settings.getString(resources.getString(R.string.key_current_feed), "-1"));

		FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(getApplicationContext());
		FeedItem currentItem = feedDBHelper.getFeedItem(currentItemId);
		RSSFeed currentFeed = feedDBHelper.getFeed(currentFeedId);
		feedDBHelper.close();

		Bitmap currentImage;
		String titleText, linkText, descriptionText;
		if (currentItem != null) {
			// get screen width (output wallpaper width)
			Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			int screenWidth = size.x;

			String imagePath = imageDirectory + currentItem.getImageName();
			BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
			bitmapOptions.inSampleSize = Helpers.getImageScale(imagePath, screenWidth, 0);
			currentImage = BitmapFactory.decodeFile(imagePath, bitmapOptions);

			titleText = currentItem.title;
			linkText = currentItem.link;
			descriptionText = currentItem.description;
		} else {
			currentImage = null;
			titleText = "";
			linkText = "";
			descriptionText = "";

			Intent downloadRSSIntent = new Intent(this, DownloadRSSService.class);
			downloadRSSIntent.setAction(Constants.ACTION_DOWNLOAD_RSS);
			startService(downloadRSSIntent);

			Intent newIntent = new Intent(this, ScheduleTimerService.class);
			newIntent.setAction(Constants.ACTION_SCHEDULE_ALARMS);
			startService(newIntent);
		}

		recycleCurrentBitmap();
		imageView.setImageBitmap(currentImage);

		((TextView) findViewById(R.id.current_item_description)).setText(Html.fromHtml(descriptionText));

		// set title with link to web page
		SpannableString content = new SpannableString(titleText);
		content.setSpan(new UnderlineSpan(), 0, content.length(), 0);

		final String feedUrl = linkText;
		TextView titleTextView = (TextView) findViewById(R.id.current_item_title);
		titleTextView.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
		titleTextView.setText(content);
		titleTextView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(feedUrl));
				startActivity(intent);

			}
		});
		((TextView) findViewById(R.id.current_item_title)).setText(titleText);
		((TextView) findViewById(R.id.current_feed)).setText(currentFeed.title);

		blockWallpaperButton.setEnabled(true);
		nextWallpaperButton.setEnabled(true);
	}


	public void blockCurrentWallpaper(View view) {
		blockWallpaperButton.setEnabled(false);
		nextWallpaperButton.setEnabled(false);

		logEvent("Disabling current item", "onOptionsItemSelected(MenuItem item)", LogEntry.LogLevel.Trace);

		FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(getApplicationContext());
		feedDBHelper.updateImageEnabled(currentItemId, false);
		feedDBHelper.close();

		sendBroadcast(new Intent(Constants.ACTION_CHANGE_WALLPAPER));

		DownloadImageService.startDownloadImageAction(this);
	}

	public void getNewWallpaper(View view) {
		blockWallpaperButton.setEnabled(false);
		nextWallpaperButton.setEnabled(false);

		sendBroadcast(new Intent(Constants.ACTION_CHANGE_WALLPAPER));
	}

//    private void showToast(String message) {
//        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
//    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_view_items_all:
				startActivity(new Intent(this, FeedItemListView.class));
				return true;
			case R.id.action_settings:
				startActivity(new Intent(this, SettingsActivity.class));
				return true;
			case R.id.action_view_feeds:
				startActivity(new Intent(this, RSSFeedListView.class));
				return true;
			case R.id.action_about:
				startActivity(new Intent(this, About.class));
				return true;
			case R.id.action_view_logs:
				startActivity(new Intent(this, LogViewList.class));
				return true;
//			case R.id.action_restart:
//				sendBroadcast(new Intent(Constants.ACTION_SCHEDULE_ALARMS));
//				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}


	private void logEvent(String message, String function, LogEntry.LogLevel level) {
		((MyApplication) getApplication()).logEvent(message, function, TAG, level);
	}
}
