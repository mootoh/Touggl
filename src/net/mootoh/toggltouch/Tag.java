package net.mootoh.toggltouch;


import java.sql.SQLException;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public final class Tag {
    public String id;
    public String name;
    public String color;
    public String taskId;

    public static final String TABLE_NAME = "tags";
    public static final String COLUMN_NAME_TAG_ID = "tag_id";
    public static final String COLUMN_NAME_NAME   = "name";
    public static final String COLUMN_NAME_COLOR  = "color";
    public static final String COLUMN_NAME_TASK_ID = "task_id";
    private static final String CURRENT_TAG_KEY = "current_tag";

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

    public static void clear(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(Tag.TABLE_NAME, null, null);
        db.close();
    }

    public static Tag getCurrent(Context context) {
        SharedPreferences sp = context.getSharedPreferences(TogglTouch.STORAGE_NAME, 0);
        String current = sp.getString(CURRENT_TAG_KEY, null);
        return current == null ? null : Tag.get(current, context);
    }

    public static void setCurrent(Context context, Tag tag) {
        SharedPreferences sp = context.getSharedPreferences(TogglTouch.STORAGE_NAME, 0);
        SharedPreferences.Editor spe = sp.edit();
        spe.putString(CURRENT_TAG_KEY, tag.id);
        spe.commit();
    }

    public static void resetCurrent(Context context) {
        SharedPreferences sp = context.getSharedPreferences(TogglTouch.STORAGE_NAME, 0);
        SharedPreferences.Editor spe = sp.edit();
        spe.remove(CURRENT_TAG_KEY);
        spe.commit();
    }
}