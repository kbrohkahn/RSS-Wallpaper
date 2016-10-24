package com.brohkahn.rsswallpaper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

class FeedDBHelper extends SQLiteOpenHelper {
	private static final int DATABASE_VERSION = 1;
//	private static final String TAG = "FeedDBHelper";

	private static final String DB_NAME = "RSS_WALLPAPER.DB";

	private static final String SQL_CREATE_ENTRIES_TABLE =
			"CREATE TABLE " + FeedItemDBEntry.TABLE_NAME + " (" +
					FeedItemDBEntry._ID + " INTEGER PRIMARY KEY, " +
					FeedItemDBEntry.COLUMN_TITLE + " TEXT, " +
					FeedItemDBEntry.COLUMN_LINK + " TEXT, " +
					FeedItemDBEntry.COLUMN_DESCRIPTION + " TEXT, " +
					FeedItemDBEntry.COLUMN_CREATION_DATE + " LONG, " +
					FeedItemDBEntry.COLUMN_IMAGE_LINK + " TEXT, " +
					FeedItemDBEntry.COLUMN_DOWNLOADED + " INTEGER, " +
					FeedItemDBEntry.COLUMN_IMAGE_NAME + " TEXT, " +
					FeedItemDBEntry.COLUMN_ENABLED + " INTEGER, " +
					FeedItemDBEntry.COLUMN_RELATED_FEED + " INTEGER, " +
					" FOREIGN KEY (" + FeedItemDBEntry.COLUMN_RELATED_FEED + ") REFERENCES " + FeedDBEntry.TABLE_NAME + "(" + FeedDBEntry._ID + "));";

	private static final String SQL_CREATE_FEEDS_TABLE =
			"CREATE TABLE " + FeedDBEntry.TABLE_NAME + " (" +
					FeedDBEntry._ID + " INTEGER PRIMARY KEY, " +
					FeedDBEntry.COLUMN_SOURCE + " TEXT UNIQUE, " +
					FeedDBEntry.COLUMN_TITLE + " TEXT, " +
					FeedDBEntry.COLUMN_IMAGE_ON_WEB_PAGE + " INTEGER, " +
					FeedDBEntry.COLUMN_LINK + " TEXT, " +
					FeedDBEntry.COLUMN_DESCRIPTION + " TEXT, " +
					FeedDBEntry.COLUMN_ENTRY_IMAGE_LINK_TAG + " TEXT, " +
					FeedDBEntry.COLUMN_ENTRY_IMAGE_LINK_ATTRIBUTE + " TEXT, " +
					FeedDBEntry.COLUMN_ENABLED + " INTEGER)";

//    private static final String SQL_ALTER_TABLE_V2 =
//            "ALTER TABLE " + FeedItemDBEntry.TABLE_NAME +
//                    " ADD COLUMN " + FeedItemDBEntry.COLUMN_STACK_TRACE + " TEXT;";


	private static FeedDBHelper instance;

	static FeedDBHelper getHelper(Context context) {
		if (instance == null) {
			instance = new FeedDBHelper(context);
		}

		return instance;
	}

	private FeedDBHelper(Context context) {
		super(context, DB_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SQL_CREATE_ENTRIES_TABLE);
		db.execSQL(SQL_CREATE_FEEDS_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        if (oldVersion < 2) {
//            db.execSQL(SQL_ALTER_TABLE_V2);
//        }
	}

	long saveFeedEntry(int feedId,
					   String title,
					   String link,
					   String description,
					   String imageLink) {
		ContentValues values = new ContentValues();
		values.put(FeedItemDBEntry.COLUMN_RELATED_FEED, feedId);
		values.put(FeedItemDBEntry.COLUMN_TITLE, title);
		values.put(FeedItemDBEntry.COLUMN_LINK, link);
		values.put(FeedItemDBEntry.COLUMN_DESCRIPTION, description);
		values.put(FeedItemDBEntry.COLUMN_IMAGE_LINK, imageLink);
		values.put(FeedItemDBEntry.COLUMN_ENABLED, 1);
		values.put(FeedItemDBEntry.COLUMN_DOWNLOADED, 0);
		values.put(FeedItemDBEntry.COLUMN_CREATION_DATE, new Date().getTime());

		SQLiteDatabase db = getWritableDatabase();
		return db.insert(FeedItemDBEntry.TABLE_NAME, null, values);
	}

	boolean updateImageDownload(int id, String imageName) {
		String query = String.format(Locale.US, "UPDATE %s SET %s=%d, %s='%s' WHERE %s=%d",
									 FeedItemDBEntry.TABLE_NAME,
									 FeedItemDBEntry.COLUMN_DOWNLOADED,
									 imageName.equals("") ? 0 : 1,
									 FeedItemDBEntry.COLUMN_IMAGE_NAME,
									 imageName.replace("'", "''"),
									 FeedItemDBEntry._ID,
									 id
		);

		return runItemUpdateQuery(query);
	}

	boolean updateImageEnabled(int id, boolean enabled) {
		String query = String.format(Locale.US, "UPDATE %s SET %s=%d WHERE %s=%d",
									 FeedItemDBEntry.TABLE_NAME,
									 FeedItemDBEntry.COLUMN_ENABLED,
									 enabled ? 1 : 0,
									 FeedItemDBEntry._ID,
									 id
		);

		return runItemUpdateQuery(query);
	}

	private boolean runItemUpdateQuery(String query) {
		SQLiteDatabase db = getWritableDatabase();
		Cursor cursor = db.rawQuery(query, null);
		boolean success = cursor.moveToFirst() && cursor.getInt(0) == 1;
		cursor.close();
		return success;
	}

	FeedItem getFeedItem(int id) {
		String query = String.format(Locale.US, "SELECT * FROM %s where %s=%d",
									 FeedItemDBEntry.TABLE_NAME,
									 FeedItemDBEntry._ID,
									 id
		);
		List<FeedItem> items = getItems(query);
		if (items.size() > 0) {
			return items.get(0);
		} else {
			return null;
		}
	}

	List<FeedItem> getRecentItems(int count, int feedId) {
		String query = String.format(Locale.US, "SELECT * FROM %s WHERE %s=1 AND %s=%d ORDER BY %s DESC LIMIT %d",
									 FeedItemDBEntry.TABLE_NAME,
									 FeedItemDBEntry.COLUMN_ENABLED,
									 FeedItemDBEntry.COLUMN_RELATED_FEED,
									 feedId,
									 FeedItemDBEntry.COLUMN_CREATION_DATE,
									 count
		);
		return getItems(query);
	}

	List<FeedItem> getAllItems() {
		String query = String.format(Locale.US, "SELECT * FROM %s", FeedItemDBEntry.TABLE_NAME);
		return getItems(query);
	}

	boolean feedItemExists(String imageLink, int feedId) {
		String query = String.format(Locale.US, "SELECT * FROM %s WHERE %s='%s' AND %s=%d",
									 FeedItemDBEntry.TABLE_NAME,
									 FeedItemDBEntry.COLUMN_IMAGE_LINK,
									 imageLink,
									 FeedItemDBEntry.COLUMN_RELATED_FEED,
									 feedId
		);
		return getItems(query).size() > 0;
	}

	private List<FeedItem> getItems(String query) {
		SQLiteDatabase db = getWritableDatabase();
		Cursor cursor = db.rawQuery(query, null);

		List<FeedItem> items = new ArrayList<>();
		for (int i = 0; i < cursor.getCount(); i++) {
			cursor.moveToPosition(i);

			int id = cursor.getInt(cursor.getColumnIndexOrThrow(FeedItemDBEntry._ID));
			String title = cursor.getString(cursor.getColumnIndexOrThrow(FeedItemDBEntry.COLUMN_TITLE));
			String link = cursor.getString(cursor.getColumnIndexOrThrow(FeedItemDBEntry.COLUMN_LINK));
			String description = cursor.getString(cursor.getColumnIndexOrThrow(FeedItemDBEntry.COLUMN_DESCRIPTION));
			String imageName = cursor.getString(cursor.getColumnIndexOrThrow(FeedItemDBEntry.COLUMN_IMAGE_NAME));
			Date date = new Date();
			date.setTime(cursor.getLong(cursor.getColumnIndexOrThrow(FeedItemDBEntry.COLUMN_CREATION_DATE)));

			FeedItem item = new FeedItem(id, title, link, description, imageName, date);
			item.downloaded = cursor.getInt(cursor.getColumnIndexOrThrow(FeedItemDBEntry.COLUMN_DOWNLOADED)) == 1;
			item.enabled = cursor.getInt(cursor.getColumnIndexOrThrow(FeedItemDBEntry.COLUMN_ENABLED)) == 1;
			item.imageLink = cursor.getString(cursor.getColumnIndexOrThrow(FeedItemDBEntry.COLUMN_IMAGE_LINK));

			items.add(item);
		}

		cursor.close();
		return items;
	}

	static class FeedItemDBEntry implements BaseColumns {
		static final String TABLE_NAME = "feed_entries";
		static final String COLUMN_RELATED_FEED = "related_feed";
		static final String COLUMN_TITLE = "title";
		static final String COLUMN_LINK = "link";
		static final String COLUMN_DESCRIPTION = "description";
		static final String COLUMN_CREATION_DATE = "creation_date";
		static final String COLUMN_IMAGE_LINK = "image_link";
		static final String COLUMN_DOWNLOADED = "downloaded";
		static final String COLUMN_IMAGE_NAME = "image_name";
		static final String COLUMN_ENABLED = "enabled";


		static String[] getAllColumns() {
			return new String[]{_ID, COLUMN_RELATED_FEED, COLUMN_TITLE, COLUMN_LINK, COLUMN_IMAGE_LINK, COLUMN_DESCRIPTION, COLUMN_CREATION_DATE, COLUMN_DOWNLOADED, COLUMN_IMAGE_NAME, COLUMN_ENABLED};
		}
	}

	List<Feed> getAvailableFeeds() {
		String query = String.format(Locale.US, "SELECT * FROM %s WHERE %s=1 ORDER BY %s DESC",
									 FeedDBEntry.TABLE_NAME,
									 FeedDBEntry.COLUMN_TITLE,
									 FeedDBEntry.COLUMN_ENABLED
		);
		return getFeeds(query);

	}

	List<Feed> getAllFeeds() {
		String query = String.format(Locale.US, "SELECT * FROM %s ORDER BY %s DESC",
									 FeedDBEntry.TABLE_NAME,
									 FeedDBEntry.COLUMN_TITLE
		);
		return getFeeds(query);
	}

	Feed getFeed(int id) {
		String query = String.format(Locale.US, "SELECT * FROM %s WHERE %s=%d",
									 FeedDBEntry.TABLE_NAME,
									 FeedDBEntry._ID,
									 id
		);

		List<Feed> returnedFeeds = getFeeds(query);
		if (returnedFeeds.size() == 0) {
			return null;
		} else {
			return returnedFeeds.get(0);
		}
	}

	Feed getFeedFromSource(String source) {
		String query = String.format(Locale.US, "SELECT * FROM %s WHERE %s='%s'",
									 FeedDBEntry.TABLE_NAME,
									 FeedDBEntry.COLUMN_SOURCE,
									 source
		);

		List<Feed> returnedFeeds = getFeeds(query);
		if (returnedFeeds.size() == 0) {
			return null;
		} else {
			return returnedFeeds.get(0);
		}
	}

	private List<Feed> getFeeds(String query) {
		SQLiteDatabase db = getWritableDatabase();
		Cursor cursor = db.rawQuery(query, null);

		List<Feed> feeds = new ArrayList<>();
		for (int i = 0; i < cursor.getCount(); i++) {
			cursor.moveToPosition(i);

			int id = cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBEntry._ID));
			String title = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_TITLE));
			String source = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_SOURCE));
			String link = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_LINK));
			String description = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_DESCRIPTION));
			boolean imageOnWebPage = cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_IMAGE_ON_WEB_PAGE)) == 1;

			Feed feed = new Feed(id, source, title, link, description, imageOnWebPage);
			feed.enabled = cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_ENABLED)) == 1;
			feed.entryImageLinkTag = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_ENTRY_IMAGE_LINK_TAG));
			feed.entryImageLinkAttribute = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_ENTRY_IMAGE_LINK_ATTRIBUTE));

			feeds.add(feed);
		}

		cursor.close();
		return feeds;
	}

	long saveFeed(String source,
				  String title,
				  boolean imageOnWebPage,
				  String entryImageLinkTag,
				  String entryImageLinkAttribute) {
		ContentValues values = new ContentValues();
		values.put(FeedDBEntry.COLUMN_SOURCE, source);
		values.put(FeedDBEntry.COLUMN_TITLE, title);
		values.put(FeedDBEntry.COLUMN_IMAGE_ON_WEB_PAGE, imageOnWebPage);
		values.put(FeedDBEntry.COLUMN_ENTRY_IMAGE_LINK_TAG, entryImageLinkTag);
		values.put(FeedDBEntry.COLUMN_ENTRY_IMAGE_LINK_ATTRIBUTE, entryImageLinkAttribute);
		values.put(FeedDBEntry.COLUMN_ENABLED, 1);

		SQLiteDatabase db = getWritableDatabase();
		return db.insert(FeedDBEntry.TABLE_NAME, null, values);
	}

	boolean updateFeedInfo(int id, String title, String link, String description) {
		String query = String.format(Locale.US, "UPDATE %s SET %s='%s', %s='%s', %s='%s' WHERE %s='%d'",
									 FeedDBEntry.TABLE_NAME,
									 FeedDBEntry.COLUMN_TITLE,
									 title,
									 FeedDBEntry.COLUMN_LINK,
									 link,
									 FeedDBEntry.COLUMN_DESCRIPTION,
									 description,
									 FeedDBEntry._ID,
									 id
		);
		return runFeedUpdateQuery(query);
	}

	boolean updateFeedEnabled(int id, boolean enabled) {
		String query = String.format(Locale.US, "UPDATE %s SET %s=%d WHERE %s='%s'",
									 FeedDBEntry.TABLE_NAME,
									 FeedDBEntry.COLUMN_ENABLED,
									 enabled ? 1 : 0,
									 FeedDBEntry._ID,
									 id
		);
		return runFeedUpdateQuery(query);
	}

	private boolean runFeedUpdateQuery(String query) {
		SQLiteDatabase db = getWritableDatabase();
		Cursor cursor = db.rawQuery(query, null);
		boolean success = cursor.moveToFirst() && cursor.getInt(0) == 1;
		cursor.close();
		return success;
	}

	static class FeedDBEntry implements BaseColumns {
		static final String TABLE_NAME = "feeds";
		static final String COLUMN_SOURCE = "source";
		static final String COLUMN_TITLE = "title";
		static final String COLUMN_LINK = "link";
		static final String COLUMN_IMAGE_ON_WEB_PAGE = "image_on_web_page";
		static final String COLUMN_DESCRIPTION = "description";
		static final String COLUMN_ENABLED = "enabled";
		static final String COLUMN_ENTRY_IMAGE_LINK_TAG = "entryImageLinkTag";
		static final String COLUMN_ENTRY_IMAGE_LINK_ATTRIBUTE = "entryImageLinkAttribute";

		static String[] getAllColumns() {
			return new String[]{_ID, COLUMN_SOURCE, COLUMN_TITLE, COLUMN_LINK, COLUMN_IMAGE_ON_WEB_PAGE, COLUMN_ENABLED, COLUMN_ENTRY_IMAGE_LINK_TAG, COLUMN_ENTRY_IMAGE_LINK_ATTRIBUTE};
		}
	}
}
