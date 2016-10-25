package com.brohkahn.rsswallpaper;

final class Constants {
	static final String SET_WALLPAPER_ACTION = "set_wallpaper";
	static final String WALLPAPER_UPDATED = "wallpaper_updated";
	static final String ICON_BITMAP_PREFIX = "icon_";

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
