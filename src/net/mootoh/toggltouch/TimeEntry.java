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
    private String started;

    private static final String DURATION = "0";
    private static final String BILLABLE = "false";
    private static final String CREATED_WITH = "TogglTouch";

    public TimeEntry(int id, String description) {
        this.id = id;
        this.description = description;
        this.tagId = null;
        this.started = null;
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

    public void setId(int id) {
        this.id = id;
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
        JSONObject data = new JSONObject();
        data.put("duration", DURATION);
        data.put("billable", BILLABLE);
        data.put("created_with", CREATED_WITH);

        started = dateNowAsISO8601();
        data.put("start", started);

        data.put("description", description);

        JSONObject json = new JSONObject();
        json.put("time_entry", data);
        return json.toString();
    }

    private String dateNowAsISO8601() {
        java.text.DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+0000");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = format.format(new Date());
        return date;
    }

    public String toStopJsonString() throws JSONException {
        JSONObject data = new JSONObject();
        data.put("stop", dateNowAsISO8601());
        assert(started != null);
        data.put("start", started);

        JSONObject json = new JSONObject();
        json.put("time_entry", data);
        return json.toString();
    }
}