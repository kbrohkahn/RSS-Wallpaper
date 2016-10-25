package com.brohkahn.rsswallpaper;

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
				String entryImageLinkTag,
				String entryImageLinkAttribute,
				boolean imageOnWebPage,
				boolean enabled) {
		this.id = id;
		this.source = source;
		this.title = title;
		this.entryImageLinkTag = entryImageLinkTag;
		this.entryImageLinkAttribute = entryImageLinkAttribute;
		this.imageOnWebPage = imageOnWebPage;
		this.enabled = enabled;


	}

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
