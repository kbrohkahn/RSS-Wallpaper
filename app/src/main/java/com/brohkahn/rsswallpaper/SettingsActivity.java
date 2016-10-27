package com.brohkahn.rsswallpaper;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
	private static final String TAG = "SettingsActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	protected void onPause() {
		logEvent("Restarting service.", "onPause()", LogEntry.LogLevel.Message);
		Intent serviceIntent = new Intent(this, ChangeWallpaperService.class);
		stopService(serviceIntent);
		startService(serviceIntent);

		DownloadImageService.startDownloadImageAction(this);

		super.onPause();
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
				// For list preferences, look up the correct display value in
				// the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(stringValue);

				// Set the summary to reflect the new value.
				preference.setSummary(
						index >= 0
								? listPreference.getEntries()[index]
								: null);

			}
//            else if (preference instanceof RingtonePreference) {
//                // For ringtone preferences, look up the correct display value
//                // using RingtoneManager.
//                if (TextUtils.isEmpty(stringValue)) {
//                    // Empty values correspond to 'silent' (no ringtone).
//                    preference.setSummary(R.string.pref_ringtone_silent);
//
//                } else {
//                    Ringtone ringtone = RingtoneManager.getRingtone(
//                            preference.getContext(), Uri.parse(stringValue));
//
//                    if (ringtone == null) {
//                        // Clear the summary if there was a lookup error.
//                        preference.setSummary(null);
//                    } else {
//                        // Set the summary to reflect the new ringtone display
//                        // name.
//                        String name = ringtone.getTitle(preference.getContext());
//                        preference.setSummary(name);
//                    }
//                }
//
//            }
			else {
				preference.setSummary(stringValue);
			}
			return true;
		}
	};

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

	private void logEvent(String message, String function, LogEntry.LogLevel level) {
		((MyApplication) getApplication()).logEvent(message, function, TAG, level);
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
			int availableFeedsCount = availableFeeds.size();

			if (availableFeedsCount == 0) {
				availableFeeds.add(Constants.getBuiltInFeed());
			}

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
			SwitchPreference showToastsPreference = (SwitchPreference) findPreference(getResources()
																							  .getString(R.string.key_show_toasts));
			MyApplication.showToasts = showToastsPreference.isChecked();
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
		private boolean initiallyStoreIcons;
		private String imageDirectory;

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
					showDeleteItemsDialog();
					return false;
				}
			});

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
			initiallyStoreIcons = preferences.getBoolean(resources.getString(R.string.key_store_icons), true);
			imageDirectory = preferences.getString(resources.getString(R.string.key_image_directory),
												   getActivity().getFilesDir().getPath() + "/"
			);

		}

		@Override
		public void onStop() {
			SwitchPreference storeIconsPreference = (SwitchPreference) findPreference(getResources()
																							  .getString(R.string.key_store_icons));
			if (storeIconsPreference.isChecked() && !initiallyStoreIcons) {
				DownloadIconService.startDownloadIconAction(getActivity());
			} else if (!storeIconsPreference.isChecked() && initiallyStoreIcons) {
				// delete all icons
				FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(getActivity());
				List<FeedItem> allItems = feedDBHelper.getAllItems();
				feedDBHelper.close();

				for (FeedItem item : allItems) {
					String imagePath = imageDirectory + item.getIconName();
					File iconFile = new File(imagePath);
					if (!iconFile.delete()) {
						((MyApplication) (getActivity().getApplication())).logEvent(
								String.format(Locale.US, "Unable to delete icon %s.", imagePath), "onStop()", TAG, LogEntry.LogLevel.Warning);
					}
				}
			}

			super.onStop();
		}

		public void showDeleteLogsDialog() {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.title_delete_logs)
				   .setMessage(R.string.delete_logs_dialog_message)
				   .setPositiveButton(R.string.delete_dialog_positive, new DialogInterface.OnClickListener() {
					   @Override
					   public void onClick(DialogInterface dialogInterface, int i) {
						   deleteItems();
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

		public void deleteItems() {
			FeedDBHelper feedDbHelper = FeedDBHelper.getHelper(getActivity());
			feedDbHelper.deleteFeedItems();
			feedDbHelper.close();
		}

		public void showDeleteItemsDialog() {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.title_delete_items)
				   .setMessage(R.string.delete_items_dialog_message)
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
}

