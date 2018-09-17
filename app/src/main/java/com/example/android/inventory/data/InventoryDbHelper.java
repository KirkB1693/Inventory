package com.example.android.inventory.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.android.inventory.data.InventoryContract.InventoryEntry;

public class InventoryDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "Inventory.db";

    public InventoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        String SQL_CREATE_INVENTORY_TABLE = "CREATE TABLE "+ InventoryContract.InventoryEntry.TABLE_NAME + " (" +
                InventoryEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                InventoryEntry.COLUMN_PRODUCT_NAME + " TEXT NOT NULL, " +
                InventoryEntry.COLUMN_PRODUCT_PRICE + " INTEGER NOT NULL, " +
                InventoryEntry.COLUMN_PRODUCT_QUANTITY + " INTEGER NOT NULL DEFAULT 0, " +
                InventoryEntry.COLUMN_PRODUCT_REORDER_METHOD + " INTEGER NOT NULL DEFAULT 0, " +
                InventoryEntry.COLUMN_PRODUCT_SUPPLIER + " TEXT NOT NULL, " +
                InventoryEntry.COLUMN_PRODUCT_REORDER_PHONE + " TEXT, " +
                InventoryEntry.COLUMN_PRODUCT_REORDER_WEBSITE + " TEXT, " +
                InventoryEntry.COLUMN_PRODUCT_IMAGE + " BLOB NOT NULL" +
                ");";

        db.execSQL(SQL_CREATE_INVENTORY_TABLE);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over

    }

}