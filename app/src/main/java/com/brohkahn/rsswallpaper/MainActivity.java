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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
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

public class MainActivity extends AppCompatActivity {
	private static final String TAG = "MainActivity";

//    private final int REQUEST_PERMISSIONS = 0;

	private int currentItemId;

	private ImageView imageView;

	//	private Button nextWallpaperButton;
	//	private Button blockWallpaperButton;
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
//		nextWallpaperButton = ((Button) findViewById(R.id.next_wallpaper_button));
//		blockWallpaperButton = ((Button) findViewById(R.id.block_wallpaper_button));

//        int internetPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
//        int wallpaperPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.SET_WALLPAPER);
//        if (internetPermission != PackageManager.PERMISSION_GRANTED || wallpaperPermission != PackageManager.PERMISSION_GRANTED) {
//            logEvent("Missing permissions.", "onCreate(Bundle savedInstanceState)", LogEntry.LogLevel.Message);
//
//            showPermissionDialog();
//        } else {
//            if (!ChangeWallpaperService.isRunning) {
//                logEvent("Service not running, restarting.", "onCreate(Bundle savedInstanceState)", LogEntry.LogLevel.Message);
//
//                restartService();
//            }
//
//            updateCurrentItem();
//        }

		if (!ChangeWallpaperService.isRunning) {
			logEvent("Service not running, restarting.", "onCreate(Bundle savedInstanceState)", LogEntry.LogLevel.Message);

			restartService();
		}

		updateCurrentItem();

		IntentFilter mStatusIntentFilter = new IntentFilter(Constants.WALLPAPER_UPDATED);
		LocalBroadcastManager.getInstance(this)
							 .registerReceiver(wallpaperUpdated, mStatusIntentFilter);
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
		currentItemId = settings.getInt(resources.getString(R.string.key_current_item), 0);
		String imageDirectory = settings.getString(resources.getString(R.string.key_image_directory), getFilesDir()
				.getPath() + "/");
		int currentFeedId = Integer.parseInt(settings.getString(resources.getString(R.string.key_current_feed), "0"));

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
		feedTextView.setTextColor(resources.getColor(R.color.colorAccent));
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


		if (currentFeed == null) {
			currentFeed = Constants.getBuiltInFeed();
		}

		((TextView) findViewById(R.id.current_feed)).setText(currentFeed.title);

		fab.setEnabled(true);
//		blockWallpaperButton.setEnabled(true);
//		nextWallpaperButton.setEnabled(true);
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
		logEvent("Restarting service.", "restartService()", LogEntry.LogLevel.Message);
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
		// Handle item selection
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

//	public void getNewWallpaper(View view) {
//		getNewWallpaper();
//	}

	public void blockCurrentWallpaper() {
		logEvent("Disabling current item", "onOptionsItemSelected(MenuItem item)", LogEntry.LogLevel.Trace);

		FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(getApplicationContext());
		feedDBHelper.updateImageEnabled(currentItemId, false);
		feedDBHelper.close();

		getNewWallpaper();

		DownloadImageService.startDownloadImageAction(this);
	}

	private void getNewWallpaper() {
//		blockWallpaperButton.setEnabled(false);
//		nextWallpaperButton.setEnabled(false);
		fab.setEnabled(false);

		logEvent("Sending set wallpaper broadcast", "onOptionsItemSelected(MenuItem item)", LogEntry.LogLevel.Trace);

		new Handler().post(new Runnable() {
			@Override
			public void run() {
				Intent intent = new Intent(Constants.SET_WALLPAPER_ACTION);
				LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
			}
		});
	}

//    private void showToast(String message) {
//        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
//    }

	private void logEvent(String message, String function, LogEntry.LogLevel level) {
		((MyApplication) getApplication()).logEvent(message, function, TAG, level);
	}
}
