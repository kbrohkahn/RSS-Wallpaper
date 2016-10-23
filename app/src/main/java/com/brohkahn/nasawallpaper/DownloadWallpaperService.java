package com.brohkahn.nasawallpaper;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
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
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;

public class DownloadWallpaperService extends IntentService {
	private static final String TAG = "DownloadWallpaperService";

	private static final String ACTION_DOWNLOAD_RSS = "com.brohkahn.nasawallpaper.action.DOWNLOAD_RSS";

	private static final int MAX_URL_CHARS = 2083;
	private static final String[] imageSuffices = {".png", ".jpg", ".jpeg"};
	private static final String[] absoluteURLPrefixes = {"http://", "https://"};
	private static final String[] relativeURLImagePrefixes = {"href=\"", "src=\""};

	private String imageDirectory;
	private static Feed currentFeed;

	private boolean noInitialItems;

	private static LogDBHelper logDBHelper;
	private static FeedDBHelper feedDBHelper;

	public DownloadWallpaperService() {
		super("DownloadWallpaperService");
	}

	public static void startDownloadRSSAction(Context context) {
		Intent intent = new Intent(context, DownloadWallpaperService.class);
		intent.setAction(ACTION_DOWNLOAD_RSS);
		context.startService(intent);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		Resources resources = getResources();
		imageDirectory = preferences.getString(resources.getString(R.string.key_image_directory), getFilesDir()
				.getPath() + "/");
		int currentFeedId = Integer.parseInt(preferences.getString(resources.getString(R.string.key_current_feed), "0"));

		logDBHelper = LogDBHelper.getHelper(this);
		feedDBHelper = FeedDBHelper.getHelper(this);

		currentFeed = feedDBHelper.getFeed(currentFeedId);
		if (currentFeed == null) {
			currentFeed = Constants.getBuiltInFeed();
		}
		noInitialItems = feedDBHelper.getRecentItems(1, currentFeedId).size() == 0;

		if (intent != null) {
			String action = intent.getAction();
			if (ACTION_DOWNLOAD_RSS.equals(action)) {
				downloadFeedItems(currentFeed.source);
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
			logEvent("Downloading RSS file.", "downloadFeedItems(String urlString)", LogEntry.LogLevel.Message);
			InputStream input = new BufferedInputStream(url.openStream(), 8192);

			// parse xml
			logEvent("Parsing XML.", "downloadFeedItems(String urlString)", LogEntry.LogLevel.Message);
			FeedParser feedParser = new FeedParser();
			feedParser.parse(input);
			input.close();

			List<FeedItem> entriesNeedingDownload = feedDBHelper.getItemsWithoutImages();
			logEvent(String.format(Locale.US, "XML parse complete, need to download %d images.", entriesNeedingDownload
							 .size()),
					 "downloadFeedItems(String urlString)",
					 LogEntry.LogLevel.Message
			);
			for (FeedItem entry : entriesNeedingDownload) {
				downloadFeedItem(entry);
			}

			downloadComplete();
		} catch (XmlPullParserException | ParseException | IOException e) {
			Log.e("Error: ", e.getMessage());
			logException(e, "downloadFeedItems(String urlString)");
		}
	}

	private void downloadFeedItem(FeedItem entry) {
		try {
			if (entry.imageLink == null) {
				logEvent(String.format(Locale.US, "No image link for %s found.", entry.title),
						 "downloadFeedItem(FeedItem entry)",
						 LogEntry.LogLevel.Warning
				);
				return;
			}

			logEvent(String.format(Locale.US, "Downloading feed image for %s.", entry.title),
					 "downloadFeedItem(FeedItem entry)",
					 LogEntry.LogLevel.Message
			);

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

			feedDBHelper.updateImageDownload(entry.id, imageName);

			logEvent(String.format(Locale.US, "Successfully downloaded and saved feed image for %s.", entry.title),
					 "downloadFeedItem(FeedItem entry)",
					 LogEntry.LogLevel.Message
			);

		} catch (Exception e) {
			Log.e("Error: ", e.getMessage());
			logException(e, "downloadFeedItem(FeedItem entry)");
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

	private void downloadComplete() {
		if (noInitialItems) {
			Intent intent = new Intent(Constants.SET_WALLPAPER_ACTION);
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		}

		feedDBHelper.close();
		feedDBHelper = null;

		logDBHelper.close();
		logDBHelper = null;

		stopSelf();
	}

	public static class FeedParser {

		private static final String rssTag = "rss";
		private static final String channelTag = "channel";
		private static final String titleTag = "title";
		private static final String linkTag = "link";
		private static final String descriptionTag = "description";

		private static final String entryTag = "item";

		private static final String ns = null;

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

				feedDBHelper.updateFeedInfo(currentFeed.id, title, link, description);
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
						|| name.equals(currentFeed.entryImageLinkTag)) {
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

						if (!currentFeed.imageOnWebPage && imageLink == null && results[1] != null) {
							imageLink = results[1];
						}
					}
				} else {
					skip(parser);
				}
			}

			if (currentFeed.imageOnWebPage && link != null) {
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

			if (!feedDBHelper.feedItemExists(imageLink, currentFeed.id)) {
				logEvent(String.format("Saving feed item title=%s.", title),
						 "readFeedItem(XmlPullParser parser)",
						 LogEntry.LogLevel.Message
				);

				feedDBHelper.saveFeedEntry(currentFeed.id, title, link, description, imageLink);
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
			if (tag.equals(currentFeed.entryImageLinkTag)) {
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
			if (!currentFeed.entryImageLinkAttribute.equals("")) {
				// link is in an attribute
				imageLink = parser.getAttributeValue(null, currentFeed.entryImageLinkAttribute);
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
