package com.brohkahn.loggerlibrary;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

public class LogViewEntry extends AppCompatActivity {
    public static final String EXTRA_KEY_LOG_ENTRY_ID = "logEntryID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_log_view_entry);

        int ID = getIntent().getIntExtra(EXTRA_KEY_LOG_ENTRY_ID, -1);
        LogEntry entry = LogDBHelper.getLogEntry(this, ID);

        findViewById(R.id.log_entry_layout).setBackgroundColor(getResources().getColor(LogEntry.getColorId(entry.level)));

        TextView timeTextView = (TextView) findViewById(R.id.log_entry_time);
        timeTextView.setText(entry.time.toString());

        TextView messageTextView = (TextView) findViewById(R.id.log_entry_message);
        messageTextView.setText(entry.message);

        TextView functionTextView = (TextView) findViewById(R.id.log_entry_function);
        functionTextView.setText(entry.function);

        TextView classTextView = (TextView) findViewById(R.id.log_entry_class);
        classTextView.setText(entry.logClass);

        if (entry.level == LogEntry.LogLevel.Error && entry.stackTrace != null && entry.stackTrace.length() > 0) {
            findViewById(R.id.log_entry_stack_trace_layout).setVisibility(View.VISIBLE);

            TextView stackTraceTextView = (TextView) findViewById(R.id.log_entry_stack_trace);
            stackTraceTextView.setText(entry.stackTrace);
        }
    }
}
