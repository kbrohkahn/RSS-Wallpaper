package com.brohkahn.rsswallpaper;

import java.util.Date;

class FeedItem {
	public int id;
	int feedId;
	String title;
	String link;
	String description;
	Date creationDate;

	String imageLink;
	boolean downloaded;
	boolean enabled;

	FeedItem(int id,
			 String title,
			 String link,
			 String description,
			 Date creationDate) {
		this.id = id;
		this.title = title;
		this.link = link;
		this.description = description;
		this.creationDate = creationDate;
	}

	FeedItem(int id, String title, String imageLink, boolean enabled) {
		this.id = id;
		this.title = title;
		this.imageLink = imageLink;
		this.enabled = enabled;
	}

	String getImageName() {
		if (imageLink != null) {
			return String.valueOf(id) + imageLink.substring(imageLink.lastIndexOf('.'));
		} else {
			return "";
		}
	}

	String getIconName() {
		if (imageLink != null) {
			return "icon_" + String.valueOf(id) + imageLink.substring(imageLink.lastIndexOf('.'));
		} else {
			return "";
		}
	}
}
