package com.brohkahn.nasawallpaper;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Xml;

import com.brohkahn.loggerlibrary.ErrorHandler;
import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DownloadWallpaperService extends IntentService {
    private static final String TAG = "DownloadWallpaperService";

    private static final String ACTION_DOWNLOAD_RSS = "com.brohkahn.nasawallpaper.action.DOWNLOAD_RSS";

    public static final String EXTRA_FEED_URL = "com.brohkahn.nasawallpaper.extra.FEED_URL";

    private String imageDirectory;

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
            String action = intent.getAction();
            if (ACTION_DOWNLOAD_RSS.equals(action)) {
                imageDirectory = PreferenceManager.getDefaultSharedPreferences(this).getString(getResources().getString(R.string.key_image_directory), getFilesDir().getPath() + "/");

                String feedUrl = intent.getStringExtra(EXTRA_FEED_URL);
                downloadFeedItems(feedUrl);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void downloadFeedItems(String urlString) {
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            connection.connect();

            // download the file
            logEvent(this, "Downloading RSS file.", "downloadFeedItems(String urlString)", LogEntry.LogLevel.Message);
            InputStream input = new BufferedInputStream(url.openStream(), 8192);

            // parse xml
            logEvent(this, "Parsing XML.", "downloadFeedItems(String urlString)", LogEntry.LogLevel.Message);
            FeedParser feedParser = new FeedParser(this);
            feedParser.parse(input);
            input.close();

            List<FeedItem> entriesNeedingDownload = FeedDBHelper.getItemsWithoutImages(this);
            logEvent(this,
                    String.format(Locale.US, "XML parse complete, need to download %d images.", entriesNeedingDownload.size()),
                    "downloadFeedItems(String urlString)",
                    LogEntry.LogLevel.Message);
            for (FeedItem entry : entriesNeedingDownload) {
                downloadFeedItem(entry);
            }

            broadcastCompletion();
        } catch (XmlPullParserException | ParseException | IOException e) {
            Log.e("Error: ", e.getMessage());
            logException(this, e, "downloadFeedItems(String urlString)");
        }
    }

    private void downloadFeedItem(FeedItem entry) {
        try {
            logEvent(this, String.format(Locale.US, "Downloading feed image for %s.", entry.title),
                    "downloadFeedItem(FeedItem entry)",
                    LogEntry.LogLevel.Message);

            // set image name to item title
            String imageName = entry.title.replace(' ', '_');

            // append extension to image name
            imageName += entry.imageLink.substring(entry.imageLink.lastIndexOf('.'));

            String outputFilePath = imageDirectory + imageName;

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

            output.flush();
            output.close();
            input.close();

            FeedDBHelper.updateItemImageDownload(this, entry.id, imageName);

            logEvent(this, String.format(Locale.US, "Successfully downloaded and saved feed image for %s.", entry.title),
                    "downloadFeedItem(FeedItem entry)",
                    LogEntry.LogLevel.Message);

        } catch (Exception e) {
            Log.e("Error: ", e.getMessage());
            logException(this, e, "downloa(FeedItem entry)");
        }
    }

    private void broadcastCompletion() {
        Intent intent = new Intent(Constants.SET_WALLPAPER_ACTION);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
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

        private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm zzz", Locale.US);

        private Context context;

        private static final String ns = null;

        private FeedParser(Context context) {
            this.context = context;
        }

        /**
         * Parse an Atom feed, returning a collection of FeedItem objects.
         *
         * @param in Atom feed, as a stream.
         * @throws org.xmlpull.v1.XmlPullParserException on error parsing feed.
         * @throws java.io.IOException                   on I/O error.
         */
        private void parse(InputStream in)
                throws XmlPullParserException, IOException, ParseException {
            logEvent(context, "Parsing feed.", "parse(InputStream in)", LogEntry.LogLevel.Message);
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            readFeed(parser);
            in.close();
        }

        /**
         * Decode a feed attached to an XmlPullParser.
         *
         * @param parser Incoming XMl
         * @throws org.xmlpull.v1.XmlPullParserException on error parsing feed.
         * @throws java.io.IOException                   on I/O error.
         */
        private void readFeed(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
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
                            readFeedItem(parser);
                        } else {
                            skip(parser);
                        }
                    }
                }
            }
        }

        /**
         * Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them
         * off to their respective "read" methods for processing. Otherwise, skips the tag.
         */
        private void readFeedItem(XmlPullParser parser)
                throws XmlPullParserException, IOException, ParseException {
            parser.require(XmlPullParser.START_TAG, ns, entryTag);
            String title = null;
            String link = null;
            String imageLink = null;
            Date publishedOn = new Date();

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
                    publishedOn = dateFormat.parse(readBasicTag(parser, entryPublishedTag));
                } else {
                    skip(parser);
                }
            }

            if (FeedDBHelper.feedItemExists(context, imageLink)) {
                logEvent(context,
                        String.format("Image %s already exists.", title),
                        "readFeedItem(XmlPullParser parser)",
                        LogEntry.LogLevel.Trace);

            } else {
                logEvent(context,
                        String.format("Saving feed item title=%s, link=%s, imageLink=%s, published=%s",
                                title, link, imageLink, publishedOn.toString()),
                        "readFeedItem(XmlPullParser parser)",
                        LogEntry.LogLevel.Message);
                FeedDBHelper.saveFeedItem(context, title, link, imageLink, publishedOn);
            }
        }

        /**
         * Reads the body of a basic XML tag, which is guaranteed not to contain any nested elements.
         * You probably want to call readTag().
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

    private static void logEvent(Context context, String message, String function, LogEntry.LogLevel level) {
        LogDBHelper.saveLogEntry(context, message, null, TAG, function, level);
    }

    private static void logException(Context context, Exception e, String function) {
        LogDBHelper.saveLogEntry(context, e.getLocalizedMessage(), ErrorHandler.getStackTraceString(e), TAG, function, LogEntry.LogLevel.Error);
    }
}
