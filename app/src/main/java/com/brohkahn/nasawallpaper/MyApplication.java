package com.brohkahn.nasawallpaper;

import android.app.Application;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;
import com.brohkahn.loggerlibrary.LoggerApplication;

import java.io.PrintWriter;
import java.io.StringWriter;

public class MyApplication extends LoggerApplication {

    @Override
    public void onCreate() {
        LogDBHelper.DB_NAME = Constants.DATABASE_NAME;

        APPLICATION_NAME = getResources().getString(R.string.app_name);
        DEVELOPER_EMAIL = getResources().getString(R.string.developer_email);

        super.onCreate();
    }
}
