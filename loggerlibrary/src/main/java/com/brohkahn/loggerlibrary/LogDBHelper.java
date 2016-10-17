package com.brohkahn.loggerlibrary;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class LogDBHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 2;
    private static final String TAG = "LogDBHelper";

    private static final String DB_NAME = "LOG_ENTRIES.DB";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + LogDBEntry.TABLE_NAME + " (" +
                    LogDBEntry._ID + " INTEGER PRIMARY KEY," +
                    LogDBEntry.COLUMN_MESSAGE + " TEXT, " +
                    LogDBEntry.COLUMN_STACK_TRACE + " TEXT, " +
                    LogDBEntry.COLUMN_CLASS + " TEXT, " +
                    LogDBEntry.COLUMN_FUNCTION + " TEXT, " +
                    LogDBEntry.COLUMN_TIME + " REAL, " +
                    LogDBEntry.COLUMN_LEVEL + " TEXT)";

//    private static final String SQL_ALTER_TABLE_V2 =
//            "ALTER TABLE " + LogDBEntry.TABLE_NAME +
//                    " ADD COLUMN " + LogDBEntry.COLUMN_STACK_TRACE + " TEXT;";

    public LogDBHelper(Context context) {
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

    private long saveLogEntry(String message, String stackTrace, String logClass, String logFunction, LogEntry.LogLevel level) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(LogDBEntry.COLUMN_MESSAGE, message);
        values.put(LogDBEntry.COLUMN_STACK_TRACE, stackTrace);
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
            String stackTrace = cursor.getString(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_STACK_TRACE));
            String logClass = cursor.getString(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_CLASS));
            String logFunction = cursor.getString(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_FUNCTION));
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_TIME)));
            int levelIndex = cursor.getInt(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_LEVEL));

            entry = new LogEntry(id, message, stackTrace, logClass, logFunction, calendar.getTime(), levelIndex);
        }

        cursor.close();
        return entry;
    }

    private List<LogEntry> getAllEntries() {
        SQLiteDatabase db = getReadableDatabase();

        String query = String.format(Locale.US, "SELECT * FROM %s", LogDBEntry.TABLE_NAME);

        Cursor cursor = db.rawQuery(query, null);

        List<LogEntry> entries = new ArrayList<>();
        boolean entriesInCursor = cursor.moveToFirst();
        while (entriesInCursor) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(LogDBEntry._ID));
            String message = cursor.getString(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_MESSAGE));
            String stackTrace = cursor.getString(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_STACK_TRACE));
            String logClass = cursor.getString(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_CLASS));
            String logFunction = cursor.getString(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_FUNCTION));
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_TIME)));
            int levelIndex = cursor.getInt(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_LEVEL));

            entries.add(new LogEntry(id, message, stackTrace, logClass, logFunction, calendar.getTime(), levelIndex));

            entriesInCursor = cursor.moveToNext();
        }

        cursor.close();
        return entries;

    }

    public static void saveLogEntry(Context context, String message, String stackTrace, String tag, String function, LogEntry.LogLevel type) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, String.format(Locale.US, "%s at %s: %s", type.toString(), function, message));
        }

        LogDBHelper helper = new LogDBHelper(context);
        helper.saveLogEntry(message, stackTrace, tag, function, type);
        helper.close();
    }

    public static LogEntry getLogEntry(Context context, int id) {
        LogDBHelper helper = new LogDBHelper(context);
        LogEntry entry = helper.getLogEntry(id);
        helper.close();
        return entry;
    }
//
//    public static Cursor getAllEntries(Context context, String DB_NAME) {
//        LogDBHelper helper = new LogDBHelper(context, DB_NAME);
//
//        SQLiteDatabase db = helper.getReadableDatabase();
//
//        String query = String.format(Locale.US, "SELECT * FROM %s", LogDBEntry.TABLE_NAME);
//        Cursor cursor = db.rawQuery(query, null);
//
//        helper.close();
//        return cursor;
//    }

    public static class LogDBEntry implements BaseColumns {
        public static final String TABLE_NAME = "log_entries";
        public static final String COLUMN_MESSAGE = "message";
        public static final String COLUMN_STACK_TRACE = "stack_trace";
        public static final String COLUMN_CLASS = "class";
        public static final String COLUMN_FUNCTION = "function";
        public static final String COLUMN_TIME = "creation_time";
        public static final String COLUMN_LEVEL = "level";
    }
}
