package com.brohkahn.nasawallpaper;

import java.util.Date;

public class FeedItem {
    public long id;
    public String title;
    public String link;
    public String imageName;
    public Date published;

    FeedItem(long id, String title, String link, String imageName, Date published) {
        this.id = id;
        this.title = title;
        this.link = link;
        this.imageName = imageName;
        this.published = published;
    }
}
