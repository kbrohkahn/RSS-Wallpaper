package com.brohkahn.loggerlibrary;

import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;

public class LoggerApplication extends Application {
    protected String APPLICATION_NAME = "UNKNOWN";
    protected String DEVELOPER_EMAIL = "kevin@broh-kahn.com";

    @Override
    public void onCreate() {

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                handleUncaughtException(e);
            }
        });

        super.onCreate();
    }

    public void handleUncaughtException(Throwable e) {
        final String errorString = getStackTraceString(e);
        e.printStackTrace();

        LogDBHelper.saveLogEntry(this, e.getLocalizedMessage(), errorString, "N/A", "N/A", LogEntry.LogLevel.Error);
        System.exit(1);

        final Resources resources = getResources();
        String title = resources.getString(R.string.crash_dialog_title);
        String message = String.format(resources.getString(R.string.crash_dialog_message), APPLICATION_NAME, DEVELOPER_EMAIL);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.crash_dialog_positive_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        sendCrashEmail(errorString);
                        System.exit(1);
                    }
                }).setNegativeButton(R.string.crash_dialog_negative_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        System.exit(1);
                    }
                });
        builder.create().show();

    }

    private String getStackTraceString(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    public void sendCrashEmail(String errorString) {
        final Resources resources = getResources();
        String subject = String.format(resources.getString(R.string.crash_email_subject), APPLICATION_NAME);
        String body = resources.getString(R.string.crash_email_body);

        // save crash report
        String filename = String.format("%s Crash %s", APPLICATION_NAME, Calendar.getInstance().toString());
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(filename, Context.MODE_WORLD_READABLE);
            outputStream.write(errorString.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            body += "\n\nError saving file:\n" + getStackTraceString(e);
        }


        File fileLocation;
        Uri path = null;
        try {
            fileLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), filename);
            path = Uri.fromFile(fileLocation);
        } catch (Exception e) {
            e.printStackTrace();
            body += String.format("\n\nUnable to open error stack trace as attachment\n%s\n\n%s",
                    getStackTraceString(e),
                    errorString);
        }

        Intent emailIntent = new Intent(Intent.ACTION_SEND, Uri.fromParts(
                "mailto", DEVELOPER_EMAIL, null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        if (path != null) {
            emailIntent.putExtra(Intent.EXTRA_STREAM, path);
        }

        startActivity(Intent.createChooser(emailIntent, "Send email..."));

    }
}
