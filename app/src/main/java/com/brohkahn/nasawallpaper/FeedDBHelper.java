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


    private static FeedDBHelper instance;
    private SQLiteDatabase db;

    public static FeedDBHelper getHelper(Context context, boolean writeAccess) {
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
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        if (oldVersion < 2) {
//            db.execSQL(SQL_ALTER_TABLE_V2);
//        }
    }

    public long saveFeedEntry(String title, String link, String imageLink, Date published) {
        ContentValues values = new ContentValues();
        values.put(FeedDBEntry.COLUMN_TITLE, title);
        values.put(FeedDBEntry.COLUMN_LINK, link);
        values.put(FeedDBEntry.COLUMN_IMAGE_LINK, imageLink);
        values.put(FeedDBEntry.COLUMN_PUBLISHED, published.getTime());
        values.put(FeedDBEntry.COLUMN_ENABLED, 1);
        values.put(FeedDBEntry.COLUMN_DOWNLOADED, 0);

        openDatabaseIfNecessary(true);
        return db.insert(FeedDBEntry.TABLE_NAME, null, values);
    }

    public boolean updateImageDownload(int id, String imageName) {
        String query = String.format(Locale.US, "UPDATE %s SET %s=1, %s='%s' WHERE %s=%d",
                FeedDBEntry.TABLE_NAME,
                FeedDBEntry.COLUMN_DOWNLOADED,
                FeedDBEntry.COLUMN_IMAGE_NAME,
                imageName.replace("'", "''"),
                FeedDBEntry._ID,
                id);

        return runUpdateQuery(query);
    }

    public boolean updateImageEnabled(int id, boolean enabled) {
        String query = String.format(Locale.US, "UPDATE %s SET %s=%d WHERE %s=%d",
                FeedDBEntry.TABLE_NAME,
                FeedDBEntry.COLUMN_ENABLED,
                enabled ? 1 : 0,
                FeedDBEntry._ID,
                id);

        return runUpdateQuery(query);
    }

    private boolean runUpdateQuery(String query) {
        openDatabaseIfNecessary(true);
        Cursor cursor = db.rawQuery(query, null);
        boolean success = cursor.moveToFirst() && cursor.getInt(0) == 1;
        cursor.close();
        return success;
    }

    public FeedItem getFeedItem(int id) {
        String query = String.format(Locale.US, "SELECT * FROM %s where %s=%d",
                FeedDBEntry.TABLE_NAME,
                FeedDBEntry._ID,
                id);
        List<FeedItem> items = getItems(query);
        if (items.size() > 0) {
            return items.get(0);
        } else {
            return null;
        }
    }

    public List<FeedItem> getItemsWithoutImages() {
        String query = String.format(Locale.US, "SELECT * FROM %s WHERE %s=0",
                FeedDBEntry.TABLE_NAME,
                FeedDBEntry.COLUMN_DOWNLOADED);
        return getItems(query);
    }

    public List<FeedItem> getRecentItems(int count) {
        String query = String.format(Locale.US, "SELECT * FROM %s WHERE %s=1 AND %s=1 AND %s is not null ORDER BY %s DESC LIMIT %d",
                FeedDBEntry.TABLE_NAME,
                FeedDBEntry.COLUMN_ENABLED,
                FeedDBEntry.COLUMN_DOWNLOADED,
                FeedDBEntry.COLUMN_IMAGE_NAME,
                FeedDBEntry.COLUMN_PUBLISHED,
                count);
        return getItems(query);
    }

    public List<FeedItem> getAllItems() {
        String query = String.format(Locale.US, "SELECT * FROM %s ORDER BY %s DESC",
                FeedDBEntry.TABLE_NAME,
                FeedDBEntry.COLUMN_PUBLISHED);
        return getItems(query);
    }

    public boolean feedItemExists(String imageName) {
        String query = String.format(Locale.US, "SELECT * FROM %s WHERE %s='%s'",
                FeedDBEntry.TABLE_NAME,
                FeedDBEntry.COLUMN_IMAGE_LINK,
                imageName);
        return getItems(query).size() > 0;
    }

    public List<FeedItem> getItems(String query) {
        try {
            openDatabaseIfNecessary(false);
            db = getWritableDatabase();
            Cursor cursor = db.rawQuery(query, null);

            List<FeedItem> items = new ArrayList<>();
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);

                int id = cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBEntry._ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_TITLE));
                String link = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_LINK));
                String imageName = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_IMAGE_NAME));
                Date date = new Date();
                date.setTime(cursor.getLong(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_PUBLISHED)));

                FeedItem item = new FeedItem(id, title, link, imageName, date);
                item.downloaded = cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_DOWNLOADED)) == 1;
                item.enabled = cursor.getInt(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_ENABLED)) == 1;
                item.imageLink = cursor.getString(cursor.getColumnIndexOrThrow(FeedDBEntry.COLUMN_IMAGE_LINK));

                items.add(item);
            }

            cursor.close();
            return items;
        } catch (Exception e) {
            throw e;
        }
    }

    private void openDatabaseIfNecessary(boolean writeAccess) {
        boolean unopenedDatabase = db == null || !db.isOpen();
        if (!unopenedDatabase && db.isReadOnly() && writeAccess) {
            db.close();
            unopenedDatabase = true;
        }

        if (unopenedDatabase) {
            if (writeAccess) {
                db = getWritableDatabase();
            } else {
                db = getReadableDatabase();
            }
        }
    }

    public static class FeedDBEntry implements BaseColumns {
        public static final String TABLE_NAME = "feed_entries";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_LINK = "link";
        public static final String COLUMN_IMAGE_LINK = "image_link";
        public static final String COLUMN_PUBLISHED = "published";
        public static final String COLUMN_DOWNLOADED = "downloaded";
        public static final String COLUMN_IMAGE_NAME = "image_name";
        public static final String COLUMN_ENABLED = "enabled";


        public static String[] getAllColumns() {
            return new String[]{_ID, COLUMN_TITLE, COLUMN_LINK, COLUMN_IMAGE_LINK, COLUMN_PUBLISHED, COLUMN_DOWNLOADED, COLUMN_IMAGE_NAME, COLUMN_ENABLED};
        }
    }
}

