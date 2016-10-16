package com.brohkahn.loggerlibrary;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class LogViewList extends AppCompatActivity {
    Cursor logEntryCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_view_list);

        LogDBHelper helper = new LogDBHelper(this, LogDBHelper.DB_NAME);

        SQLiteDatabase db = helper.getReadableDatabase();

        String query = String.format(Locale.US, "SELECT * FROM %s ORDER BY %s DESC",
                LogDBHelper.LogDBEntry.TABLE_NAME,
                LogDBHelper.LogDBEntry.COLUMN_TIME);
        logEntryCursor = db.rawQuery(query, null);


        LogViewListAdaper adapter = new LogViewListAdaper(this, logEntryCursor, 0);

        ListView listView = (ListView) findViewById(R.id.log_list_view);
        listView.setAdapter(adapter);

        helper.close();

    }

    @Override
    protected void onDestroy() {
        logEntryCursor.close();
        super.onDestroy();
    }

    public class LogViewListAdaper extends CursorAdapter {
        private SimpleDateFormat dateFormat;

        public LogViewListAdaper(Context context, Cursor cursor, int flags) {
            super(context, cursor, flags);

            dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
        }

        public void bindView(View view, Context context, Cursor cursor) {
            TextView dateTextView = (TextView) view.findViewById(R.id.list_item_date);
            long dateInMillis = cursor.getLong(cursor.getColumnIndexOrThrow(LogDBHelper.LogDBEntry.COLUMN_TIME));
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(dateInMillis);
            dateTextView.setText(dateFormat.format(calendar.getTime()));

            TextView messageTextView = (TextView) view.findViewById(R.id.list_item_message);
            String message = cursor.getString(cursor.getColumnIndexOrThrow(LogDBHelper.LogDBEntry.COLUMN_MESSAGE));
            messageTextView.setText(message);

            int logLevelInt = cursor.getInt(cursor.getColumnIndexOrThrow(LogDBHelper.LogDBEntry.COLUMN_LEVEL));
            LogEntry.LogLevel logLevel = LogEntry.LogLevel.values()[logLevelInt];
            view.setBackgroundColor(getResources().getColor(LogEntry.getColorId(logLevel)));

            final int ID = cursor.getInt(cursor.getColumnIndexOrThrow(LogDBHelper.LogDBEntry._ID));
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    displayLogEntry(ID);
                }
            });
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
        }
    }

    public void displayLogEntry(int ID) {
        Intent intent = new Intent(this, LogViewEntry.class);
        intent.putExtra(LogViewEntry.EXTRA_KEY_LOG_ENTRY_ID, ID);
        startActivity(intent);
    }
}
