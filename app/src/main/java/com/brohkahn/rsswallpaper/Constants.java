package com.brohkahn.rsswallpaper;

final class Constants {
	static final String SET_WALLPAPER_ACTION = "set_wallpaper";
	static final String WALLPAPER_UPDATED = "wallpaper_updated";
//	static final String ICON_BITMAP_PREFIX = "icon_";

	static final int MAX_URL_CHARS = 2083;
	static final int APPROXIMATE_FEED_ITEM_COUNT = 100;
	static final int SUPPORTED_FEED_COUNT = 2;

	static RSSFeed getBuiltInFeed() {
		return new RSSFeed(1,
						   "http://www.nasa.gov/rss/dyn/lg_image_of_the_day.rss",
						   "NASA Image of the Day",
						   "enclosure",
						   "url",
						   false,
						   true
		);
	}


}
