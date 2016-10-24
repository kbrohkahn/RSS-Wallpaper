package com.brohkahn.rsswallpaper;

import android.graphics.BitmapFactory;

final class Constants {
	static final String SET_WALLPAPER_ACTION = "set_wallpaper";
	static final String WALLPAPER_UPDATED = "wallpaper_updated";
	static final String ICON_BITMAP_PREFIX = "rss_icon_";

	static int getImageScale(String imagePath, int outputWidth, int outputHeight) {
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

	static Feed getBuiltInFeed() {
		Feed feed = new Feed(0,
							 "http://www.nasa.gov/rss/dyn/lg_image_of_the_day.rss",
							 "NASA Image of the Day",
							 "http://www.nasa.gov",
							 "The latest NASA \"Image of the Day\" image.",
							 false
		);
		feed.entryImageLinkTag = "enclosure";
		feed.entryImageLinkAttribute = "url";

		return feed;
	}

}