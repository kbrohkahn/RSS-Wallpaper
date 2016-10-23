package com.brohkahn.nasawallpaper;

public class Feed {
	public int id;
	public String source;
	public String title;
	public String link;
	public String description;
	public boolean imageOnWebPage;
	public boolean enabled;

	public String entryImageLinkTag;
	public String entryImageLinkAttribute;

	public Feed(int id,
				String source,
				String title,
				String link,
				String description,
				boolean imageOnWebPage) {
		this.id = id;
		this.source = source;
		this.title = title;
		this.link = link;
		this.description = description;
		this.imageOnWebPage = imageOnWebPage;
	}
}
