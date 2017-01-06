package com.brohkahn.rsswallpaper;

import java.io.File;
import java.util.Date;

class FeedItem {
	public int id;
	int feedId;
	String title;
	String link;
	String description;
	Date creationDate;

	String imageLink;

	boolean enabled;


	FeedItem(int id,
			 int feedId,
			 String title,
			 String link,
			 String description,
			 String imageLink,
			 boolean enabled,
			 Date creationDate) {
		this.id = id;
		this.feedId = feedId;
		this.title = title;
		this.link = link;
		this.description = description;
		this.imageLink = imageLink;
		this.enabled = enabled;
		this.creationDate = creationDate;
	}

//	FeedItem(int id, String title, String imageLink, boolean enabled) {
//		this.id = id;
//		this.title = title;
//		this.imageLink = imageLink;
//		this.enabled = enabled;
//	}

	boolean imageIsDownloaded(String folder) {
		File file = new File(folder + getImageName());
		return file.exists();
	}

	String getImageName() {
		if (imageLink != null) {
			return String.valueOf(id) + "_" + imageLink.substring(imageLink.lastIndexOf('/') + 1);
		} else {
			return "";
		}
	}

//	String getIconName() {
//		if (imageLink != null) {
//			return "icon_" + String.valueOf(id) + "_" + imageLink.substring(imageLink.lastIndexOf('/') + 1);
//		} else {
//			return "";
//		}
//	}
}
