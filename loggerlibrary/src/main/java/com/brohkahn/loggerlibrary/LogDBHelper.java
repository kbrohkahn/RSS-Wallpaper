package com.brohkahn.loggerlibrary;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.Calendar;
import java.util.Locale;

public class LogDBHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String TAG = "LogDBHelper";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + LogDBEntry.TABLE_NAME + " (" +
                    LogDBEntry._ID + " INTEGER PRIMARY KEY," +
                    LogDBEntry.COLUMN_MESSAGE + " TEXT, " +
                    LogDBEntry.COLUMN_CLASS + " TEXT, " +
                    LogDBEntry.COLUMN_FUNCTION + " TEXT, " +
                    LogDBEntry.COLUMN_TIME + " REAL, " +
                    LogDBEntry.COLUMN_LEVEL + " TEXT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + LogDBEntry.TABLE_NAME;


    private LogDBHelper(Context context, String databaseName) {
        super(context, databaseName, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    private long saveLogEntry(String message, String logClass, String logFunction, LogEntry.LogLevel level) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(LogDBEntry.COLUMN_MESSAGE, message);
        values.put(LogDBEntry.COLUMN_CLASS, logClass);
        values.put(LogDBEntry.COLUMN_FUNCTION, logFunction);
        values.put(LogDBEntry.COLUMN_MESSAGE, message);
        values.put(LogDBEntry.COLUMN_TIME, java.util.Calendar.getInstance().getTimeInMillis());
        values.put(LogDBEntry.COLUMN_LEVEL, level.ordinal());

        return db.insert(LogDBEntry.TABLE_NAME, null, values);
    }

    private LogEntry getLogEntry(int id) {
        SQLiteDatabase db = getReadableDatabase();

        String query = String.format(Locale.US, "SELECT * FROM %s where %s=%d",
                LogDBEntry.TABLE_NAME,
                LogDBEntry._ID,
                id);

        Cursor cursor = db.rawQuery(query, null);

        LogEntry entry = null;
        if (cursor.moveToFirst()) {
            String message = cursor.getString(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_MESSAGE));
            String logClass = cursor.getString(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_CLASS));
            String logFunction = cursor.getString(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_FUNCTION));
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_TIME)));
            int levelIndex = cursor.getInt(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_LEVEL));

            entry = new LogEntry(id, message, logClass, logFunction, calendar, levelIndex);
        }

        cursor.close();
        return entry;
    }

    public static void saveLogEntry(Context context, String databaseName, String message, String tag, String function, LogEntry.LogLevel type) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, String.format(Locale.US, "%s at %s: %s", type.toString(), function, message));
        }

        LogDBHelper helper = new LogDBHelper(context, databaseName);
        helper.saveLogEntry(message, tag, function, type);
        helper.close();
    }

    public static LogEntry getLogEntry(Context context, String databaseName, int id) {
        LogDBHelper helper = new LogDBHelper(context, databaseName);
        LogEntry entry = helper.getLogEntry(id);
        helper.close();
        return entry;
    }

    private static class LogDBEntry implements BaseColumns {
        private static final String TABLE_NAME = "log_entries";
        private static final String COLUMN_MESSAGE = "message";
        private static final String COLUMN_CLASS = "class";
        private static final String COLUMN_FUNCTION = "function";
        private static final String COLUMN_TIME = "creation_time";
        private static final String COLUMN_LEVEL = "level";
    }
}
