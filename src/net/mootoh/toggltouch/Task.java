package net.mootoh.toggltouch;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Task {
    private int id;
    private String description;
    private Date started;

    private static final String DURATION = "0";
    private static final String BILLABLE = "false";
    private static final String CREATED_WITH = "TogglTouch";

    public static final String TABLE_NAME = "tasks";
    public static final String COLUMN_NAME_DESCRIPTION = "description";
    public static final String COLUMN_NAME_ID = "id_";

    public Task(int id, String description) {
        this.id = id;
        this.description = description;
        this.started = null;
    }

    public String getDescription() {
        return description;
    }

    public String getId() {
        return id + "";
    }

    public void setId(int id) {
        this.id = id;
    }

    public String toJsonString() throws JSONException {
        JSONObject data = new JSONObject();
        data.put("duration", DURATION);
        data.put("billable", BILLABLE);
        data.put("created_with", CREATED_WITH);

        started = new Date();
        data.put("start", dateAsISO8601(started));
        data.put("stop", dateAsISO8601(started));
        data.put("description", description);
        data.put("duration", -1);

        JSONObject json = new JSONObject();
        json.put("time_entry", data);
        return json.toString();
    }

    private String dateAsISO8601(Date date) {
        java.text.DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+0000");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(date);
    }

    public String toStopJsonString() throws JSONException {
        JSONObject data = new JSONObject();
        assert(started != null);
        data.put("start", dateAsISO8601(started));
        Date stopped = new Date();
        data.put("stop", dateAsISO8601(stopped));
        data.put("duration", (stopped.getTime() - started.getTime()) / 1000);
        data.put("description", description);

        JSONObject json = new JSONObject();
        json.put("time_entry", data);
        return json.toString();
    }

    // ----------------------------------------------------------------------------------
    // DB
    //
    public void save(JSONObject json, Context context) throws SQLException, JSONException {
        String description;
        description = json.getString("description");

        ContentValues values = new ContentValues();
        values.put("description", description);

        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = db.insert("timeEntries", null, values);
        db.close();
        if (rowId <= 0)
            throw new SQLException("Faild to insert row for description:" + description);
    }

    public void save(Context context) throws SQLException {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_ID, id);
        values.put(COLUMN_NAME_DESCRIPTION, description);

        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = db.insert(TABLE_NAME, null, values);
        db.close();
        if (rowId <= 0)
            throw new SQLException("Faild to insert row for description:" + description);
    }

    public void addTimeEntry(Task entry, Context context) throws SQLException {
        ContentValues values = new ContentValues();
        values.put("description", entry.getDescription());

        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = db.insert("timeEntries", null, values);
        db.close();
        if (rowId <= 0)
            throw new SQLException("Faild to insert row for description:" + entry.getDescription());
    }

    public List <Task> getTimeEntries(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("timeEntries", null, null, null, null, null, null);

        ArrayList <Task> entries = new ArrayList<Task>();
        while (cursor.moveToNext()) {
            Task entry = new Task(cursor.getInt(cursor.getColumnIndex("id")),
                    cursor.getString(cursor.getColumnIndex("description")));
            entries.add(entry);
        }
        cursor.close();
        db.close();
        return entries;
    }

    public Task currentTimeEntry() {
        // TODO Auto-generated method stub
        return null;
    }

    public void stopCurrentTimeEntry() {
        // TODO Auto-generated method stub

    }

    public void startTimeEntry(Task timeEntry) {
        // TODO Auto-generated method stub
    }

    public static Task[] getAll(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(Task.TABLE_NAME, null, null, null, null, null, null);

        ArrayList <Task> tasks = new ArrayList<Task>();
        while (cursor.moveToNext()) {
            Task task = new Task(cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ID)),
                                 cursor.getString(cursor.getColumnIndex(COLUMN_NAME_DESCRIPTION)));
            tasks.add(task);
        }
        cursor.close();
        db.close();

        Task[] ret = new Task[tasks.size()];
        tasks.toArray(ret);
        return ret;
    }
    
    public static void sync(final Context context, final TaskSyncDelegate delegate) {
        TogglApi api = new TogglApi(context);
        api.getTimeEntries(new ApiResponseDelegate<Task[]>() {
            public void onSucceeded(Task[] result) {
                // TODO: re-link relation between Tag-Task
                for (Task task: result) {
                    try {
                        task.save(context);
                    } catch (SQLException e) {
                        delegate.onFailed(e);
                        return;
                    }
                }
                delegate.onSucceeded(result);
            }
            
            public void onFailed(Exception e) {
                delegate.onFailed(e);
            }
        });
    }

    public static void clear(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(Task.TABLE_NAME, null, null);
        db.close();
    }

    public String toString() {
        return description;
    }
}