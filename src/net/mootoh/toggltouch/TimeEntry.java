package net.mootoh.toggltouch;

public class TimeEntry {
    private int id;
    private String description;
    private String tagId;

    public TimeEntry(int id, String description) {
        this.id = id;
        this.description = description;
        this.tagId = null;
    }

    public TimeEntry(int id, String description, String tagId) {
        this.id = id;
        this.description = description;
        this.tagId = tagId;
    }

    public String getDescription() {
        return description;
    }

    public int getId() {
        return id;
    }

    public String getTagId() {
        return tagId;
    }

    public void assignTag(String tagId, PersistentStorage pStorage) {
        pStorage.assignTagForTimeEntry(tagId, this);
    }

    public void removeTag(String tagId, PersistentStorage pStorage) {
        pStorage.removeTagFromTimeEntry(tagId, this);

    }
}