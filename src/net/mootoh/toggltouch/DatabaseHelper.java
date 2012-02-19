package net.mootoh.toggltouch;

import java.sql.SQLException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class DatabaseHelper extends SQLiteOpenHelper {
    private static final int DB_VERSION  = 1;
    private static final String DB_NAME  = "TimeCard";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String[] DB_CREATE = {
                "CREATE TABLE " + Tag.TABLE_NAME  + " ("
                        + Tag.COLUMN_NAME_TAG_ID  + " TEXT PRIMARY KEY NOT NULL,"
                        + Tag.COLUMN_NAME_NAME    + " TEXT,"
                        + Tag.COLUMN_NAME_COLOR   + " TEXT NOT NULL,"
                        + Tag.COLUMN_NAME_TASK_ID + " TEXT"
                        + ");",
                 "CREATE TABLE " + Task.TABLE_NAME     + " ("
                        + Task.COLUMN_NAME_ID          + " INTEGER PRIMARY KEY,"
                        + Task.COLUMN_NAME_DESCRIPTION + " TEXT NOT NULL,"
                        + Task.COLUMN_NAME_STARTED     + " TEXT NOT NULL"
                        + ");"
        };

        for (String sql : DB_CREATE)
            db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Task.TABLE_NAME 
                + "; DROP TABLE IF EXISTS " + Tag.TABLE_NAME);
        onCreate(db);
    }

    public void reset() throws SQLException {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(Task.TABLE_NAME, null, null);
        db.delete(Tag.TABLE_NAME, null, null);
    }
}