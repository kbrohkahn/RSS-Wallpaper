package com.brohkahn.nasawallpaper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FeedDBHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String TAG = "FeedDBHelper";

    private static final String DB_NAME = "NASA_WALLPAPER.DB";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + FeedDBEntry.TABLE_NAME + " (" +
                    FeedDBEntry._ID + " INTEGER PRIMARY KEY, " +
                    FeedDBEntry.COLUMN_TITLE + " TEXT, " +
                    FeedDBEntry.COLUMN_LINK + " TEXT UNIQUE, " +
                    FeedDBEntry.COLUMN_IMAGE_LINK + " TEXT, " +
                    FeedDBEntry.COLUMN_PUBLISHED + " LONG, " +
                    FeedDBEntry.COLUMN_DOWNLOADED + " INTEGER, " +
                    FeedDBEntry.COLUMN_IMAGE_NAME + " TEXT UNIQUE, " +
                    FeedDBEntry.COLUMN_ENABLED + " INTEGER)";

//    private static final String SQL_ALTER_TABLE_V2 =
//            "ALTER TABLE " + FeedDBEntry.TABLE_NAME +
//                    " ADD COLUMN " + FeedDBEntry.COLUMN_STACK_TRACE + " TEXT;";

    private FeedDBHelper(Context context) {
        super(context, DB_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        if (oldVersion < 2) {
//            db.execSQL(SQL_ALTER_TABLE_V2);
//        }
    }

    private List<FeedItem> getItems(String query) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(query, null);

        List<FeedItem> items = new ArrayList<>();
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);

            int id = cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBEntry._ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_TITLE));
            String link = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_LINK));
            String imageName = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_IMAGE_NAME));
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_PUBLISHED)));

            FeedItem item = new FeedItem(id, title, link, imageName, calendar.getTime());
            item.downloaded = cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_DOWNLOADED)) == 1;
            item.enabled = cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_ENABLED)) == 1;
            item.imageLink = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_IMAGE_LINK));

            items.add(item);
        }

        cursor.close();
        return items;
    }

    private long saveFeedEntry(String title, String link, String imageLink, Date published) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(FeedDBEntry.COLUMN_TITLE, title);
        values.put(FeedDBEntry.COLUMN_LINK, link);
        values.put(FeedDBEntry.COLUMN_IMAGE_LINK, imageLink);
        values.put(FeedDBEntry.COLUMN_PUBLISHED, published.getTime());
        values.put(FeedDBEntry.COLUMN_ENABLED, 1);
        values.put(FeedDBEntry.COLUMN_DOWNLOADED, 0);

        return db.insert(FeedDBEntry.TABLE_NAME, null, values);
    }

    private boolean updateImageDownload(int id, String imageName) {
        String query = String.format(Locale.US, "UPDATE %s SET %s=1, %s='%s' WHERE %s=%d",
                FeedDBEntry.TABLE_NAME,
                FeedDBEntry.COLUMN_DOWNLOADED,
                FeedDBEntry.COLUMN_IMAGE_NAME,
                imageName.replace("'", "''"),
                FeedDBEntry._ID,
                id);

        return runUpdateQuery(query);
    }

    private boolean updateIgnoredImage(int id, boolean enabled) {
        String query = String.format(Locale.US, "UPDATE %s SET %s=%d, WHERE %s=%d",
                FeedDBEntry.TABLE_NAME,
                FeedDBEntry.COLUMN_ENABLED,
                enabled ? 1 : 0,
                FeedDBEntry._ID,
                id);

        return runUpdateQuery(query);
    }

    private boolean runUpdateQuery(String query) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        boolean success = cursor.moveToFirst() && cursor.getInt(0) == 1;
        cursor.close();
        return success;
    }

    public static long saveFeedItem(Context context, String title, String link, String imageLink, Date published) {
        FeedDBHelper helper = new FeedDBHelper(context);
        long resultId = helper.saveFeedEntry(title, link, imageLink, published);
        helper.close();
        return resultId;
    }

    public static FeedItem getFeedItem(Context context, int id) {
        String query = String.format(Locale.US, "SELECT * FROM %s where %s=%d",
                FeedDBEntry.TABLE_NAME,
                FeedDBEntry._ID,
                id);
        List<FeedItem> items = getItemsFromQuery(context, query);
        if (items.size() > 0) {
            return items.get(0);
        } else {
            return null;
        }
    }

    public static List<FeedItem> getItemsWithoutImages(Context context) {
        String query = String.format(Locale.US, "SELECT * FROM %s WHERE %s=0",
                FeedDBEntry.TABLE_NAME,
                FeedDBEntry.COLUMN_DOWNLOADED);
        return getItemsFromQuery(context, query);
    }

    public static List<FeedItem> getRecentItems(Context context, int count) {
        String query = String.format(Locale.US, "SELECT * FROM %s WHERE %s=1 ORDER BY %s DESC LIMIT %d",
                FeedDBEntry.TABLE_NAME,
                FeedDBEntry.COLUMN_ENABLED,
                FeedDBEntry.COLUMN_PUBLISHED,
                count);
        return getItemsFromQuery(context, query);
    }

    public static List<FeedItem> getAllItems(Context context) {
        String query = String.format(Locale.US, "SELECT * FROM %s ORDER BY %s DESC",
                FeedDBEntry.TABLE_NAME,
                FeedDBEntry.COLUMN_PUBLISHED);
        return getItemsFromQuery(context, query);
    }

    public static boolean feedItemExists(Context context, String imageName) {
        String query = String.format(Locale.US, "SELECT * FROM %s WHERE %s='%s'",
                FeedDBEntry.TABLE_NAME,
                FeedDBEntry.COLUMN_IMAGE_LINK,
                imageName);
        return getItemsFromQuery(context, query).size() > 0;
    }

    private static List<FeedItem> getItemsFromQuery(Context context, String query) {
        FeedDBHelper helper = new FeedDBHelper(context);
        List<FeedItem> items = helper.getItems(query);
        helper.close();
        return items;
    }

    public static boolean updateItemImageDownload(Context context, int id, String imageName) {
        FeedDBHelper helper = new FeedDBHelper(context);
        boolean success = helper.updateImageDownload(id, imageName);
        helper.close();
        return success;
    }

    public static boolean updateItemImageIgnore(Context context, int id, boolean enabled) {
        FeedDBHelper helper = new FeedDBHelper(context);
        boolean success = helper.updateIgnoredImage(id, enabled);
        helper.close();
        return success;
    }

    private static class FeedDBEntry implements BaseColumns {
        private static final String TABLE_NAME = "feed_entries";
        private static final String COLUMN_TITLE = "title";
        private static final String COLUMN_LINK = "link";
        private static final String COLUMN_IMAGE_LINK = "image_link";
        private static final String COLUMN_PUBLISHED = "published";
        private static final String COLUMN_DOWNLOADED = "downloaded";
        private static final String COLUMN_IMAGE_NAME = "image_name";
        private static final String COLUMN_ENABLED = "enabled";
    }
}

