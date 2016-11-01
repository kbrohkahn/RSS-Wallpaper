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
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.brohkahn.loggerlibrary.LogEntry;
import com.brohkahn.loggerlibrary.LogViewList;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
	private static final String TAG = "MainActivity";

	private int currentItemId;

	private ImageView imageView;

	private FloatingActionButton fab;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//        Thread.setDefaultUncaughtExceptionHandler(new ErrorHandler(this, true));

		setContentView(R.layout.activity_main);

		Toolbar toolbar = (Toolbar) findViewById(R.id.activity_main_toolbar);
		setSupportActionBar(toolbar);

		fab = (FloatingActionButton) findViewById(R.id.activity_main_fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				blockCurrentWallpaper();
			}
		});

		imageView = ((ImageView) findViewById(R.id.current_item_image));

		// check if any feeds exist, if not save defaults
		FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(this);
		if (feedDBHelper.getAllFeeds().size() == 0) {

			// get and save default feed
			List<RSSFeed> defaultFeedList = new ArrayList<>();
			defaultFeedList.add(new RSSFeed(0,
					"http://www.nasa.gov/rss/dyn/lg_image_of_the_day.rss",
					"NASA Image of the Day",
					"enclosure",
					"url",
					false,
					true
			));
			feedDBHelper.saveNewFeeds(defaultFeedList);

			// get id of feed we just saved, put it in preferences
			int currentFeedId = feedDBHelper.getAllFeeds().get(0).id;
			Resources resources = getResources();
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
			editor.putString(resources.getString(R.string.key_current_feed), String.valueOf(currentFeedId));
			editor.apply();
		}

		// check if we have no initial items before closing helper
		boolean noInitialItems = feedDBHelper.getAllItems().size() == 0;

		feedDBHelper.close();

		// if no initial items, we need to download and restart timers
		if (noInitialItems) {
			Intent alarmIntent = new Intent(Constants.BROADCAST_SCHEDULE_ALARMS);
			LocalBroadcastManager.getInstance(this).sendBroadcast(alarmIntent);

			Intent rssIntent = new Intent(Constants.BROADCAST_DOWNLOAD_RSS);
			LocalBroadcastManager.getInstance(this).sendBroadcast(rssIntent);
		} else {
			updateCurrentItem();
		}

		// listen for wallpaper updates while active
		IntentFilter mStatusIntentFilter = new IntentFilter(Constants.BROADCAST_WALLPAPER_UPDATED);
		LocalBroadcastManager.getInstance(this).registerReceiver(wallpaperUpdated, mStatusIntentFilter);
	}

	@Override
	protected void onDestroy() {
		recycleCurrentBitmap();

		LocalBroadcastManager.getInstance(this).unregisterReceiver(wallpaperUpdated);
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
		String imageDirectory = settings.getString(resources.getString(R.string.key_image_directory), getFilesDir()
				.getPath() + "/");
		int currentFeedId = Integer.parseInt(settings.getString(resources.getString(R.string.key_current_feed), "1"));

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
			bitmapOptions.inSampleSize = MyApplication.getImageScale(imagePath, screenWidth, 0);
			currentImage = BitmapFactory.decodeFile(imagePath, bitmapOptions);

			titleText = currentItem.title;
			linkText = currentItem.link;
			descriptionText = currentItem.description;
		} else {
			currentImage = null;
			titleText = "";
			linkText = "";
			descriptionText = "";
		}

		recycleCurrentBitmap();
		imageView.setImageBitmap(currentImage);

		((TextView) findViewById(R.id.current_item_description)).setText(Html.fromHtml(descriptionText));

		// set title with link to web page
		SpannableString content = new SpannableString(titleText);
		content.setSpan(new UnderlineSpan(), 0, content.length(), 0);

		final String feedUrl = linkText;
		TextView feedTextView = (TextView) findViewById(R.id.current_item_title);
		feedTextView.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
		feedTextView.setText(content);
		feedTextView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(feedUrl));
				startActivity(intent);

			}
		});
		((TextView) findViewById(R.id.current_item_title)).setText(titleText);
		((TextView) findViewById(R.id.current_feed)).setText(currentFeed.title);

		fab.setEnabled(true);
	}

//    public void showPermissionDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this)
//                .setTitle(R.string.permission_request_title)
//                .setMessage(R.string.permission_request_message)
//                .setPositiveButton(R.string.permission_request_positive_button, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        requestInternetPermission();
//                    }
//                })
//                .setNegativeButton(R.string.permission_request_negative_button, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        Uri packageUri = Uri.parse("package:" + getApplicationContext().getPackageName());
//                        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
//                        startActivity(uninstallIntent);
//                    }
//                });
//        builder.create().show();
//    }
//
//    private void requestInternetPermission() {
//        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET, Manifest.permission.SET_WALLPAPER}, REQUEST_PERMISSIONS);
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode,
//                                           @NonNull String permissions[],
//                                           @NonNull int[] grantResults) {
//        switch (requestCode) {
//            case REQUEST_PERMISSIONS: {
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    logEvent("All permissions received :)",
//                            "onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)",
//                            LogEntry.LogLevel.Message);
//
//                    restartService();
//                } else {
//                    showPermissionDialog();
//                }
//            }
//        }
//    }

	public void restartService() {
		logEvent("Restarting service.", "restartService()", LogEntry.LogLevel.Trace);
		Intent serviceIntent = new Intent(this, ChangeWallpaperService.class);
		stopService(serviceIntent);
		startService(serviceIntent);
	}

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
			case R.id.action_restart:
				restartService();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void blockCurrentWallpaper() {
		logEvent("Disabling current item", "onOptionsItemSelected(MenuItem item)", LogEntry.LogLevel.Trace);

		fab.setEnabled(false);


		FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(getApplicationContext());
		feedDBHelper.updateImageEnabled(currentItemId, false);
		feedDBHelper.close();

		Intent intent = new Intent(Constants.ACTION_CHANGE_WALLPAPER);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

		DownloadImageService.startDownloadImageAction(this, false);
	}

//	private void getNewWallpaper() {
//		blockWallpaperButton.setEnabled(false);
//		nextWallpaperButton.setEnabled(false);
//		fab.setEnabled(false);
//
//		logEvent("Sending set wallpaper broadcast", "onOptionsItemSelected(MenuItem item)", LogEntry.LogLevel.Trace);
//
//		new Handler().post(new Runnable() {
//			@Override
//			public void run() {
//				Intent intent = new Intent(Constants.ACTION_CHANGE_WALLPAPER);
//				LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
//			}
//		});
//	}

//    private void showToast(String message) {
//        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
//    }

	private void logEvent(String message, String function, LogEntry.LogLevel level) {
		((MyApplication) getApplication()).logEvent(message, function, TAG, level);
	}
}
