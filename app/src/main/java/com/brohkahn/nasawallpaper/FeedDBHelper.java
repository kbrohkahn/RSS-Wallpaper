package com.brohkahn.nasawallpaper;

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

public class FeedDBHelper extends SQLiteOpenHelper {
	private static final int DATABASE_VERSION = 1;
	private static final String TAG = "FeedDBHelper";

	private static final String DB_NAME = "NASA_WALLPAPER.DB";

	private static final String SQL_CREATE_ENTRIES_TABLE =
			"CREATE TABLE " + FeedItemDBEntry.TABLE_NAME + " (" +
					FeedItemDBEntry._ID + " INTEGER PRIMARY KEY, " +
					FeedItemDBEntry.COLUMN_TITLE + " TEXT, " +
					FeedItemDBEntry.COLUMN_LINK + " TEXT, " +
					FeedItemDBEntry.COLUMN_CREATION_DATE + " LONG, " +
					FeedItemDBEntry.COLUMN_IMAGE_LINK + " TEXT, " +
					FeedItemDBEntry.COLUMN_DOWNLOADED + " INTEGER, " +
					FeedItemDBEntry.COLUMN_IMAGE_NAME + " TEXT UNIQUE, " +
					FeedItemDBEntry.COLUMN_ENABLED + " INTEGER)";

	private static final String SQL_CREATE_FEEDS_TABLE =
			"CREATE TABLE " + FeedDBEntry.TABLE_NAME + " (" +
					FeedDBEntry._ID + " INTEGER PRIMARY KEY, " +
					FeedDBEntry.COLUMN_TITLE + " TEXT, " +
					FeedDBEntry.COLUMN_SOURCE + " TEXT UNIQUE, " +
					FeedDBEntry.COLUMN_LINK + " TEXT, " +
					FeedDBEntry.COLUMN_DESCRIPTION + " TEXT, " +
					FeedDBEntry.COLUMN_ENTRY_IMAGE_LINK_TAG + " TEXT, " +
					FeedDBEntry.COLUMN_ENTRY_IMAGE_LINK_ATTRIBUTE + " TEXT, " +
					FeedDBEntry.COLUMN_ENABLED + " INTEGER)";

//    private static final String SQL_ALTER_TABLE_V2 =
//            "ALTER TABLE " + FeedItemDBEntry.TABLE_NAME +
//                    " ADD COLUMN " + FeedItemDBEntry.COLUMN_STACK_TRACE + " TEXT;";


	private static FeedDBHelper instance;

	public static FeedDBHelper getHelper(Context context) {
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

	public long saveFeedEntry(String title, String link, String imageLink) {
		ContentValues values = new ContentValues();
		values.put(FeedItemDBEntry.COLUMN_TITLE, title);
		values.put(FeedItemDBEntry.COLUMN_LINK, link);
		values.put(FeedItemDBEntry.COLUMN_IMAGE_LINK, imageLink);
		values.put(FeedItemDBEntry.COLUMN_ENABLED, 1);
		values.put(FeedItemDBEntry.COLUMN_DOWNLOADED, 0);

		SQLiteDatabase db = getWritableDatabase();
		return db.insert(FeedItemDBEntry.TABLE_NAME, null, values);
	}

	public boolean updateImageDownload(int id, String imageName) {
		String query = String.format(Locale.US, "UPDATE %s SET %s=1, %s='%s', %s=%d WHERE %s=%d",
									 FeedItemDBEntry.TABLE_NAME,
									 FeedItemDBEntry.COLUMN_DOWNLOADED,
									 FeedItemDBEntry.COLUMN_IMAGE_NAME,
									 imageName.replace("'", "''"),
									 FeedItemDBEntry.COLUMN_CREATION_DATE,
									 new Date().getTime(),
									 FeedItemDBEntry._ID,
									 id
		);

		return runUpdateQuery(query);
	}

	public boolean updateImageEnabled(int id, boolean enabled) {
		String query = String.format(Locale.US, "UPDATE %s SET %s=%d WHERE %s=%d",
									 FeedItemDBEntry.TABLE_NAME,
									 FeedItemDBEntry.COLUMN_ENABLED,
									 enabled ? 1 : 0,
									 FeedItemDBEntry._ID,
									 id
		);

		return runUpdateQuery(query);
	}

	private boolean runUpdateQuery(String query) {
		SQLiteDatabase db = getWritableDatabase();
		Cursor cursor = db.rawQuery(query, null);
		boolean success = cursor.moveToFirst() && cursor.getInt(0) == 1;
		cursor.close();
		return success;
	}

	public FeedItem getFeedItem(int id) {
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

	public List<FeedItem> getItemsWithoutImages() {
		String query = String.format(Locale.US, "SELECT * FROM %s WHERE %s=0",
									 FeedItemDBEntry.TABLE_NAME,
									 FeedItemDBEntry.COLUMN_DOWNLOADED
		);
		return getItems(query);
	}

	public List<FeedItem> getRecentItems(int count) {
		String query = String.format(Locale.US, "SELECT * FROM %s WHERE %s=1 AND %s=1 AND %s is not null ORDER BY %s DESC LIMIT %d",
									 FeedItemDBEntry.TABLE_NAME,
									 FeedItemDBEntry.COLUMN_ENABLED,
									 FeedItemDBEntry.COLUMN_DOWNLOADED,
									 FeedItemDBEntry.COLUMN_IMAGE_NAME,
									 FeedItemDBEntry.COLUMN_CREATION_DATE,
									 count
		);
		return getItems(query);
	}

	public List<FeedItem> getAllItems() {
		String query = String.format(Locale.US, "SELECT * FROM %s ORDER BY %s DESC",
									 FeedItemDBEntry.TABLE_NAME,
									 FeedItemDBEntry.COLUMN_CREATION_DATE
		);
		return getItems(query);
	}

	public boolean feedItemExists(String imageName) {
		String query = String.format(Locale.US, "SELECT * FROM %s WHERE %s='%s'",
									 FeedItemDBEntry.TABLE_NAME,
									 FeedItemDBEntry.COLUMN_IMAGE_LINK,
									 imageName
		);
		return getItems(query).size() > 0;
	}

	public List<FeedItem> getItems(String query) {
		try {
			SQLiteDatabase db = getWritableDatabase();
			Cursor cursor = db.rawQuery(query, null);

			List<FeedItem> items = new ArrayList<>();
			for (int i = 0; i < cursor.getCount(); i++) {
				cursor.moveToPosition(i);

				int id = cursor.getInt(cursor.getColumnIndexOrThrow(FeedItemDBEntry._ID));
				String title = cursor.getString(cursor.getColumnIndexOrThrow(FeedItemDBEntry.COLUMN_TITLE));
				String link = cursor.getString(cursor.getColumnIndexOrThrow(FeedItemDBEntry.COLUMN_LINK));
				String imageName = cursor.getString(cursor.getColumnIndexOrThrow(FeedItemDBEntry.COLUMN_IMAGE_NAME));
				Date date = new Date();
				date.setTime(cursor.getLong(cursor.getColumnIndexOrThrow(FeedItemDBEntry.COLUMN_CREATION_DATE)));

				FeedItem item = new FeedItem(id, title, link, imageName, date);
				item.downloaded = cursor.getInt(cursor.getColumnIndexOrThrow(FeedItemDBEntry.COLUMN_DOWNLOADED)) == 1;
				item.enabled = cursor.getInt(cursor.getColumnIndexOrThrow(FeedItemDBEntry.COLUMN_ENABLED)) == 1;
				item.imageLink = cursor.getString(cursor.getColumnIndexOrThrow(FeedItemDBEntry.COLUMN_IMAGE_LINK));

				items.add(item);
			}

			cursor.close();
			return items;
		} catch (Exception e) {
			throw e;
		}
	}

	public static class FeedItemDBEntry implements BaseColumns {
		public static final String TABLE_NAME = "feed_entries";
		public static final String COLUMN_TITLE = "title";
		public static final String COLUMN_LINK = "link";
		public static final String COLUMN_CREATION_DATE = "creation_date";
		public static final String COLUMN_IMAGE_LINK = "image_link";
		public static final String COLUMN_DOWNLOADED = "downloaded";
		public static final String COLUMN_IMAGE_NAME = "image_name";
		public static final String COLUMN_ENABLED = "enabled";


		public static String[] getAllColumns() {
			return new String[]{_ID, COLUMN_TITLE, COLUMN_LINK, COLUMN_IMAGE_LINK, COLUMN_CREATION_DATE, COLUMN_DOWNLOADED, COLUMN_IMAGE_NAME, COLUMN_ENABLED};
		}
	}

	public List<Feed> getAvailableFeeds() {
		String query = String.format(Locale.US, "SELECT * FROM %s WHERE %s=1 ORDER BY %s DESC",
									 FeedDBEntry.TABLE_NAME,
									 FeedDBEntry.COLUMN_TITLE,
									 FeedDBEntry.COLUMN_ENABLED
		);
		return getFeeds(query);

	}

	public List<Feed> getAllFeeds() {
		String query = String.format(Locale.US, "SELECT * FROM %s ORDER BY %s DESC",
									 FeedDBEntry.TABLE_NAME,
									 FeedDBEntry.COLUMN_TITLE
		);
		return getFeeds(query);
	}

	public Feed getFeed(int id) {
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

	private List<Feed> getFeeds(String query) {
		try {
			SQLiteDatabase db = getWritableDatabase();
			Cursor cursor = db.rawQuery(query, null);

			List<Feed> feeds = new ArrayList<>();
			for (int i = 0; i < cursor.getCount(); i++) {
				cursor.moveToPosition(i);

				int id = cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBEntry._ID));
				String title = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_TITLE));
				String source = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_SOURCE));

				Feed feed = new Feed(id, title, source);
				feed.link = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_LINK));
				feed.description = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_DESCRIPTION));
				feed.enabled = cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_ENABLED)) == 1;
				feed.entryImageLinkTag = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_ENTRY_IMAGE_LINK_TAG));
				feed.entryImageLinkAttribute = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_ENTRY_IMAGE_LINK_ATTRIBUTE));

				feeds.add(feed);
			}

			cursor.close();
			return feeds;
		} catch (Exception e) {
			throw e;
		}
	}

	public long saveFeed(String title, String link) {
		ContentValues values = new ContentValues();
		values.put(FeedDBEntry.COLUMN_TITLE, title);
		values.put(FeedDBEntry.COLUMN_SOURCE, link);
		values.put(FeedDBEntry.COLUMN_ENABLED, 1);

		SQLiteDatabase db = getWritableDatabase();
		return db.insert(FeedDBEntry.TABLE_NAME, null, values);
	}

	public boolean updateFeedInfo(int id, String title, String link, String description) {
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
		return runUpdateQuery(query);
	}

	private boolean updateFeedEnabled(int id, boolean enabled) {
		String query = String.format(Locale.US, "UPDATE %s SET %s=%d WHERE %s='%s'",
									 FeedDBEntry.TABLE_NAME,
									 FeedDBEntry.COLUMN_ENABLED,
									 enabled ? 1 : 0,
									 FeedDBEntry._ID,
									 id
		);
		return runUpdateQuery(query);
	}

	public boolean runFeedUpdateQuery(String query) {
		SQLiteDatabase db = getWritableDatabase();
		Cursor cursor = db.rawQuery(query, null);
		boolean success = cursor.moveToFirst() && cursor.getInt(0) == 1;
		cursor.close();
		return success;
	}

	public static class FeedDBEntry implements BaseColumns {
		public static final String TABLE_NAME = "feeds";
		public static final String COLUMN_TITLE = "title";
		public static final String COLUMN_SOURCE = "source";
		public static final String COLUMN_LINK = "link";
		public static final String COLUMN_DESCRIPTION = "description";
		public static final String COLUMN_ENABLED = "enabled";
		public static final String COLUMN_ENTRY_IMAGE_LINK_TAG = "entryImageLinkTag";
		public static final String COLUMN_ENTRY_IMAGE_LINK_ATTRIBUTE = "entryImageLinkAttribute";

		public static String[] getAllColumns() {
			return new String[]{_ID, COLUMN_TITLE, COLUMN_SOURCE, COLUMN_LINK, COLUMN_ENABLED, COLUMN_ENTRY_IMAGE_LINK_TAG, COLUMN_ENTRY_IMAGE_LINK_ATTRIBUTE};
		}
	}
}

