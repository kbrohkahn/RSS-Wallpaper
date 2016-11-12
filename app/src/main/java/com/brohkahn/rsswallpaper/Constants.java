package com.brohkahn.rsswallpaper;

final class Constants {
	private static final String PACKAGE_ACTION = "com.brohkahn.rsswallpaper.action.";
	static final String ACTION_CHANGE_WALLPAPER = PACKAGE_ACTION + "change_wallpaper";
	static final String ACTION_DOWNLOAD_RSS = PACKAGE_ACTION + "download_rss";
	static final String ACTION_DOWNLOAD_ICONS = PACKAGE_ACTION + "download_icons";
	static final String ACTION_DOWNLOAD_IMAGES = PACKAGE_ACTION + "download_images";
	static final String ACTION_SCHEDULE_ALARMS = PACKAGE_ACTION + "schedule_alarms";
	static final String ACTION_WALLPAPER_UPDATED = PACKAGE_ACTION + "wallpaper_updated";

	static final String KEY_INTENT_SOURCE = "intentSource";

	static final String ICONS_FOLDER = "icons/";

	static final int CHANGE_WALLPAPER_RECEIVER_CODE = 89232565;
	static final int DOWNLOAD_RSS_SERVICE_CODE = 32477277;

//	static final String ICON_BITMAP_PREFIX = "icon_";

	//	static final int MAX_URL_CHARS = 2083;
	static final int APPROXIMATE_FEED_ITEM_COUNT = 50;
	static final int SUPPORTED_FEED_COUNT = 2;


}
