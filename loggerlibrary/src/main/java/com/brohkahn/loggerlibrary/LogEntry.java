package com.brohkahn.loggerlibrary;

import java.util.Calendar;

public class LogEntry {
    public int id;
    public String message;
    public String stackTrace;
    public String logClass;
    public String function;
    public Calendar time;
    public LogLevel level;

    public enum LogLevel {
        Error,
        Warning,
        Trace,
        Message
    }

    public LogEntry(int id, String message, String stackTrace, String logClass, String logFunction, Calendar time, int levelIndex) {
        this.id = id;
        this.message = message;
        this.stackTrace = stackTrace;
        this.logClass = logClass;
        this.function = logFunction;
        this.time = time;
        this.level = LogLevel.values()[levelIndex];
    }

    public static int getColorId(LogLevel level) {
        int backgroundColorId;
        switch (level) {
            case Error:
                backgroundColorId = R.color.error;
                break;
            case Warning:
                backgroundColorId = R.color.warning;
                break;
            case Trace:
                backgroundColorId = R.color.trace;
                break;
            case Message:
                backgroundColorId = R.color.message;
                break;
            default:
                backgroundColorId = R.color.message;
                break;
        }

        return backgroundColorId;
    }

}
