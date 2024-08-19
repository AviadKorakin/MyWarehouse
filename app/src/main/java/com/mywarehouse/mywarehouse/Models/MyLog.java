package com.mywarehouse.mywarehouse.Models;

import com.mywarehouse.mywarehouse.Enums.LogType;

import java.util.Date;

import java.util.Date;
import java.util.Objects;

public class MyLog {
    private String id;
    private String title;
    private Date date;
    private String notes;
    private String invokedBy;
    private boolean isCollapsed;
    private LogType type;

    // Constructors, getters, and setters

    public MyLog() {
        // Default constructor required for calls to DataSnapshot.getValue(Log.class)
    }

    public MyLog(String id, String title, Date date, String notes, String invokedBy, LogType type) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.notes = notes;
        this.invokedBy = invokedBy;
        this.isCollapsed = true;  // Default to collapsed
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getInvokedBy() {
        return invokedBy;
    }

    public void setInvokedBy(String invokedBy) {
        this.invokedBy = invokedBy;
    }

    public boolean isCollapsed() {
        return isCollapsed;
    }

    public void setCollapsed(boolean collapsed) {
        isCollapsed = collapsed;
    }

    public LogType getType() {
        return type;
    }

    public void setType(LogType type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MyLog myLog = (MyLog) o;
        return isCollapsed == myLog.isCollapsed &&
                Objects.equals(id, myLog.id) &&
                Objects.equals(title, myLog.title) &&
                Objects.equals(date, myLog.date) &&
                Objects.equals(notes, myLog.notes) &&
                Objects.equals(invokedBy, myLog.invokedBy) &&
                type == myLog.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, date, notes, invokedBy, isCollapsed, type);
    }
}
