package com.brohkahn.rsswallpaper;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Xml;

import com.brohkahn.loggerlibrary.ErrorHandler;
import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class DownloadRSSService extends IntentService {
	private static final String TAG = "DownloadRSSService";

	private static final String ACTION_DOWNLOAD_RSS = "com.brohkahn.nasawallpaper.action.download_rss";

	private static final int MAX_URL_CHARS = 2083;
	private static final String[] imageSuffices = {".png", ".jpg", ".jpeg"};
	private static final String[] absoluteURLPrefixes = {"http://", "https://"};
	private static final String[] relativeURLImagePrefixes = {"href=\"", "src=\""};

	private static LogDBHelper logDBHelper;
	private static FeedDBHelper feedDBHelper;

	public DownloadRSSService() {
		super("DownloadRSSService");
	}

	public static void startDownloadRSSAction(Context context) {
		Intent intent = new Intent(context, DownloadRSSService.class);
		intent.setAction(ACTION_DOWNLOAD_RSS);
		context.startService(intent);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			String action = intent.getAction();
			if (ACTION_DOWNLOAD_RSS.equals(action)) {
				startDownload();
			}
		}
	}

	public void startDownload() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		Resources resources = getResources();
		boolean wifiOnly = preferences.getBoolean(resources.getString(R.string.key_update_wifi_only), false);
		int currentFeedId = Integer.parseInt(preferences.getString(resources.getString(R.string.key_current_feed), "0"));
		int numberToDownload = Integer.parseInt(preferences.getString(resources.getString(R.string.key_number_to_rotate), "7"));

		// check if we can download anything based on internet connection
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

		String message;
		boolean canDownload;
		if (activeNetwork == null) {
			message = "Not connected to internet, unable to download feeds.";
			canDownload = false;
		} else {
			boolean wifiConnection = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
			if (wifiOnly && !wifiConnection) {
				message = "Not connected to Wifi, unable to download feeds.";
				canDownload = false;
			} else {
				canDownload = true;
				if (wifiOnly) {
					message = "Connected to Wifi, starting download of feeds.";
				} else {
					message = "Connected to internet, starting download of feeds.";
				}
			}
		}

		logDBHelper = LogDBHelper.getHelper(this);
		feedDBHelper = FeedDBHelper.getHelper(this);
		logEvent(message, "startDownloadIntent()", LogEntry.LogLevel.Message);

		if (canDownload) {
			List<Feed> allFeeds = feedDBHelper.getAllFeeds();
			if (allFeeds.size() == 0) {
				allFeeds.add(Constants.getBuiltInFeed());
			}

			for (Feed feed : allFeeds) {
				saveFeedItems(feed);
			}

			List<FeedItem> recentItems = feedDBHelper.getRecentItems(numberToDownload, currentFeedId);
			for (FeedItem item : recentItems) {
				if (!item.downloaded) {
					DownloadImageService.startDownloadImageAction(this);
					break;
				}
			}
		}

		feedDBHelper.close();
		feedDBHelper = null;

		logDBHelper.close();
		logDBHelper = null;

		stopSelf();

	}

	/**
	 * Handle action Foo in the provided background thread with the provided
	 * parameters.
	 */
	private void saveFeedItems(Feed feed) {
		try {
			URL url = new URL(feed.source);
			URLConnection connection = url.openConnection();
			connection.connect();

			// download the file
			logEvent("Downloading RSS file.", "saveFeedItems(String urlString)", LogEntry.LogLevel.Message);
			InputStream input = new BufferedInputStream(url.openStream(), 8192);

			// parse xml
			logEvent("Parsing XML.", "saveFeedItems(String urlString)", LogEntry.LogLevel.Message);
			FeedParser feedParser = new FeedParser(feed);
			feedParser.parse(input);
			input.close();

			logEvent("XML parse complete.", "saveFeedItems(String urlString)", LogEntry.LogLevel.Message);

		} catch (XmlPullParserException | ParseException | IOException e) {
			Log.e("Error: ", e.getMessage());
			logException(e, "saveFeedItems(String urlString)");
		}
	}

	private static String parseLinkFromText(String text, String url) {
		String link = null;
		for (String imageExtension : imageSuffices) {
			int endIndex = text.indexOf(imageExtension);
			if (endIndex > -1) {
				int endIndexWithExtension = endIndex + imageExtension.length();
				int startIndex = -1;

				// check for absolute URLs
				for (String prefix : absoluteURLPrefixes) {
					int index = text.lastIndexOf(prefix, endIndex);
					if (index > startIndex) {
						startIndex = index;
					}
				}

				if (startIndex > -1 && startIndex + MAX_URL_CHARS > endIndexWithExtension) {
					link = text.substring(startIndex, endIndexWithExtension);
					break;
				}

				// check for relative URLs
				for (String prefix : relativeURLImagePrefixes) {
					int index = text.lastIndexOf(prefix, endIndex);
					if (index > startIndex) {
						startIndex = index + prefix.length();
					}
				}
				if (startIndex > -1 && startIndex + MAX_URL_CHARS > endIndexWithExtension) {
					// get absolute url
					int absolutePathStart = url.indexOf("/", 10);
					if (absolutePathStart > -1) {
						url = url.substring(0, absolutePathStart);
					}

					// append backslash
					if (!url.endsWith("/")) {
						url += "/";
					}

					link = url + text.substring(startIndex, endIndexWithExtension);
					break;
				}
			}
		}
		return link;
	}

	public static class FeedParser {
		private static final String rssTag = "rss";
		private static final String channelTag = "channel";
		private static final String titleTag = "title";
		private static final String linkTag = "link";
		private static final String descriptionTag = "description";

		private static final String entryTag = "item";

		private static final String ns = null;

		private List<FeedItem> unsavedFeedItems;

		private Feed feed;

		FeedParser(Feed feed) {
			this.feed = feed;
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
			logEvent("Parsing feed.", "parse(InputStream in)", LogEntry.LogLevel.Message);

			unsavedFeedItems = new ArrayList<>();

			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(in, null);
			parser.nextTag();
			readFeed(parser);
			in.close();

			for (FeedItem item : unsavedFeedItems) {
				logEvent(String.format("Saving feed item title=%s.", item.title),
						 "readFeedItem(XmlPullParser parser)",
						 LogEntry.LogLevel.Message
				);

				feedDBHelper.saveFeedEntry(feed.id, item.title, item.link, item.description, item.imageLink);
			}
		}

		/**
		 * Decode a feed attached to an XmlPullParser.
		 *
		 * @param parser Incoming XMl
		 * @throws org.xmlpull.v1.XmlPullParserException on error parsing feed.
		 * @throws java.io.IOException                   on I/O error.
		 */
		private void readFeed(XmlPullParser parser) throws XmlPullParserException, IOException,
				ParseException {
			parser.require(XmlPullParser.START_TAG, ns, rssTag);
			while (parser.next() != XmlPullParser.END_TAG) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}

				String title = null, link = null, description = null;
				String name = parser.getName();
				if (name.equals(channelTag)) {
					parser.require(XmlPullParser.START_TAG, ns, channelTag);

					while (parser.next() != XmlPullParser.END_TAG) {
						if (parser.getEventType() != XmlPullParser.START_TAG) {
							continue;
						}

						String[] results;
						name = parser.getName();
						switch (name) {
							case titleTag:
							case linkTag:
							case descriptionTag:
								results = readBasicTag(parser, name);
								if (results != null) {
									switch (name) {
										case titleTag:
											title = results[0];
											break;
										case linkTag:
											link = results[0];
											break;
										case descriptionTag:
											description = results[0];
											break;
									}
								}
								break;
							case entryTag:
								readFeedItem(parser);
								break;
							default:
								skip(parser);
								break;
						}
					}
				}

				feedDBHelper.updateFeedInfo(feed.id, title, link, description);
			}
		}

		/**
		 * Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them
		 * off to their respective "read" methods for processing. Otherwise, skips the tag.
		 */
		private void readFeedItem(XmlPullParser parser)
				throws XmlPullParserException, IOException, ParseException {
			parser.require(XmlPullParser.START_TAG, ns, entryTag);

			String title = null, link = null, description = null, imageLink = null;
			while (parser.next() != XmlPullParser.END_TAG) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}

				String name = parser.getName();
				if (name.equals(titleTag)
						|| name.equals(linkTag)
						|| name.equals(descriptionTag)
						|| name.equals(feed.entryImageLinkTag)) {
					String[] results = readBasicTag(parser, name);
					if (results != null) {
						if (results[0] != null) {
							// match first result with correct variable
							switch (name) {
								case titleTag:
									title = results[0];
									break;
								case linkTag:
									link = results[0];
									break;
								case descriptionTag:
									description = results[0];
									break;
							}
						}

						if (!feed.imageOnWebPage && imageLink == null && results[1] != null) {
							imageLink = results[1];
						}
					}
				} else {
					skip(parser);
				}
			}

			if (feed.imageOnWebPage && link != null) {
				try {
					URL url = new URL(link);
					URLConnection connection = url.openConnection();
					connection.connect();

					// download the file
					InputStream inputStream = new BufferedInputStream(url.openStream());

					BufferedReader bReader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"), 8);
					StringBuilder sBuilder = new StringBuilder();

					String line;
					while ((line = bReader.readLine()) != null) {
						sBuilder.append(line + "\n");
					}

					inputStream.close();
					String fullHTML = sBuilder.toString();

					imageLink = parseLinkFromText(fullHTML, link);
				} catch (IOException e) {
					logException(e, "doInBackground(String... params)");
					Log.e("JSONException", "Error: " + e.toString());
				}
			}

			if (!feedDBHelper.feedItemExists(imageLink, feed.id)) {
				// create item and add to beginning of unsaved items list
				FeedItem item = new FeedItem(-1, title, link, description, "", null);
				item.imageLink = imageLink;
				unsavedFeedItems.add(0, item);
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
		private String[] readBasicTag(XmlPullParser parser, String tag)
				throws IOException, XmlPullParserException {
			parser.require(XmlPullParser.START_TAG, ns, tag);
			String result[] = new String[2];

			// get tag text
			if (parser.next() == XmlPullParser.TEXT) {
				result[0] = parser.getText();
			}

			// see if we need to get image link from this tag as well
			if (tag.equals(feed.entryImageLinkTag)) {
				result[1] = getImageLink(parser, result[0]);
			}

			// goto next tag if we read something
			if (result[0] != null) {
				parser.nextTag();
			}

			parser.require(XmlPullParser.END_TAG, ns, tag);
			return result;
		}

		private String getImageLink(XmlPullParser parser, String text)
				throws IOException, XmlPullParserException {
			String imageLink;
			if (!feed.entryImageLinkAttribute.equals("")) {
				// link is in an attribute
				imageLink = parser.getAttributeValue(null, feed.entryImageLinkAttribute);
			} else {
				// link is hidden somewhere in text
				imageLink = parseLinkFromText(text, "");
			}

			if (imageLink == null) {
				logEvent(String.format("Link not found in text %s.", text),
						 "getLink(XmlPullParser parser, String text)",
						 LogEntry.LogLevel.Warning
				);
			}

			return imageLink;
		}


//		/**
//		 * Processes link tags in the feed.
//		 */
//		private String readAlternateLink(XmlPullParser parser, String tag)
//				throws IOException, XmlPullParserException {
//			parser.require(XmlPullParser.START_TAG, ns, tag);
//			String link = parser.getAttributeValue(null, entryImageLinkAttribute);
//
////            if (relType.equals("alternate")) {
////                link = parser.getAttributeValue(null, "href");
////            }
//
//			while (true) {
//				if (parser.nextTag() == XmlPullParser.END_TAG) {
//					break;
//				}
//				// Intentionally break; consumes any remaining sub-tags.
//			}
//			return link;
//		}


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

	private static void logEvent(String message, String function, LogEntry.LogLevel level) {
		logDBHelper.saveLogEntry(message, null, TAG, function, level);
	}

	private static void logException(Exception e, String function) {
		logDBHelper.saveLogEntry(e.getLocalizedMessage(), ErrorHandler.getStackTraceString(e), TAG, function, LogEntry.LogLevel.Error);
	}
}
