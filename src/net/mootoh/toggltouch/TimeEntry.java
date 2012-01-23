package net.mootoh.toggltouch;

import java.util.Date;

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
//        Date startDate = new Date();
//        json.put("startDate", startDate.toString());
        data.put("start", "2012-01-21T16:19:45+02:00");
        data.put("created_with", created_with);
        data.put("description", description);

        JSONObject json = new JSONObject();
        json.put("time_entry", data);
        return json.toString();
    }
}