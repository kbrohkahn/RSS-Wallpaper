package com.brohkahn.rsswallpaper;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;

import java.io.File;

class Helpers {
	static BooleanMessage canDownload(NetworkInfo activeNetwork, boolean wifiOnly) {
		BooleanMessage response = new BooleanMessage();

		if (activeNetwork == null) {
			response.message = "Not connected to internet, unable to download images.";
			response.booleanValue = false;
		} else {
			boolean wifiConnection = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
			if (wifiOnly && !wifiConnection) {
				response.message = "Not connected to Wifi, unable to download images.";
				response.booleanValue = false;
			} else {
				response.booleanValue = true;
				if (wifiOnly) {
					response.message = "Connected to Wifi, starting download of images.";
				} else {
					response.message = "Connected to internet, starting download of images.";
				}
			}
		}
		return response;
	}

	static int getImageScale(String imagePath, float outputWidth, float outputHeight) {
		BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
		bitmapOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(imagePath, bitmapOptions);

		// Calculate inSampleSize
		int imageHeight = bitmapOptions.outHeight;
		int imageWidth = bitmapOptions.outWidth;
		int inSampleSize = 1;

		while (imageWidth > outputWidth && imageHeight > outputHeight) {
			imageHeight /= 2;
			imageWidth /= 2;
			inSampleSize *= 2;
		}

		return inSampleSize;
	}

//	static String getDefaultFolder(Context context) {
//		return context.getFilesDir().getPath() + "/";
//	}

	static String getStoragePath(Context context, String which) {
		String path = context.getFilesDir().getPath() + "/";

		switch (which) {
			case "LOCAL":
				// already set as default
				break;
			case "INTERNAL":
				File externalStorageDirectory = Environment.getExternalStorageDirectory();
				if (externalStorageDirectory.exists()) {
					String desiredPath = externalStorageDirectory.getPath() + "/rssImages/";

					File rssImageDirectory = new File(desiredPath);
					if (rssImageDirectory.exists() || rssImageDirectory.mkdirs()) {
						path = desiredPath;
					}
				}
				break;
			case "EXTERNAL":
				// not implemented
				break;
		}

		return path;
	}
}
