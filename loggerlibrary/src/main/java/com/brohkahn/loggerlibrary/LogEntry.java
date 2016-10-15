package com.brohkahn.loggerlibrary;

import android.util.Log;

import java.util.Calendar;

public class LogEntry {
    public int id;
    public String message;
    public String logClass;
    public String logFunction;
    public Calendar time;
    public LogLevel level;

    public enum LogLevel {
        Error,
        Warning,
        Trace,
        Message
    }

    public LogEntry(int id, String message, String logClass, String logFunction, Calendar time, int levelIndex) {
        this.id = id;
        this.message = message;
        this.logClass = logClass;
        this.logFunction = logFunction;
        this.time = time;
        this.level = LogLevel.values()[levelIndex];
    }

}
