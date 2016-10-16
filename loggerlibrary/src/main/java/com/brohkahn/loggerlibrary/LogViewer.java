package com.brohkahn.loggerlibrary;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Calendar;

public class LogViewer extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_viewer);

        Cursor cursor = LogDBHelper.getAllEntries(this);

        LogViewListAdaper adapter = new LogViewListAdaper(this, cursor, 0);

        ListView listView = (ListView) findViewById(R.id.log_list_view);
        listView.setAdapter(adapter);
    }


    public class LogViewListAdaper extends CursorAdapter {
        private LayoutInflater inflater;

        // Default constructor
        public LogViewListAdaper(Context context, Cursor cursor, int flags) {
            super(context, cursor, flags);
            inflater = getLayoutInflater();
        }

        public void bindView(View view, Context context, Cursor cursor) {
            TextView dateTextView = (TextView) view.findViewById(R.id.list_item_date);
            long dateInMillis = cursor.getLong(cursor.getColumnIndexOrThrow(LogDBHelper.LogDBEntry.COLUMN_TIME));
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(dateInMillis );
            dateTextView.setText(calendar.toString());

            TextView messageTextView = (TextView) view.findViewById(R.id.list_item_message);
            String message = cursor.getString(cursor.getColumnIndexOrThrow(LogDBHelper.LogDBEntry.COLUMN_MESSAGE));
            messageTextView.setText(message);
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return inflater.inflate(android.R.layout.simple_list_item_1, parent);
        }
    }
}
