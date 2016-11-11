package com.brohkahn.rsswallpaper;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.List;

public class DirectoryArrayAdapter extends ArrayAdapter<String> {
	private Context context;
	private List<String> directories;

	public DirectoryArrayAdapter(Context context, int resource, List<String> directories) {
		super(context, resource, directories);

		this.directories = directories;
		this.context = context;

		updateDirectories();
	}

	@NonNull
	@Override
	public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
		View view = convertView;

		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.directory_list_item, parent);
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

	private void enterDirectory(int position) {
		DirectoryChooser.currentDirectory = directories.get(position);
		DirectoryChooser.currentPath = DirectoryChooser.currentPath + directories.get(position) + "/";
		updateDirectories();
	}

	public void updateDirectories() {
		for (String directory : directories) {
			remove(directory);
		}

		File currentDirectory = new File(DirectoryChooser.currentPath);
		for (File file : currentDirectory.listFiles()) {
			if (file.isDirectory()) {
				add(file.getName());
			}
		}

		notifyDataSetChanged();
	}
}
