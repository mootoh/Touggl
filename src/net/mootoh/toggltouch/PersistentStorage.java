package net.mootoh.toggltouch;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public final class PersistentStorage {
    DatabaseHelper databaseHelper;

    public PersistentStorage(Context context) {
        databaseHelper = new DatabaseHelper(context);
    }

    public void addTag(String tagId, String name, String color) throws SQLException {
        ContentValues values = new ContentValues();
        values.put("id", tagId);
        values.put("name", name);
        values.put("color", color);

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        long rowId = db.insert("tags", null, values);
        if (rowId <= 0)
            throw new SQLException("Faild to insert row for " + tagId);
    }

    public void deleteTag(String tagId) throws Exception {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        int deleted = db.delete("tags", "id is '" + tagId + "'", null);
        if (deleted != 1)
            throw new Exception("Failed in deleting a tag:" + tagId);
    }

    public boolean isBrandNewTag(String tagId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.query("tags", null, "id is '" + tagId + "'", null, null, null, null);
        boolean ret = cursor.getCount() == 0;
        cursor.close();
        return ret;
    }

    public Tag currentTag() {
        final String query = "SELECT * FROM touches t1 INNER JOIN tags t2 ON t1.tagId=t2.id ORDER BY t1.touchedAt DESC LIMIT 1";

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
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

    public String getTagName(String tagId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String[] columns = { "name" };
        Cursor cursor = db.query("tags", columns, "id is '" + tagId + "'", null, null, null, null);
        String ret = cursor.moveToFirst() ? cursor.getString(cursor.getColumnIndex("name")) : null;
        cursor.close();
        return ret;
    }

    public void reset() throws SQLException {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete("tags", null, null);
        db.delete("touches", null, null);
        db.delete("timeEntries", null, null);
    }

    public Tag[] getTags() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.query("tags", null, null, null, null, null, null);

        ArrayList <Tag> tags = new ArrayList<Tag>();
        while (cursor.moveToNext()) {
            Tag tag = new Tag(cursor.getString(cursor.getColumnIndex("id")),
                    cursor.getString(cursor.getColumnIndex("name")),
                    cursor.getString(cursor.getColumnIndex("color")));
            tags.add(tag);
        }
        cursor.close();

        Tag[] ret = new Tag[tags.size()];
        tags.toArray(ret);
        return ret;
    }

    public void addTimeEntry(JSONObject json) throws SQLException, JSONException {
        String description;
        description = json.getString("description");

        ContentValues values = new ContentValues();
        values.put("description", description);

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        long rowId = db.insert("timeEntries", null, values);
        if (rowId <= 0)
            throw new SQLException("Faild to insert row for description:" + description);
    }

    public void addTimeEntry(String description) throws SQLException {
        ContentValues values = new ContentValues();
        values.put("description", description);

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        long rowId = db.insert("timeEntries", null, values);
        if (rowId <= 0)
            throw new SQLException("Faild to insert row for description:" + description);
    }

    public void addTimeEntry(TimeEntry entry) throws SQLException {
        ContentValues values = new ContentValues();
        values.put("description", entry.getDescription());

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        long rowId = db.insert("timeEntries", null, values);
        if (rowId <= 0)
            throw new SQLException("Faild to insert row for description:" + entry.getDescription());
    }

    public List <TimeEntry> getTimeEntries() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.query("timeEntries", null, null, null, null, null, null);

        ArrayList <TimeEntry> entries = new ArrayList<TimeEntry>();
        while (cursor.moveToNext()) {
            TimeEntry entry = new TimeEntry(cursor.getInt(cursor.getColumnIndex("id")),
                    cursor.getString(cursor.getColumnIndex("description")));
            entries.add(entry);
        }
        cursor.close();
        return entries;
    }

    private TimeEntry getTimeEntryForTagId(String tagId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String[] args = {tagId};
        Cursor cursor = db.query("timeEntries", null, "tagId=?", args, null, null, null);

        TimeEntry entry = null;
        while (cursor.moveToNext()) {
            entry = new TimeEntry(cursor.getInt(cursor.getColumnIndex("id")), cursor.getString(cursor.getColumnIndex("description")), cursor.getString(cursor.getColumnIndex("tagId")));
            break;
        }
        cursor.close();

        return entry;
    }

    public void assignTagForTimeEntry(String tagId, TimeEntry timeEntry) {
        TimeEntry existance = getTimeEntryForTagId(tagId);
        if (existance != null) {
            removeTagFromTimeEntry(tagId, existance);
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("tagId", tagId);

        String args[] = {"" + timeEntry.getId()};
        db.update("timeEntries", values, "id=?", args);
    }

    public void removeTagFromTimeEntry(String tagId, TimeEntry timeEntry) {
        // TODO Auto-generated method stub
    }

    public TimeEntry currentTimeEntry() {
        // TODO Auto-generated method stub
        return null;
    }

    public void stopCurrentTimeEntry() {
        // TODO Auto-generated method stub
        
    }

    public void startTimeEntry(TimeEntry timeEntry) {
        // TODO Auto-generated method stub
    }
}

final class DatabaseHelper extends SQLiteOpenHelper {
    private static final int DB_VERSION  = 1;
    private static final String DB_NAME  = "TimeCard";
    private static final String[] DB_CREATE = {
        "create TABLE tags ("
                + "id TEXT not null primary key, "
                + "name TEXT not null, "
                + "color TEXT"
                + ");",
        "create TABLE timeEntries ("
                + "description TEXT not null primary key, "
                + "tagId TEXT"
                + ");",

        "insert into tags (id, name, color) values ('VOID_TAG_ID', 'VOID', '999999');",
        "insert into touches (tagId) values ('VOID_TAG_ID');"
    };

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            for (String sql : DB_CREATE)
                db.execSQL(sql);
        } catch (android.database.SQLException ex) {
            Log.e(getClass().getSimpleName(), "exception in creating db: " + ex);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS tags; DROP TABLE IF EXISTS touches");
        onCreate(db);
    }
}