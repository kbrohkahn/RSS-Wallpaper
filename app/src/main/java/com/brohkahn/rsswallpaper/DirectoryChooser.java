package com.brohkahn.rsswallpaper;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class DirectoryChooser {
	private static final String TAG = "DirectoryChooser";

	private List<String> currentPath;

	private Context context;
	private DirectoryArrayAdapter directoryArrayAdapter;

	private TextView currentDirectoryTextView;

	DirectoryChooser(Context context) {
		currentPath = new ArrayList<>();

		this.context = context;
	}

	void showDialog() {
		// start intent to choose directory
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		LayoutInflater inflater = ((Activity) context).getLayoutInflater();

		builder.setView(inflater.inflate(R.layout.directory_dialog, null))
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						saveCurrentDirectory();

						dialogInterface.dismiss();
					}
				})
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						dialogInterface.dismiss();
					}
				});
		AlertDialog dialog = builder.create();
		dialog.show();


		currentDirectoryTextView = (TextView) dialog.findViewById(R.id.current_directory);

		directoryArrayAdapter = new DirectoryArrayAdapter(context, R.layout.directory_list_item, getDirectoryList());

		ListView directoryListView = (ListView) dialog.findViewById(R.id.directory_list_view);
		if (directoryListView != null) {
			directoryListView.setAdapter(directoryArrayAdapter);
		}

		ImageButton backButton = (ImageButton) dialog.findViewById(R.id.exit_directory_button);
		if (backButton != null) {
			backButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					exitDirectoryButtonClick();
				}
			});
		}

		ImageButton createDirectoryButton = (ImageButton) dialog.findViewById(R.id.create_directory_button);
		if (createDirectoryButton != null) {
			createDirectoryButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					createDirectoryButtonClick();
				}
			});
		}
	}

	private String getCurrentFullPath() {
		return "/" + TextUtils.join("/", currentPath);
	}

	private void saveCurrentDirectory() {
		if (directoryArrayAdapter != null) {
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context)
					.edit();
			editor.putString(context.getResources().getString(R.string.key_image_directory), getCurrentFullPath());
			editor.apply();
		}
	}

	private void createDirectoryButtonClick() {
		if (directoryArrayAdapter != null) {
			directoryArrayAdapter.updateDirectoriesList();
		}
	}

	private void exitDirectoryButtonClick() {
		if (currentPath.size() > 0) {
			currentPath.remove(currentPath.size() - 1);

			if (directoryArrayAdapter != null) {
				directoryArrayAdapter.updateDirectoriesList();
			}
		}
	}

	private List<String> getDirectoryList() {
		String directoryName;
		if (currentPath.size() == 0) {
			directoryName = "Root directory";
		} else {
			directoryName = currentPath.get(currentPath.size() - 1);
		}
		currentDirectoryTextView.setText(directoryName);

		String message;
		File currentDirectory = new File(getCurrentFullPath());
		if (!currentDirectory.exists()) {
			message = "Error: path " + currentPath + " does not exist";
			logEvent(message, "getDirectoryList", LogEntry.LogLevel.Error);
			showToast(message);
			return new ArrayList<>();
		} else if (currentDirectory.listFiles() == null) {
			message = "Warning: path " + currentPath + " contains no files";
			logEvent(message, "getDirectoryList", LogEntry.LogLevel.Warning);
			showToast(message);
			return new ArrayList<>();
		} else {
			List<String> directories = new ArrayList<>();
			for (File file : currentDirectory.listFiles()) {
				if (file.isDirectory()) {
					directories.add(file.getName() + "/");
				}
			}
			return directories;
		}
	}

	private void showToast(String message) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

	private void logEvent(String message, String function, LogEntry.LogLevel level) {
		LogDBHelper helper = LogDBHelper.getHelper(context);
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
					enterDirectory(position);
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

		private void enterDirectory(int position) {
			currentPath.add(directories.get(position));
			updateDirectoriesList();
		}


		void updateDirectoriesList() {
			this.directories = getDirectoryList();
			notifyDataSetChanged();
		}
	}
}
