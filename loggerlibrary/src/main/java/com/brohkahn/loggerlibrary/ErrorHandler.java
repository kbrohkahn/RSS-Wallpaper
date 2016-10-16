package com.brohkahn.loggerlibrary;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;

public class ErrorHandler implements Thread.UncaughtExceptionHandler {
    private Context context;
    private String appName;
    private String className;
    private String developerEmail;

    private boolean isInForeground;

    public ErrorHandler(Context context, boolean isInForeground) {
        this.context = context;
        this.appName = context.getResources().getString(R.string.app_name);
        this.developerEmail = context.getResources().getString(R.string.developer_email);
        this.className = context.getClass().getCanonicalName();

        this.isInForeground = isInForeground;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        final String errorString = getStackTraceString(e);
        e.printStackTrace();
        LogDBHelper.saveLogEntry(context, e.getLocalizedMessage(), errorString, className, "Unknown", LogEntry.LogLevel.Error);

        if (isInForeground) {
            final Intent intent = new Intent(context, ErrorDialogActivity.class);
            intent.putExtra(ErrorDialogActivity.EXTRA_KEY_APPLICATION_NAME, appName);
            intent.putExtra(ErrorDialogActivity.EXTRA_KEY_DEVELOPER_EMAIL, developerEmail );
            intent.putExtra(ErrorDialogActivity.EXTRA_KEY_ERROR_STRING, errorString);
            context.getApplicationContext().startActivity(intent);

        } else {
            System.exit(1);
        }


    }

    public static String getStackTraceString(Throwable e) {
        String stackTraceString = "";
        for (StackTraceElement element : e.getStackTrace()) {
            stackTraceString += element.toString() + "\n";
        }
        return stackTraceString;
    }

}
