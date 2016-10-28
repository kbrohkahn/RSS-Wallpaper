package com.brohkahn.loggerlibrary;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.Calendar;
import java.util.Locale;

public class LogDBHelper extends SQLiteOpenHelper {
	private static final int DATABASE_VERSION = 2;
//    private static final String TAG = "LogDBHelper";

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

//	private static LogDBHelper instance;

	public static synchronized LogDBHelper getHelper(Context context) {
		return new LogDBHelper(context);
//		if (instance == null) {
//			instance = new LogDBHelper(context.getApplicationContext());
//		}
//		return instance;
	}

	private LogDBHelper(Context context) {
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

	public void deleteLogs() {
		SQLiteDatabase db = getWritableDatabase();
		db.delete(LogDBEntry.TABLE_NAME, null, null);
	}

	public long saveLogEntry(String message,
							 String stackTrace,
							 String logClass,
							 String logFunction,
							 LogEntry.LogLevel level) {

		ContentValues values = new ContentValues();
		values.put(LogDBEntry.COLUMN_MESSAGE, message);
		values.put(LogDBEntry.COLUMN_STACK_TRACE, stackTrace);
		values.put(LogDBEntry.COLUMN_CLASS, logClass);
		values.put(LogDBEntry.COLUMN_FUNCTION, logFunction);
		values.put(LogDBEntry.COLUMN_TIME, java.util.Calendar.getInstance().getTimeInMillis());
		values.put(LogDBEntry.COLUMN_LEVEL, level.ordinal());

		SQLiteDatabase db = getWritableDatabase();
		return db.insert(LogDBEntry.TABLE_NAME, null, values);
	}

	LogEntry getLogEntry(int id) {
		String query = String.format(Locale.US, "SELECT * FROM %s where %s=%d",
									 LogDBEntry.TABLE_NAME,
									 LogDBEntry._ID,
									 id
		);

		SQLiteDatabase db = getReadableDatabase();
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

//	public List<LogEntry> getAllEntries() {
//		String query = String.format(Locale.US, "SELECT * FROM %s", LogDBEntry.TABLE_NAME);
//
//		SQLiteDatabase db = getReadableDatabase();
//		Cursor cursor = db.rawQuery(query, null);
//
//		List<LogEntry> entries = new ArrayList<>();
//		boolean entriesInCursor = cursor.moveToFirst();
//		while (entriesInCursor) {
//			int id = cursor.getInt(cursor.getColumnIndexOrThrow(LogDBEntry._ID));
//			String message = cursor.getString(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_MESSAGE));
//			String stackTrace = cursor.getString(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_STACK_TRACE));
//			String logClass = cursor.getString(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_CLASS));
//			String logFunction = cursor.getString(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_FUNCTION));
//			Calendar calendar = Calendar.getInstance();
//			calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_TIME)));
//			int levelIndex = cursor.getInt(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_LEVEL));
//
//			entries.add(new LogEntry(id, message, stackTrace, logClass, logFunction, calendar.getTime(), levelIndex));
//
//			entriesInCursor = cursor.moveToNext();
//		}
//
//		cursor.close();
//		return entries;
//
//	}
//	public List<LogEntry> getAllEntries() {
//		String query = String.format(Locale.US, "SELECT * FROM %s", LogDBEntry.TABLE_NAME);
//
//		SQLiteDatabase db = getReadableDatabase();
//		Cursor cursor = db.rawQuery(query, null);
//
//		List<LogEntry> entries = new ArrayList<>();
//		boolean entriesInCursor = cursor.moveToFirst();
//		while (entriesInCursor) {
//			int id = cursor.getInt(cursor.getColumnIndexOrThrow(LogDBEntry._ID));
//			String message = cursor.getString(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_MESSAGE));
//			String stackTrace = cursor.getString(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_STACK_TRACE));
//			String logClass = cursor.getString(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_CLASS));
//			String logFunction = cursor.getString(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_FUNCTION));
//			Calendar calendar = Calendar.getInstance();
//			calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_TIME)));
//			int levelIndex = cursor.getInt(cursor.getColumnIndexOrThrow(LogDBEntry.COLUMN_LEVEL));
//
//			entries.add(new LogEntry(id, message, stackTrace, logClass, logFunction, calendar.getTime(), levelIndex));
//
//			entriesInCursor = cursor.moveToNext();
//		}
//
//		cursor.close();
//		return entries;
//
//	}

	static class LogDBEntry implements BaseColumns {
		static final String TABLE_NAME = "log_entries";
		static final String COLUMN_MESSAGE = "message";
		static final String COLUMN_STACK_TRACE = "stack_trace";
		static final String COLUMN_CLASS = "class";
		static final String COLUMN_FUNCTION = "function";
		static final String COLUMN_TIME = "creation_time";
		static final String COLUMN_LEVEL = "level";

		static String[] getAllColumns() {
			return new String[]{_ID, COLUMN_MESSAGE, COLUMN_STACK_TRACE, COLUMN_CLASS, COLUMN_FUNCTION, COLUMN_TIME, COLUMN_LEVEL};
		}
	}
}
