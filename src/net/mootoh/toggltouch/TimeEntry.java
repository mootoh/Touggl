package net.mootoh.toggltouch;

public class TimeEntry {
    private int id;
    private String description;

    public TimeEntry(int id, String description) {
        this.id = id;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public int getId() {
        return id;
    }
}