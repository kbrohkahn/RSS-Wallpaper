package com.brohkahn.loggerlibrary;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class LogViewEntry extends AppCompatActivity {
	public static final String EXTRA_KEY_LOG_ENTRY_ID = "logEntryID";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_log_view_entry);

		Toolbar toolbar = (Toolbar) findViewById(R.id.log_view_entry_toolbar);
		setSupportActionBar(toolbar);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		// get entry from ID
		int id = getIntent().getIntExtra(EXTRA_KEY_LOG_ENTRY_ID, -1);
		LogDBHelper helper = LogDBHelper.getHelper(getApplicationContext());
		LogEntry entry = helper.getLogEntry(id);
		helper.close();


		findViewById(R.id.log_entry_layout).setBackgroundColor(ContextCompat.getColor(this, LogEntry.getColorId(entry.level)));

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


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		int id = item.getItemId();

		if (id == android.R.id.home) {
			finish();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
}
