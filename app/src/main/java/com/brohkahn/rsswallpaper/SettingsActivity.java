package com.brohkahn.rsswallpaper;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.widget.Toast;

import com.brohkahn.loggerlibrary.LogDBHelper;

import java.util.List;

public class SettingsActivity extends AppCompatPreferenceActivity {
//	private static final String TAG = "SettingsActivity";

	private static final int REQUEST_PERMISSIONS_CODE = 1;
//	private static final int CHOOSE_DIRECTORY_CODE = 2;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onIsMultiPane() {
		return (getResources().getConfiguration().screenLayout
				& Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.pref_headers, target);
	}


	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 *
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	private static void bindPreferenceSummaryToValue(Preference preference) {
		// Set the listener to watch for value changes.
		preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's
		// current value.
		sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference
				.getContext())
				.getString(preference
						.getKey(), "")
		);
	}

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();

			if (preference instanceof ListPreference) {
				setListPreferenceSummary(preference, stringValue);
			} else {
				preference.setSummary(stringValue);
			}
			return true;
		}
	};

	public static void setListPreferenceSummary(Preference preference, String newValue) {
		// For list preferences, look up the correct display value in
		// the preference's 'entries' list.
		ListPreference listPreference = (ListPreference) preference;
		int index = listPreference.findIndexOfValue(newValue);

		// Set the summary to reflect the new value.
		preference.setSummary(
				index >= 0
						? listPreference.getEntries()[index]
						: null);
	}

	/**
	 * This method stops fragment injection in malicious applications.
	 * Make sure to deny any unknown fragments here.
	 */
	protected boolean isValidFragment(String fragmentName) {
		return PreferenceFragment.class.getName().equals(fragmentName)
				|| RSSFeedPreferenceFragment.class.getName().equals(fragmentName)
				|| WallpaperPreferenceFragment.class.getName().equals(fragmentName)
				|| NotificationPreferenceFragment.class.getName().equals(fragmentName)
				|| StoragePreferenceFragment.class.getName().equals(fragmentName);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public static class RSSFeedPreferenceFragment extends PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_rss_feed);
			setHasOptionsMenu(true);

			FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(getActivity());
			List<RSSFeed> availableFeeds = feedDBHelper.getAvailableFeeds();
			feedDBHelper.close();

			int availableFeedsCount = availableFeeds.size();
			String[] currentFeedValues = new String[availableFeedsCount];
			String[] currentFeedTitles = new String[availableFeedsCount];
			for (int i = 0; i < availableFeedsCount; i++) {
				RSSFeed feed = availableFeeds.get(i);
				currentFeedTitles[i] = feed.title;
				currentFeedValues[i] = String.valueOf(feed.id);
			}

			Resources resources = getResources();

			ListPreference feedListPreference = (ListPreference) findPreference(resources.getString(R.string.key_current_feed));
			feedListPreference.setEntries(currentFeedTitles);
			feedListPreference.setEntryValues(currentFeedValues);

			bindPreferenceSummaryToValue(feedListPreference);
			bindPreferenceSummaryToValue(findPreference(resources.getString(R.string.key_update_interval)));
			bindPreferenceSummaryToValue(findPreference(resources.getString(R.string.key_update_time)));
		}

		@Override
		public void onStop() {
			Intent newIntent = new Intent(getActivity(), ScheduleTimerService.class);
			newIntent.setAction(Constants.ACTION_SCHEDULE_ALARMS);
			getActivity().startService(newIntent);

			super.onStop();
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			int id = item.getItemId();
			if (id == android.R.id.home) {
				startActivity(new Intent(getActivity(), SettingsActivity.class));
				return true;
			}
			return super.onOptionsItemSelected(item);
		}
	}

	public static class NotificationPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_notification);
			setHasOptionsMenu(true);
		}

		@Override
		public void onStop() {
			SwitchPreference showMessageToastsPreference = (SwitchPreference) findPreference(getResources()
					.getString(R.string.key_show_message_toasts));
			MyApplication.showMessageToasts = showMessageToastsPreference.isChecked();

			SwitchPreference showToastsPreference = (SwitchPreference) findPreference(getResources()
					.getString(R.string.key_show_error_toasts));
			MyApplication.showErrorToasts = showToastsPreference.isChecked();
			super.onStop();
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			int id = item.getItemId();
			if (id == android.R.id.home) {
				startActivity(new Intent(getActivity(), SettingsActivity.class));
				return true;
			}
			return super.onOptionsItemSelected(item);
		}
	}

	public static class WallpaperPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_wallpaper);
			setHasOptionsMenu(true);

			Resources resources = getResources();
			final String broadcastTitle = resources.getString(R.string.title_receive_broadcast);

			Preference preference = findPreference(resources.getString(R.string.key_receive_broadcast));
			preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText(broadcastTitle, Constants.ACTION_CHANGE_WALLPAPER);
					clipboard.setPrimaryClip(clip);

					Toast.makeText(getActivity(), "Text copied", Toast.LENGTH_SHORT).show();
					return true;
				}
			});


			bindPreferenceSummaryToValue(findPreference(resources.getString(R.string.key_number_to_rotate)));
			bindPreferenceSummaryToValue(findPreference(resources.getString(R.string.key_change_interval)));
			bindPreferenceSummaryToValue(findPreference(resources.getString(R.string.key_crop_and_scale_type)));

			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
				SwitchPreference lockScreenPreference = (SwitchPreference) findPreference(resources.getString(R.string.key_set_lock_screen));
				lockScreenPreference.setEnabled(false);
				lockScreenPreference.setChecked(false);

			}
		}

		@Override
		public void onStop() {
			Intent newIntent = new Intent(getActivity(), ScheduleTimerService.class);
			newIntent.setAction(Constants.ACTION_SCHEDULE_ALARMS);
			getActivity().startService(newIntent);

			super.onStop();
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			int id = item.getItemId();
			if (id == android.R.id.home) {
				startActivity(new Intent(getActivity(), SettingsActivity.class));
				return true;
			}
			return super.onOptionsItemSelected(item);
		}
	}


	public static class StoragePreferenceFragment extends PreferenceFragment {
		private String desiredStorageLocation;
		private String originalImageLocation;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_storage);
			setHasOptionsMenu(true);

			Resources resources = getResources();
			Preference deleteLogsPreference = findPreference(resources.getString(R.string.key_delete_logs));
			deleteLogsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					showDeleteLogsDialog();
					return false;
				}
			});

			Preference deleteItemsPreference = findPreference(resources.getString(R.string.key_delete_items));
			deleteItemsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					showDeleteItemsDialog(false, true);
					return false;
				}
			});

			Preference purgeImagesPreference = findPreference(resources.getString(R.string.key_purge_unused_images));
			purgeImagesPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					purgeImagesPreferenceChanged((boolean) newValue);
					return true;
				}
			});

			Preference directoryPreference = findPreference(resources.getString(R.string.key_image_storage));
			directoryPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					setListPreferenceSummary(preference, desiredStorageLocation);

					desiredStorageLocation = newValue.toString();
					changeDirectory();
					return true;
				}
			});


			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
			originalImageLocation = preferences.getString(getResources().getString(R.string.key_image_storage), "LOCAL");
			setListPreferenceSummary(directoryPreference, originalImageLocation);
			bindPreferenceSummaryToValue(findPreference(resources.getString(R.string.key_compression_type)));
		}

		public void purgeImagesPreferenceChanged(boolean newValue) {
			if (newValue) {
				showDeleteItemsDialog(true, false);
			} else {
				DownloadImageService.startDownloadImageAction(getActivity());
			}
		}

		public void showDeleteLogsDialog() {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.title_delete_logs)
					.setMessage(R.string.delete_logs_dialog_message)
					.setPositiveButton(R.string.delete_dialog_positive, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							deleteLogs();
							dialogInterface.dismiss();
						}
					})
					.setNegativeButton(R.string.delete_dialog_negative, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							dialogInterface.dismiss();
						}
					});
			builder.create().show();
		}

		public void deleteLogs() {
			LogDBHelper logDbHelper = LogDBHelper.getHelper(getActivity());
			logDbHelper.deleteLogs();
			logDbHelper.close();
		}

		public void showDeleteItemsDialog(final boolean purgeOld, final boolean deleteAll) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.title_delete_items)
					.setMessage(R.string.delete_items_dialog_message)
					.setPositiveButton(R.string.delete_dialog_positive, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							deleteItems(purgeOld, deleteAll);
							dialogInterface.dismiss();
						}
					})
					.setNegativeButton(R.string.delete_dialog_negative, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							dialogInterface.dismiss();
						}
					});
			builder.create().show();
		}

		public void deleteItems(boolean purgeOld, boolean deleteAll) {
			DeleteFileTask deleteFileTask = new DeleteFileTask(getActivity());
			deleteFileTask.purgeOldImages = purgeOld;
			deleteFileTask.deleteAllItems = deleteAll;
			deleteFileTask.execute();
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			int id = item.getItemId();
			if (id == android.R.id.home) {
				startActivity(new Intent(getActivity(), SettingsActivity.class));
				return true;
			}
			return super.onOptionsItemSelected(item);
		}

		public void changeDirectory() {
			if (!hasStoragePermission()) {
				showPermissionDialog();
			} else {
				startChangeDirectoryTask();
			}
		}


		public boolean hasStoragePermission() {
			return ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager
					.PERMISSION_GRANTED;
		}

		public void showPermissionDialog() {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
					.setTitle(R.string.permission_request_title)
					.setMessage(R.string.permission_request_message)
					.setPositiveButton(R.string.permission_request_positive_button, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							requestStoragePermission();
							dialogInterface.dismiss();
						}
					})
					.setNegativeButton(R.string.permission_request_negative_button, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							resetStorageLocation();
							dialogInterface.dismiss();
						}
					});
			builder.create().show();
		}

		private void requestStoragePermission() {
			ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS_CODE);
		}

		@Override
		public void onRequestPermissionsResult(int requestCode,
											   @NonNull String permissions[],
											   @NonNull int[] grantResults) {
			switch (requestCode) {
				case REQUEST_PERMISSIONS_CODE: {
					// If request is cancelled, the result arrays are empty.
					if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
						resetStorageLocation();
					} else {
						startChangeDirectoryTask();
					}
				}
			}
		}

		public void resetStorageLocation() {
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
			editor.putString(getResources().getString(R.string.key_image_storage), "LOCAL");
			editor.apply();
		}

		public void startChangeDirectoryTask() {
			if (!originalImageLocation.equals(desiredStorageLocation)) {
				String originalPath = Helpers.getStoragePath(getActivity(), originalImageLocation);
				String newPath = Helpers.getStoragePath(getActivity(), desiredStorageLocation);

				new ChangeDirectoryTask(getActivity(), originalPath, newPath).execute();

				originalImageLocation = desiredStorageLocation;

			}
		}
	}

	//	public void startDirectoryChooser() {
//		startActivityForResult(new Intent(this, DirectoryChooserActivity.class), CHOOSE_DIRECTORY_CODE);
//	}

//	@Override
//	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		if (resultCode == RESULT_OK) {
//			if (requestCode == CHOOSE_DIRECTORY_CODE) {
//				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
//				String oldPath = preferences.getString(getResources().getString(R.string.key_image_location),
//						Helpers.getDefaultFolder(this));
//				String newPath = data.getStringExtra(DirectoryChooserActivity.RESULT_KEY);
//
//				new ChangeDirectoryTask(this).execute(oldPath, newPath);
//
////				SharedPreferences.Editor editor = preferences.edit();
////				editor.putString(getResources().getString(R.string.key_image_location), newPath);
////				editor.apply();
//
//
//			}
//		} else {
//			super.onActivityResult(requestCode, resultCode, data);
//		}
//	}
}

