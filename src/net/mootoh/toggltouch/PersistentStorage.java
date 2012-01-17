package net.mootoh.toggltouch;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.SimpleTimeZone;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public final class PersistentStorage {
    DatabaseHelper databaseHelper;
    final static public String VOID_TAG_ID   = "VOID";
    final static public String VOID_TAG_NAME = "VOID";

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

        addVoidTag(db);
        addVoidTouch(db);
}

    private void addVoidTag(SQLiteDatabase db) throws SQLException {
        // void tag is permanent
        ContentValues values = new ContentValues();
        values.put("id", VOID_TAG_ID);
        values.put("name", VOID_TAG_NAME);
        values.put("color", "#999999");

        long rowId = db.insert("tags", null, values);
        if (rowId <= 0)
            throw new SQLException("Faild to insert void tag");
    }

    private void addVoidTouch(SQLiteDatabase db) throws SQLException {
        // void touch is permanent
        ContentValues values = new ContentValues();
        values.put("tagId", VOID_TAG_ID);

        long rowId = db.insert("touches", null, values);
        if (rowId <= 0)
            throw new SQLException("Faild to insert void touch");
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

    private void addTouch(String tagId) throws SQLException {
        ContentValues values = new ContentValues();
        values.put("tagId", tagId);

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        long rowId = db.insert("touches", null, values);
        if (rowId <= 0)
            throw new SQLException("Faild to insert row for " + tagId);
    }

    public void startTag(String tagId) throws SQLException {
        addTouch(tagId);
    }

    public void stopCurrentTag() throws SQLException {
        addTouch(VOID_TAG_ID);
    }

    public ArrayList <String[]> getHistoryAll() throws Exception {
        final String query = "SELECT * FROM touches t1 INNER JOIN tags t2 ON t1.tagId=t2.id ORDER BY t1.touchedAt ASC";

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        ArrayList <String[]> history = new ArrayList<String[]>();

        cursor.moveToFirst();
        String curTagId = cursor.getString(cursor.getColumnIndex("tagId"));
        String curName  = cursor.getString(cursor.getColumnIndex("name"));
        Date   curDate  = parseDate(cursor.getString(cursor.getColumnIndex("touchedAt")));
        String curColor = cursor.getString(cursor.getColumnIndex("color"));

        while (cursor.moveToNext()) {
            String tagId = cursor.getString(cursor.getColumnIndex("tagId"));
            if (curTagId == tagId)
                throw new Exception("Logic Error: a tagId should not be consequential");

            String name = cursor.getString(cursor.getColumnIndex("name"));
            Date date   = parseDate(cursor.getString(cursor.getColumnIndex("touchedAt")));
            String elapsed = getElapsedTime(curDate, date);
            String color= cursor.getString(cursor.getColumnIndex("color"));
            if (color.charAt(0) != '#')
                color = '#' + color;

            if (! curTagId.equals(VOID_TAG_ID)) {
                String[] pair = {curName, elapsed, curColor};
                history.add(pair);
            }

            curTagId = tagId;
            curName  = name;
            curDate  = date;
            curColor = color;
        }

        cursor.close();
        Collections.reverse(history);

        return history;
    }

    private Date parseDate(String dateString) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleTimeZone tz = new SimpleTimeZone(0, "GMT");
        formatter.setTimeZone(tz);
        return formatter.parse(dateString);
    }

    private String getElapsedTime(java.util.Date from, java.util.Date to) {
        long toTime = to.getTime();
        long fromTime  = from.getTime();
        long elapsed = (toTime - fromTime);

        long elapsedHour = elapsed / (60*60*1000);
        elapsed -= elapsedHour * (60*60*1000);

        long elapsedMin = elapsed / (60*1000);
        elapsed -= elapsedMin * 60*1000;
        long elapsedSec = elapsed / 1000;

        return String.format("%02d:%02d:%02d", elapsedHour, elapsedMin, elapsedSec);
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
        "create TABLE touches ("
                + "tagId text not null, "
                + "touchedAt DATETIME DEFAULT CURRENT_TIMESTAMP PRIMARY KEY"
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