package com.brohkahn.rsswallpaper;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

class ChangeDirectoryTask extends AsyncTask<Void, Integer, Boolean> {
	private static final String TAG = "ChangeDirectoryTask";

	private Context context;
	private ProgressDialog dialog;

	private String oldPath;
	private String newPath;

	private List<String> itemImageNames;

	ChangeDirectoryTask(Context context, String oldPath, String newPath) {
		this.context = context;
		this.oldPath = oldPath;
		this.newPath = newPath;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();


		FeedDBHelper feedDBHelper = FeedDBHelper.getHelper(context);
		List<FeedItem> allItems = feedDBHelper.getAllItems();
		feedDBHelper.close();

		// move all images
		itemImageNames = new ArrayList<>();
		for (FeedItem item : allItems) {
			File oldImageFile = new File(oldPath + item.getImageName());
			if (oldImageFile.exists()) {
				itemImageNames.add(item.getImageName());
			}
		}

		dialog = new ProgressDialog(context);
		dialog.setProgress(0);
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setMax(itemImageNames.size());
		dialog.setTitle(R.string.change_directory_title);
		dialog.show();
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		if (oldPath == null) {
			logEvent("Original file directory is invalid", LogEntry.LogLevel.Error);
			return false;
		} else if (newPath == null) {
			logEvent("New file directory is invalid", LogEntry.LogLevel.Error);
			return false;
		} else {
			logEvent("Changing directory from " + oldPath + " to " + newPath, LogEntry.LogLevel.Trace);

			boolean allSuccess = true;

			for (int i = 0; i < itemImageNames.size(); i++) {
				File oldImageFile = new File(oldPath, itemImageNames.get(i));
				File newImageFile = new File(newPath, itemImageNames.get(i));
				newImageFile.setWritable(true);

				boolean success = false;

				if (!oldImageFile.exists()) {
					logEvent(oldImageFile.getAbsolutePath() + " does not exist!", LogEntry.LogLevel.Error);
				} else if (newImageFile.exists()) {
					logEvent(newImageFile.getAbsolutePath() + " already exists", LogEntry.LogLevel.Warning);
					if (!oldImageFile.delete()) {
						logEvent("Unable to delete" + oldImageFile.getAbsolutePath(), LogEntry.LogLevel.Warning);
					}
//				} else if (!oldImageFile.canRead()) {
//					logEvent("Can't read from " + oldImageFile.getAbsolutePath(), LogEntry.LogLevel.Warning);
//				} else if (!newImageFile.canWrite()) {
//					logEvent("Can't write to " + newImageFile.getAbsolutePath(), LogEntry.LogLevel.Warning);
//				} else if (!oldImageFile.renameTo(newImageFile)) {
//					logEvent("Error moving file " + oldImageFile.getAbsolutePath() + " to " + newImageFile.getAbsolutePath(),
//							LogEntry.LogLevel.Error);
				} else {
					try {
						FileChannel inChannel = new FileInputStream(oldImageFile).getChannel();
						FileChannel outChannel = new FileOutputStream(newImageFile).getChannel();

						try {
							inChannel.transferTo(0, inChannel.size(), outChannel);

							inChannel.close();
							outChannel.close();
						} catch (IOException e) {
							logEvent(e.getMessage(), LogEntry.LogLevel.Error);
						}
					} catch (IOException e) {
						logEvent(e.getMessage(), LogEntry.LogLevel.Error);
					}
					success = true;
				}

				if (!success) {
					allSuccess = false;
				}

				publishProgress(i);
			}

			return allSuccess;
		}
	}

	@Override
	protected void onProgressUpdate(final Integer... values) {
		dialog.setProgress(values[0]);
	}

	@Override
	protected void onPostExecute(Boolean success) {
		dialog.dismiss();

		if (!success) {
			logEvent("Failed to copy all files to new folder", LogEntry.LogLevel.Error);
		} else {
			logEvent("Successfully moved all files", LogEntry.LogLevel.Message);
		}

		super.onPostExecute(success);
	}

	private void logEvent(String message, LogEntry.LogLevel level) {
		LogDBHelper helper = LogDBHelper.getHelper(context);
		helper.saveLogEntry(message, null, TAG, "doInBackground", level);
		helper.close();
	}
}
