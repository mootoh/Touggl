package net.mootoh.touggl;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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
    private Date startedAt;

    private static java.text.DateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private static java.text.DateFormat formatter2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    static {
        formatter1.setTimeZone(TimeZone.getTimeZone("UTC"));
        formatter2.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final String CREATED_WITH = "Touggl";

    public static final String TABLE_NAME = "tasks";
    public static final String COLUMN_NAME_DESCRIPTION = "description";
    public static final String COLUMN_NAME_ID = "id_";
    public static final String COLUMN_NAME_STARTED = "started";

    public Task(int id, String description, Date started) {
        this.id = id;
        this.description = description;
        this.startedAt = started;
    }

    public Task(int id, String description, String started) {
        this.id = id;
        this.description = description;
        try {
            this.startedAt = formatter1.parse(started);
        } catch (ParseException e1) {
            try {
                this.startedAt = formatter2.parse(started);
            } catch (ParseException e2) {
                e2.printStackTrace();
            }
        }
    }

    @Override
    public String toString() {
        return "Task #" + id + ": " + description + "started: " + startedAt.toString();
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

    public void setStartedAt(Date date) {
        this.startedAt = date;
    }

    public void updateStartedAtToNow() {
        startedAt = new Date();
    }

    public String toStartJsonString() throws JSONException {
        JSONObject data = new JSONObject();
        Calendar calendar = new GregorianCalendar();
        long duration = -calendar.getTimeInMillis() / 1000;
        data.put("duration", duration);
        data.put("created_with", CREATED_WITH);

        data.put("start", "null");
        data.put("stop", "null");
        data.put("description", description);

        JSONObject json = new JSONObject();
        json.put("time_entry", data);
        return json.toString();
    }

    private String dateAsISO8601(Date date) {
        return formatter1.format(date);
    }

    public String toStopJsonString() throws JSONException {
        JSONObject data = new JSONObject();
        assert(startedAt != null);
        data.put("start", dateAsISO8601(startedAt));
        Date stopped = new Date();
        data.put("stop", dateAsISO8601(stopped));
        data.put("duration", (stopped.getTime() - startedAt.getTime()) / 1000);
        data.put("description", description);

        JSONObject json = new JSONObject();
        json.put("time_entry", data);
        return json.toString();
    }

    // ----------------------------------------------------------------------------------
    // DB
    //
    public void save(Context context) throws SQLException {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_ID, id);
        values.put(COLUMN_NAME_DESCRIPTION, description);
        values.put(COLUMN_NAME_STARTED, formatter1.format(startedAt));

        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = db.insert(TABLE_NAME, null, values);
        db.close();
        if (rowId <= 0)
            throw new SQLException("Faild to insert row for description:" + description);
    }

    public void update(Context context) throws SQLException {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_ID, id);
        values.put(COLUMN_NAME_STARTED, formatter1.format(startedAt));

        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String args[] = {description};
        int ret = db.update(TABLE_NAME, values, COLUMN_NAME_DESCRIPTION + " is ?", args);
        db.close();
        if (ret != 1)
            throw new SQLException("Faild to update the row for description:" + description);
    }

    public static Task getTask(String taskId, Context context) {
        String[] selectionArgs = {taskId};

        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, COLUMN_NAME_ID + " is ?", selectionArgs, null, null, null);
        Task task = null;
        if (cursor.moveToFirst()) {
            String dateString = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_STARTED));
            try {
                Date started = formatter1.parse(dateString);
                task = new Task(
                        cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ID)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_NAME_DESCRIPTION)),
                        started
                        );
            } catch (ParseException e1) {
                try {
                    Date started = formatter2.parse(dateString);
                    task = new Task(
                            cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ID)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_NAME_DESCRIPTION)),
                            started
                            );
                } catch (ParseException e2) {
                    e2.printStackTrace();
                }
            }
        }
        cursor.close();
        db.close();
        return task;
    }

    public static Task getTaskForDescription(String description, Context context) {
        String[] selectionArgs = {description};

        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, COLUMN_NAME_DESCRIPTION + " is ?", selectionArgs, null, null, null);
        Task task = null;
        if (cursor.moveToFirst()) {
            String dateString = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_STARTED));
            try {
                Date started = formatter1.parse(dateString);
                task = new Task(
                        cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ID)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_NAME_DESCRIPTION)),
                        started
                        );
            } catch (ParseException e1) {
                try {
                    Date started = formatter2.parse(dateString);
                    task = new Task(
                            cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ID)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_NAME_DESCRIPTION)),
                            started
                            );
                } catch (ParseException e2) {
                    e2.printStackTrace();
                }
            }
        }
        cursor.close();
        db.close();
        return task;
    }

    public static Task[] getAll(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(Task.TABLE_NAME, null, null, null, null, null, null);

        ArrayList <Task> tasks = new ArrayList<Task>();
        while (cursor.moveToNext()) {
            String dateString = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_STARTED));
            try {
                Date started = formatter1.parse(dateString);
                Task task = new Task(cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ID)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_NAME_DESCRIPTION)),
                        started);
                tasks.add(task);
            } catch (ParseException e) {
                try {
                    Date started = formatter2.parse(dateString);
                    Task task = new Task(cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ID)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_NAME_DESCRIPTION)),
                            started);
                    tasks.add(task);
                } catch (ParseException e2) {
                    e2.printStackTrace();
                }
            }
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
                for (Task task: result) {
                    Task existingTask = getTaskForDescription(task.description, context);
                    if (existingTask == null) {
                        try {
                            task.save(context);
                        } catch (SQLException e) {
                            delegate.onFailed(e);
                            return;
                        }
                    } else {
                        if (existingTask.getId().equals(task.getId()))
                            continue;

                        // re-link relation between Tag-Task
                        Tag tag = Tag.getForTaskId(existingTask.getId(), context);

                        existingTask.setId(task.id);
                        existingTask.setStartedAt(task.getStartedAt());
                        try {
                            existingTask.update(context);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        if (tag != null)
                            tag.assignTask(existingTask, context);
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

    public Date getStartedAt() {
        return startedAt;
    }
}