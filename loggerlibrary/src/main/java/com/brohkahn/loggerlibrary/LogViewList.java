package com.brohkahn.loggerlibrary;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogViewList extends AppCompatActivity {
	LogViewListAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log_view_list);

		Toolbar toolbar = (Toolbar) findViewById(R.id.log_list_view_toolbar);
		setSupportActionBar(toolbar);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		adapter = new LogViewListAdapter(this, getCursorLoader().loadInBackground(), 0);

		ListView listView = (ListView) findViewById(R.id.log_list_view);
		listView.setAdapter(adapter);
	}

	private CursorLoader getCursorLoader() {
		return new CursorLoader(getApplicationContext(),
								Uri.EMPTY,
								LogDBHelper.LogDBEntry.getAllColumns(),
								null,
								null,
								LogDBHelper.LogDBEntry.COLUMN_TIME + " DESC"
		) {
			@Override
			public Cursor loadInBackground() {
				LogDBHelper dbHelper = LogDBHelper.getHelper(getApplicationContext());
				SQLiteDatabase db = dbHelper.getReadableDatabase();
				return db.query(LogDBHelper.LogDBEntry.TABLE_NAME,
								getProjection(),
								getSelection(),
								getSelectionArgs(),
								null,
								null,
								this.getSortOrder(),
								"5000"
				);
			}
		};
	}

	@Override
	protected void onDestroy() {
		adapter.getCursor().close();
		adapter.changeCursor(null);
		adapter = null;

		super.onDestroy();
	}

	public class LogViewListAdapter extends CursorAdapter {
		private SimpleDateFormat dateFormat;

		private LogViewListAdapter(Context context, Cursor cursor, int flags) {
			super(context, cursor, flags);

			dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
		}

		public void bindView(View view, Context context, Cursor cursor) {
			TextView dateTextView = (TextView) view.findViewById(R.id.list_item_date);
			long dateInMillis = cursor.getLong(cursor.getColumnIndexOrThrow(LogDBHelper.LogDBEntry.COLUMN_TIME));
			Date date = new Date();
			date.setTime(dateInMillis);
			dateTextView.setText(dateFormat.format(date));

			TextView messageTextView = (TextView) view.findViewById(R.id.list_item_message);
			String message = cursor.getString(cursor.getColumnIndexOrThrow(LogDBHelper.LogDBEntry.COLUMN_MESSAGE));
			messageTextView.setText(message);

			int logLevelInt = cursor.getInt(cursor.getColumnIndexOrThrow(LogDBHelper.LogDBEntry.COLUMN_LEVEL));
			LogEntry.LogLevel logLevel = LogEntry.LogLevel.values()[logLevelInt];
			view.setBackgroundColor(ContextCompat.getColor(context, LogEntry.getColorId(logLevel)));

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


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.log_list_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		int id = item.getItemId();
		if (id == android.R.id.home) {
			finish();
			return true;
		} else if (id == R.id.delete_logs) {
			showDeleteLogsDialog();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}


	public void showDeleteLogsDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.delete_logs_dialog_title)
			   .setMessage(R.string.delete_logs_dialog_message)
			   .setPositiveButton(R.string.delete_dialog_positive, new DialogInterface.OnClickListener() {
				   @Override
				   public void onClick(DialogInterface dialogInterface, int i) {
					   deleteLogs();
					   dialogInterface.dismiss();
				   }
			   })
			   .setNegativeButton(R.string.delete_dialog_negative, new DialogInterface.OnClickListener() {
				   @Override
				   public void onClick(DialogInterface dialogInterface, int i) {
					   dialogInterface.dismiss();
				   }
			   });
		builder.create().show();
	}

	public void deleteLogs() {
		LogDBHelper logDbHelper = LogDBHelper.getHelper(this);
		logDbHelper.deleteLogs();
		logDbHelper.close();

		adapter.getCursor().close();
		adapter.changeCursor(getCursorLoader().loadInBackground());
	}
}
