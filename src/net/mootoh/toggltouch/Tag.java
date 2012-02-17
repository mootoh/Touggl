package net.mootoh.toggltouch;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public final class Tag {
    public String id;
    public String name;
    public String color;
    public String taskId;
    public Date timeStamp;

    public static final String TABLE_NAME = "tags";
    public static final String COLUMN_NAME_TAG_ID = "tag_id";
    public static final String COLUMN_NAME_NAME   = "name";
    public static final String COLUMN_NAME_COLOR  = "color";
    public static final String COLUMN_NAME_TASK_ID = "task_id";

    public Tag(String id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public Tag(String id, String name, String color, String taskId) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.taskId = taskId;
    }
/*
    public Tag(String id, String name, String color, String touchedAt) throws ParseException {
        this.id = id;
        this.name = name;
        this.color = color;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleTimeZone tz = new SimpleTimeZone(0, "GMT");
        formatter.setTimeZone(tz);
        this.timeStamp = formatter.parse(touchedAt);
    }
*/
    public void save(Context context) throws SQLException {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_TAG_ID, id);
        values.put(COLUMN_NAME_NAME, name);
        values.put(COLUMN_NAME_COLOR, color);
        
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = db.insert(TABLE_NAME, null, values);
        db.close();
        if (rowId <= 0)
            throw new SQLException("Faild to insert row for " + values.getAsString(COLUMN_NAME_TAG_ID));
    }

    static public Tag get(String tagId, Context context) {
        String[] selectionArgs = {tagId};
        return getForSelection(COLUMN_NAME_TAG_ID + " is ?", selectionArgs, context);
    }

    static public Tag getForTaskId(String taskId, Context context) {
        String[] selectionArgs = {taskId};
        return getForSelection(COLUMN_NAME_TASK_ID + " is ?", selectionArgs, context);
    }

    static private Tag getForSelection(String selection, String[] selectionArgs, Context context) {
        Tag gotTag = null;

        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, selection, selectionArgs, null, null, null);
        if (cursor.moveToFirst()) {
            gotTag = new Tag(
                    cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TAG_ID)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_NAME_NAME)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_NAME_COLOR)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TASK_ID))
                    );
        }
        cursor.close();
        db.close();
        return gotTag;
    }

    public void delete(Context context) throws Exception {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deleted = db.delete(TABLE_NAME, COLUMN_NAME_TAG_ID + " is '" + id + "'", null);
        db.close();
        if (deleted != 1)
            throw new Exception("Failed in deleting a tag:" + id);
    }

    public static boolean isBrandNew(String tagId, Context context) {
        return Tag.get(tagId,  context) == null;
    }
/*
    public Tag currentTag(Context context) {
        // TODO: fix touches table
        final String query = "SELECT * FROM touches t1 INNER JOIN tags t2 ON t1.tagId=t2.id ORDER BY t1.touchedAt DESC LIMIT 1";

        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        Tag returnTag = null;

        if (cursor.moveToFirst()) {
            String touchedAt = cursor.getString(cursor.getColumnIndex("touchedAt"));

            try {
                Tag current = new Tag(
                        cursor.getString(cursor.getColumnIndex("tagId")),
                        cursor.getString(cursor.getColumnIndex("name")),
                        cursor.getString(cursor.getColumnIndex("color")),
                        touchedAt);
                returnTag = current;
            } catch (ParseException e) {
                Log.e(getClass().getSimpleName(), "cannot parse the date format:" + touchedAt);
            }
        }
        cursor.close();
        return returnTag;
    }
*/
    public String getTagName(String tagId, Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] columns = { "name" };
        Cursor cursor = db.query("tags", columns, COLUMN_NAME_TAG_ID + " is '" + tagId + "'", null, null, null, null);
        String ret = cursor.moveToFirst() ? cursor.getString(cursor.getColumnIndex("name")) : null;
        cursor.close();
        db.close();
        return ret;
    }

    static public Tag[] getAll(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("tags", null, null, null, null, null, null);

        ArrayList <Tag> tags = new ArrayList<Tag>();
        while (cursor.moveToNext()) {
            Tag tag = new Tag(
                    cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TAG_ID)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_NAME_NAME)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_NAME_COLOR)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TASK_ID))
                    );
            tags.add(tag);
        }

        cursor.close();
        db.close();

        Tag[] ret = new Tag[tags.size()];
        tags.toArray(ret);
        return ret;
    }

    public void assignTask(Task task, Context context) {
        if (hasTask())
            removeTask(context);

        updateTask(task.getId(), context);
    }

    public void removeTask(Context context) {
        updateTask(null, context);
    }

    private void updateTask(String taskId, Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        if (taskId == null)
            values.putNull(COLUMN_NAME_TASK_ID);
        else
            values.put(COLUMN_NAME_TASK_ID, taskId);

        String args[] = {id};
        int ret = db.update(TABLE_NAME, values, COLUMN_NAME_TAG_ID + " is ?", args);
        db.close();
        if (ret != 1) {
            // TODO: raise an exception or do something error handling
            Log.e(getClass().getSimpleName(), "could not update the table for tagId:" + id + ", taskId:" + taskId);
        }

        this.taskId = taskId;
    }

    private boolean hasTask() {
        return taskId != null;
    }

    public void removeTagFromTimeEntry(String tagId, Task timeEntry) {
        // TODO Auto-generated method stub
    }

    public static void clear(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(Tag.TABLE_NAME, null, null);
        db.close();
    }
}