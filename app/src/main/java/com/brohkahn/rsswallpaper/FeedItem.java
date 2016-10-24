package com.brohkahn.rsswallpaper;

import java.util.Date;

public class FeedItem {
	public int id;
	public String title;
	public String link;
	public String description;
	public String imageName;
	public Date creationDate;

	public String imageLink;
	public boolean downloaded;
	public boolean enabled;

	FeedItem(int id,
			 String title,
			 String link,
			 String description,
			 String imageName,
			 Date creationDate) {
		this.id = id;
		this.title = title;
		this.link = link;
		this.description = description;
		this.imageName = imageName;
		this.creationDate = creationDate;
	}
}
