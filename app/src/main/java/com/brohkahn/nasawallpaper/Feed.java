package com.brohkahn.nasawallpaper;

public class Feed {
	public int id;
	public String title;
	public String source;
	public boolean enabled;

	public String link;
	public String description;

	public String entryImageLinkTag;
	public String entryImageLinkAttribute;

	public Feed(int id, String title, String source) {
		this.id = id;
		this.title = title;
		this.source = source;
	}
}
