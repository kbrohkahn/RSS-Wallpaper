package com.brohkahn.loggerlibrary;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;

public class ErrorDialogActivity extends AppCompatActivity {
    public static String EXTRA_KEY_APPLICATION_NAME = "applicationName";
    public static String EXTRA_KEY_DEVELOPER_EMAIL = "developerEmail";
    public static String EXTRA_KEY_ERROR_STRING = "errorString";

    private String appName;
    private String developerEmail;
    private String errorString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        appName = intent.getStringExtra(EXTRA_KEY_APPLICATION_NAME);
        developerEmail = intent.getStringExtra(EXTRA_KEY_DEVELOPER_EMAIL);
        errorString = intent.getStringExtra(EXTRA_KEY_ERROR_STRING);

        final Resources resources = getResources();
        String title = resources.getString(R.string.crash_dialog_title);
        String message = String.format(resources.getString(R.string.crash_dialog_message), appName, developerEmail);

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
//        sendCrashEmail(errorString);
    }


    private void sendCrashEmail(String errorString) {
        final Resources resources = getResources();
        String subject = String.format(resources.getString(R.string.crash_email_subject), appName);
        String body = resources.getString(R.string.crash_email_body);

        // save crash report
//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("");
        String filename = String.format("%s Crash %s", appName, Calendar.getInstance().getTime().toString());
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(filename, Context.MODE_WORLD_READABLE);
            outputStream.write(errorString.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            body += "\n\nError saving file:\n" + ErrorHandler.getStackTraceString(e);
        }


        File fileLocation;
        Uri path = null;
        try {
            fileLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), filename);
            path = Uri.fromFile(fileLocation);
        } catch (Exception e) {
            e.printStackTrace();
            body += String.format("\n\nUnable to open error stack trace as attachment\n%s\n\n%s",
                    ErrorHandler.getStackTraceString(e),
                    errorString);
        }

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", developerEmail, null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        if (path != null) {
            emailIntent.putExtra(Intent.EXTRA_STREAM, path);
        }

        startActivity(Intent.createChooser(emailIntent, "Send error email..."));

    }
}
