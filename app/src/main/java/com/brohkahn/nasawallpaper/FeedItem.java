package com.brohkahn.nasawallpaper;

public class FeedItem {
    public String title;
    public String link;
    public String imageLink;
    public long published;

    FeedItem(String title, String link, String imageLink, long published) {
        this.title = title;
        this.link = link;
        this.imageLink = imageLink;
        this.published = published;
    }
}
