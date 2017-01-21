package com.brohkahn.rsswallpaper;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DirectoryChooserActivity extends AppCompatActivity {
	private static final String TAG = "DirectoryChooser";

	public static final String RESULT_KEY = "directory";

	private List<String> currentPath;

	private DirectoryArrayAdapter directoryArrayAdapter;

	private TextView currentDirectoryTextView;

	private RelativeLayout newDirectoryLayout;
	private EditText newDirectoryEditText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		String originalPath = Helpers.getStoragePath(this,
				preferences.getString(getResources().getString(R.string.key_image_storage), "LOCAL"));

		currentPath = new ArrayList<>(Arrays.asList(originalPath.substring(1).split("/")));

		// start intent to choose directory
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setView(getLayoutInflater().inflate(R.layout.directory_dialog, null))
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {

						dialogInterface.dismiss();
						saveCurrentDirectory();
						finish();
					}
				})
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						dialogInterface.dismiss();
						finish();
					}
				});
		AlertDialog dialog = builder.create();
		dialog.show();


		currentDirectoryTextView = (TextView) dialog.findViewById(R.id.current_directory);

		directoryArrayAdapter = new DirectoryArrayAdapter(this, R.layout.directory_list_item, getDirectoryList());

		ListView directoryListView = (ListView) dialog.findViewById(R.id.directory_list_view);
		if (directoryListView != null) {
			directoryListView.setAdapter(directoryArrayAdapter);
		}

		newDirectoryLayout = (RelativeLayout) dialog.findViewById(R.id.create_directory_layout);
		newDirectoryEditText = (EditText) dialog.findViewById(R.id.create_directory_edit_text);
	}

	private String getCurrentFullPath() {
		return currentPath.size() == 0 ? "/" : "/" + TextUtils.join("/", currentPath) + "/";
	}

	private void saveCurrentDirectory() {
		Intent returnIntent = getIntent();
		returnIntent.putExtra(RESULT_KEY, getCurrentFullPath());
		setResult(RESULT_OK, returnIntent);
	}

	public void newDirectoryButtonClick(View view) {
		newDirectoryLayout.setVisibility(View.VISIBLE);
	}

	public void createDirectoryButtonClick(View view) {
		String newDirectoryName = newDirectoryEditText.getText().toString();

		currentPath.add(newDirectoryName);

		File newDirectory = new File(getCurrentFullPath());
		if (!newDirectory.mkdir()) {
			logEvent("Unable to create directory " + getCurrentFullPath(),
					"createDirectoryButtonClick",
					LogEntry.LogLevel.Error);
			currentPath.remove(currentPath.size() - 1);
		}

		hideNewDirectoryLayout();

		directoryArrayAdapter.updateDirectoriesList();
	}

	public void hideNewDirectoryLayout() {
		newDirectoryLayout.setVisibility(View.GONE);
	}

	public void exitDirectoryButtonClick(View view) {
		if (currentPath.size() > 0) {
			currentPath.remove(currentPath.size() - 1);

			if (directoryArrayAdapter != null) {
				directoryArrayAdapter.updateDirectoriesList();
			}
		}
	}

	private List<String> getDirectoryList() {
		currentDirectoryTextView.setText(getCurrentFullPath());

		String message;
		File currentDirectory = new File(getCurrentFullPath());
		if (!currentDirectory.exists()) {
			message = "Error: path " + currentDirectory + " does not exist";
			logEvent(message, "getDirectoryList", LogEntry.LogLevel.Error);
			showToast(message);
			return new ArrayList<>();
		} else if (currentDirectory.listFiles() == null) {
			message = "Warning: path " + currentDirectory + " contains no files";
			logEvent(message, "getDirectoryList", LogEntry.LogLevel.Warning);
			showToast(message);
			return new ArrayList<>();
		} else {
			List<String> directories = new ArrayList<>();
			for (File file : currentDirectory.listFiles()) {
				if (file.isDirectory()) {
					// sort by name
					String fileName = file.getName();
					int index;
					for (index = 0; index < directories.size(); index++) {
						String tempFileName = directories.get(index);
						if (tempFileName.compareToIgnoreCase(fileName) > 1) {
							break;
						}
					}

					directories.add(index, file.getName());
				}
			}
			return directories;
		}
	}

	private void showToast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	private void logEvent(String message, String function, LogEntry.LogLevel level) {
		LogDBHelper helper = LogDBHelper.getHelper(this);
		helper.saveLogEntry(message, null, TAG, function, level);
		helper.close();
	}

	private class DirectoryArrayAdapter extends ArrayAdapter<String> {
		private Context context;
		private List<String> directories;

		DirectoryArrayAdapter(Context context, int resource, List<String> directories) {
			super(context, resource, directories);

			this.directories = directories;
			this.context = context;
		}

		@NonNull
		@Override
		public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
			View view = convertView;

			if (view == null) {
				LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(R.layout.directory_list_item, null);
			}

			String directory = directories.get(position);
			((TextView) view.findViewById(R.id.directory_name)).setText(directory);

			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					enterDirectoryAtPosition(position);
				}
			});


			return view;
		}

		@Override
		public int getCount() {
			return directories.size();
		}

		@Nullable
		@Override
		public String getItem(int position) {
			return directories.get(position);
		}

		private void enterDirectoryAtPosition(int position) {
			currentPath.add(directories.get(position));
			updateDirectoriesList();
		}


		void updateDirectoriesList() {
			this.directories = getDirectoryList();
			notifyDataSetChanged();
		}
	}
}
