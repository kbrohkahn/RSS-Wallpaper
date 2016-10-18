package com.brohkahn.nasawallpaper;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.util.Locale;

public class FeedItemView extends AppCompatActivity {
    public static final String TAG = "FeedItemView";
    public static final String EXTRA_KEY_FEED_ITEM_ID = "feedItemID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(com.brohkahn.loggerlibrary.R.layout.activity_log_view_entry);

        final int ID = getIntent().getIntExtra(EXTRA_KEY_FEED_ITEM_ID, -1);
        FeedItem item = FeedDBHelper.getFeedItem(this, ID);

        if (item == null) {
            LogDBHelper.saveLogEntry(this,
                    String.format(Locale.US, "Unable to find feed item with id of %d", ID),
                    null,
                    TAG,
                    "onCreate(Bundle savedInstanceState)",
                    LogEntry.LogLevel.Warning);
            finish();
            return;
        }

        setTitle(item.title);

        TextView linkTextView = (TextView) findViewById(R.id.feed_item_link);
        linkTextView.setText(item.link);

        TextView publishedTextView = (TextView) findViewById(R.id.feed_item_published);
        publishedTextView.setText(item.published.toString());

        CheckBox enabledCheckBox = (CheckBox) findViewById(R.id.feed_item_enabled);
        enabledCheckBox.setChecked(item.enabled);
        enabledCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                updateItemEnabled(ID, b);

            }
        });

        CheckBox downloadedCheckBox = (CheckBox) findViewById(R.id.feed_item_downloaded);
        downloadedCheckBox.setChecked(item.downloaded);
    }

    private void updateItemEnabled(int itemId, boolean enabled) {
        FeedDBHelper.updateItemImageEnabled(this, itemId, enabled);
    }
}
