package com.brohkahn.nasawallpaper;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Date;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class DownloadWallpaperService extends IntentService {

    private static final String ACTION_DOWNLOAD_RSS = "com.brohkahn.nasawallpaper.action.DOWNLOAD_RSS";

    public static final String EXTRA_FEED_URL = "com.brohkahn.nasawallpaper.extra.FEED_URL";

    public DownloadWallpaperService() {
        super("DownloadWallpaperService");
    }

    public static void startDownloadRSSAction(Context context, String feedUrl) {
        Intent intent = new Intent(context, DownloadWallpaperService.class);
        intent.setAction(ACTION_DOWNLOAD_RSS);
        intent.putExtra(EXTRA_FEED_URL, feedUrl);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_DOWNLOAD_RSS.equals(action)) {
                final String feedUrl = intent.getStringExtra(EXTRA_FEED_URL);
                startDownload(feedUrl);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void startDownload(String urlString) {
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            connection.connect();

            // download the file
            InputStream input = new BufferedInputStream(url.openStream(), 8192);

            // parse xml
            FeedParser feedParser = new FeedParser();
            List<Entry> entries = feedParser.parse(input);

            for (Entry entry : entries) {
                downloadEntry(entry);
            }
            input.close();

            broadcastCompletion();
        } catch (Exception e) {
            Log.e("Error: ", e.getMessage());
        }

    }

    private void downloadEntry(Entry entry) {
        try {
            String fileExtension = entry.imageLink.substring(entry.imageLink.lastIndexOf('.'));
            String outputFilePath = getFilesDir().getPath() + "/" + entry.title + fileExtension;

            File oldFile = new File(outputFilePath);
            if (!oldFile.exists()) {
                URL url = new URL(entry.imageLink);
                URLConnection connection = url.openConnection();
                connection.connect();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                // Output stream
                OutputStream output = new FileOutputStream(outputFilePath);

                byte data[] = new byte[1024];

                int count;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();
            }
        } catch (Exception e) {
            Log.e("Error: ", e.getMessage());
        }
    }

    private void broadcastCompletion() {
        Intent localIntent = new Intent(Constants.DOWNLOAD_COMPLETE_BROADCAST);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    public static class Entry {
        private final String title;
        private final String link;
        private final String imageLink;
        private final long published;

        Entry(String title, String link, String imageLink, long published) {
            this.title = title;
            this.link = link;
            this.imageLink = imageLink;
            this.published = published;
        }
    }

    public static class FeedParser {
        private String feedStartTag = "rss";
        private String channelStartTag = "channel";
        private String entryTag = "item";
        private String entryTitleTag = "title";
        private String entryLinkTag = "link";
        private String entryPublishedTag = "pubDate";
        private String entryImageLinkTag = "enclosure";
        private String entryImageLinkAttribute = "url";


        // We don't use XML namespaces
        private static final String ns = null;

        /**
         * Parse an Atom feed, returning a collection of Entry objects.
         *
         * @param in Atom feed, as a stream.
         * @return List of {@link com.brohkahn.nasawallpaper.DownloadWallpaperService.Entry} objects.
         * @throws org.xmlpull.v1.XmlPullParserException on error parsing feed.
         * @throws java.io.IOException                   on I/O error.
         */
        public List<Entry> parse(InputStream in)
                throws XmlPullParserException, IOException, ParseException {
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(in, null);
                parser.nextTag();
                return readFeed(parser);
            } finally {
                in.close();
            }
        }

        /**
         * Decode a feed attached to an XmlPullParser.
         *
         * @param parser Incoming XMl
         * @return List of {@link com.brohkahn.nasawallpaper.DownloadWallpaperService.Entry} objects.
         * @throws org.xmlpull.v1.XmlPullParserException on error parsing feed.
         * @throws java.io.IOException                   on I/O error.
         */
        private List<Entry> readFeed(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
            List<Entry> entries = new ArrayList<>();

            parser.require(XmlPullParser.START_TAG, ns, feedStartTag);
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }

                String name = parser.getName();
                if (name.equals(channelStartTag)) {
                    parser.require(XmlPullParser.START_TAG, ns, channelStartTag);

                    while (parser.next() != XmlPullParser.END_TAG) {
                        if (parser.getEventType() != XmlPullParser.START_TAG) {
                            continue;
                        }

                        name = parser.getName();
                        if (name.equals(entryTag)) {
                            entries.add(readEntry(parser));
                        } else {
                            skip(parser);
                        }
                    }
                }
            }
            return entries;
        }

        /**
         * Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them
         * off to their respective "read" methods for processing. Otherwise, skips the tag.
         */
        private Entry readEntry(XmlPullParser parser)
                throws XmlPullParserException, IOException, ParseException {
            parser.require(XmlPullParser.START_TAG, ns, entryTag);
            String title = null;
            String link = null;
            String imageLink = null;
            long publishedOn = 0;

            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();
                if (name.equals(entryTitleTag)) {
                    title = readBasicTag(parser, entryTitleTag);
                } else if (name.equals(entryLinkTag)) {
                    link = readBasicTag(parser, entryLinkTag);
                } else if (name.equals(entryImageLinkTag)) {
                    imageLink = readAlternateLink(parser, entryImageLinkTag);
                } else if (name.equals(entryPublishedTag)) {
                    publishedOn = Date.parse(readBasicTag(parser, entryPublishedTag));
                } else {
                    skip(parser);
                }
            }
            return new Entry(title, link, imageLink, publishedOn);
        }


        /**
         * Reads the body of a basic XML tag, which is guaranteed not to contain any nested elements.
         * <p>
         * <p>You probably want to call readTag().
         *
         * @param parser Current parser object
         * @param tag    XML element tag name to parse
         * @return Body of the specified tag
         * @throws java.io.IOException
         * @throws org.xmlpull.v1.XmlPullParserException
         */
        private String readBasicTag(XmlPullParser parser, String tag)
                throws IOException, XmlPullParserException {
            parser.require(XmlPullParser.START_TAG, ns, tag);
            String result = readText(parser);
            parser.require(XmlPullParser.END_TAG, ns, tag);
            return result;
        }

        /**
         * Processes link tags in the feed.
         */
        private String readAlternateLink(XmlPullParser parser, String tag)
                throws IOException, XmlPullParserException {
            parser.require(XmlPullParser.START_TAG, ns, tag);
            String link = parser.getAttributeValue(null, entryImageLinkAttribute);

//            if (relType.equals("alternate")) {
//                link = parser.getAttributeValue(null, "href");
//            }

            while (true) {
                if (parser.nextTag() == XmlPullParser.END_TAG) break;
                // Intentionally break; consumes any remaining sub-tags.
            }
            return link;
        }

        /**
         * For the tags title and summary, extracts their text values.
         */
        private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
            String result = null;
            if (parser.next() == XmlPullParser.TEXT) {
                result = parser.getText();
                parser.nextTag();
            }
            return result;
        }

        /**
         * Skips tags the parser isn't interested in. Uses depth to handle nested tags. i.e.,
         * if the next tag after a START_TAG isn't a matching END_TAG, it keeps going until it
         * finds the matching END_TAG (as indicated by the value of "depth" being 0).
         */
        private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                throw new IllegalStateException();
            }
            int depth = 1;
            while (depth != 0) {
                switch (parser.next()) {
                    case XmlPullParser.END_TAG:
                        depth--;
                        break;
                    case XmlPullParser.START_TAG:
                        depth++;
                        break;
                }
            }
        }
    }
}
