package net.mootoh.toggltouch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

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

    public String toJsonString() throws JSONException {
        String duration = "0";
        String billable = "false";
        String created_with = "TogglTouch";

        JSONObject data = new JSONObject();
        data.put("duration", duration);
        data.put("billable", billable);
        java.text.DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+0000");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = format.format(new Date());
        data.put("start", date);
        data.put("created_with", created_with);
        data.put("description", description);

        JSONObject json = new JSONObject();
        json.put("time_entry", data);
        return json.toString();
    }
}