package com.brohkahn.rsswallpaper;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

public class About extends AppCompatActivity {


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		Resources resources = getResources();

		String aboutPackageText = resources.getString(R.string.about_package_text, getPackageName());
		((TextView) findViewById(R.id.about_package)).setText(aboutPackageText);


		String versionName;
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {
			versionName = "Unknown";
		}

		String aboutVersionText = resources.getString(R.string.about_version_text, versionName);
		((TextView) findViewById(R.id.about_version)).setText(aboutVersionText);

		String aboutDeveloperText = resources.getString(R.string.about_developer_text, resources.getString(R.string.developer_name));
		((TextView) findViewById(R.id.about_developer)).setText(aboutDeveloperText);

		String aboutAppText = resources.getString(R.string.about_app_text, resources.getString(R.string.developer_email));
		((TextView) findViewById(R.id.about_app)).setText(aboutAppText);

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}

	}
}
