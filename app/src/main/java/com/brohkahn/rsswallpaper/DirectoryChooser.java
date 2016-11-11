package com.brohkahn.rsswallpaper;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

class DirectoryChooser {
	static String currentPath = "/";
	static String currentDirectory = "";

	private Context context;
	private DirectoryArrayAdapter directoryArrayAdapter;

	public DirectoryChooser(Context context) {
		this.context = context;
	}

	public void showDialog() {
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

		directoryArrayAdapter = new DirectoryArrayAdapter(context, R.layout.directory_list_item, new ArrayList<String>());

		ListView directoryListView = (ListView) dialog.findViewById(R.id.directory_list_view);
		if (directoryListView != null) {
			directoryListView.setAdapter(directoryArrayAdapter);
		}

		TextView currentDirectoryTextView = (TextView) dialog.findViewById(R.id.directory_name);
		if (currentDirectoryTextView != null) {
			currentDirectoryTextView.setText(currentDirectory.equals("") ? "Root" : currentDirectory);
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

		dialog.show();
	}

	private void saveCurrentDirectory() {
		if (directoryArrayAdapter != null) {
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context)
					.edit();
			editor.putString(context.getResources().getString(R.string.key_image_directory), currentPath);
			editor.apply();
		}
	}

	private void createDirectoryButtonClick() {
		if (directoryArrayAdapter != null) {
			directoryArrayAdapter.updateDirectories();
		}
	}

	private void exitDirectoryButtonClick() {
		int lastIndex = currentPath.lastIndexOf('/', -1);
		if (lastIndex > 0) {
			currentPath = currentPath.substring(0, lastIndex);
		}

//		boolean atRoot = currentPath.lastIndexOf('/') == 0;

		if (directoryArrayAdapter != null) {
			directoryArrayAdapter.updateDirectories();
		}
	}

}
