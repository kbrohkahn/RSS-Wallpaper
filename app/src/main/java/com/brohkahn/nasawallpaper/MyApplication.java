package com.brohkahn.nasawallpaper;

import android.app.Application;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.io.PrintWriter;
import java.io.StringWriter;

public class MyApplication extends Application {

    public void onCreate() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                handleUncaughtException(thread, e);
            }
        });
    }

    public void handleUncaughtException(Thread thread, Throwable e) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        e.printStackTrace();

        LogDBHelper.saveLogEntry(this, Constants.DATABASE_NAME, stringWriter.toString(), "N/A", "N/A", LogEntry.LogLevel.Error);

//        Intent intent = new Intent();
//        intent.setAction("com.mydomain.SEND_LOG"); // see step 5.
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // required when starting from Application
//        startActivity(intent);

        System.exit(1); // kill off the crashed app
    }

}
