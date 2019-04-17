package com.android.mr_paul.blackbot.DBHelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.android.mr_paul.blackbot.Contract.MessageContract.MessageEntry;

public class MessageDBHelper extends SQLiteOpenHelper {

    private final static String DATABASE_NAME = "messages.db";
    private final static int DATABASE_VERSION = 1;

    public MessageDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        final String CREATE_DATABASE = "CREATE TABLE " +
                MessageEntry.TABLE_NAME + " (" +
                MessageEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                MessageEntry.COLUMN_SENDER + " TEXT NOT NULL, " +
                MessageEntry.COLUMN_MESSAGE + " TEXT NOT NULL, " +
                MessageEntry.COLUMN_TIMESTAMP + " TEXT NOT NULL" +
                ");";

        db.execSQL(CREATE_DATABASE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // delete the old table and recreate a new
        db.execSQL("DROP TABLE IF EXISTS " + MessageEntry.TABLE_NAME);
        onCreate(db);
    }
}
