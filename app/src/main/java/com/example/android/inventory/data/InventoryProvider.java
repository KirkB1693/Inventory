package com.example.android.inventory.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.android.inventory.data.InventoryContract.InventoryEntry;

public class InventoryProvider extends ContentProvider {
    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = InventoryProvider.class.getSimpleName();

    private InventoryDbHelper mDbHelper;

    /**
     * URI matcher code for the content URI for the inventory table
     */
    private static final int INVENTORY = 100;

    /**
     * URI matcher code for the content URI for a single product in the inventory table
     */
    private static final int INVENTORY_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.

        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY, INVENTORY);
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY + "/#", INVENTORY_ID);
    }


    /**
     * Initialize the provider and the database helper object.
     */
    @Override
    public boolean onCreate() {
        mDbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    /**
     * Perform the query for the given URI. Use the given projection, selection, selection arguments, and sort order.
     */
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                // For the INVENTORY code, query the inventory table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the inventory table.
                cursor = database.query(InventoryEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case INVENTORY_ID:
                // For the INVENTORY_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.example.android.inventory/inventory/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                // This will perform a query on the inventory table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(InventoryEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the cursor
        // so we know what Content Uri was created for.
        // If the data at this Uri changes then we know we need to update the cursor
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    /**
     * Insert new data into the provider with the given ContentValues.
     */
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return insertProduct(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert an Product into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertProduct(Uri uri, ContentValues values) {

        // Check that the name is not null
        String name = values.getAsString(InventoryEntry.COLUMN_PRODUCT_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Inventory requires a name");
        }

        // Check that the supplier is not null if present in contentValues
        String supplier = values.getAsString(InventoryEntry.COLUMN_PRODUCT_SUPPLIER);
        if (supplier == null) {
            throw new IllegalArgumentException("Inventory requires a supplier");
        }

        // Check that the quantity is positive and not null
        Integer quantity = values.getAsInteger(InventoryEntry.COLUMN_PRODUCT_QUANTITY);
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Inventory requieres a valid weight");
        }

        // Check that the price is positive and not null
        Integer price = values.getAsInteger(InventoryEntry.COLUMN_PRODUCT_PRICE);
        if (price == null || price < 0) {
            throw new IllegalArgumentException("Inventory requieres a valid price");
        }

        // Check that the reorder method is not null and is Phone, Website or Unknown
        Integer reorder = values.getAsInteger(InventoryEntry.COLUMN_PRODUCT_REORDER_METHOD);
        if (reorder == null || !InventoryEntry.isValidReorder(reorder)) {
            throw new IllegalArgumentException("Inventory requires a valid reorder method");
        }

        // Check that the phone or website are not null if selected as reorder method
        String phone = values.getAsString(InventoryEntry.COLUMN_PRODUCT_REORDER_PHONE);
        String website = values.getAsString(InventoryEntry.COLUMN_PRODUCT_REORDER_WEBSITE);
        switch (reorder) {
            case 0:
                break;
            case 1:
                if (phone == null) {
                    throw new IllegalArgumentException("Inventory requires a valid reorder phone number");
                }
                break;
            case 2:
                if (website == null) {
                    throw new IllegalArgumentException("Inventory requires a valid reorder website");
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid reorder method");
        }


        // Check that the image is not null
        byte[] image = values.getAsByteArray(InventoryEntry.COLUMN_PRODUCT_IMAGE);
        if (image == null) {
           throw new IllegalArgumentException("Inventory requires an image");
        }


        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Insert the data to the database and get the _id number of the new row
        long id = database.insert(InventoryEntry.TABLE_NAME, null, values);

        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1)

        {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        getContext().getContentResolver().notifyChange(uri, null);

        // Once we know the ID of the new row in the table,
        // return the new URI with the ID appended to the end of it
        return ContentUris.withAppendedId(uri, id);
    }

    /**
     * Updates the data at the given selection and selection arguments, with the new ContentValues.
     */
    @Override
    public int update(@NonNull Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return updateInventory(uri, contentValues, selection, selectionArgs);
            case INVENTORY_ID:
                // For the INVENTORY_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateInventory(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }


    public int updateInventory(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {

        // If there are no values to update, then don't try to update the database
        if (contentValues.size() == 0) {
            return 0;
        }

        // Check that the name is not null if present in contentValues
        if (contentValues.containsKey(InventoryEntry.COLUMN_PRODUCT_NAME)) {
            String name = contentValues.getAsString(InventoryEntry.COLUMN_PRODUCT_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Inventory requires a name");
            }
        }

        // Check that the supplier is not null if present in contentValues
        if (contentValues.containsKey(InventoryEntry.COLUMN_PRODUCT_SUPPLIER)) {
            String supplier = contentValues.getAsString(InventoryEntry.COLUMN_PRODUCT_SUPPLIER);
            if (supplier == null) {
                throw new IllegalArgumentException("Inventory requires a supplier");
            }
        }

        // Check that the preferred product reorder method is not null and is Phone, Website or Unknown if present in contentValues, if valid then check appropriate phone or website to make sure they are not null
        if (contentValues.containsKey(InventoryEntry.COLUMN_PRODUCT_REORDER_METHOD)) {
            Integer reorder = contentValues.getAsInteger(InventoryEntry.COLUMN_PRODUCT_REORDER_METHOD);
            if (reorder == null || !InventoryEntry.isValidReorder(reorder)) {
                throw new IllegalArgumentException("Inventory requires a valid reorder method");
            }
            switch (reorder) {
                case 0:
                    break;
                case 1:
                    String phone = null;
                    if (contentValues.containsKey(InventoryEntry.COLUMN_PRODUCT_REORDER_PHONE)) {
                        phone = contentValues.getAsString(InventoryEntry.COLUMN_PRODUCT_REORDER_PHONE);
                    }
                    if (phone == null) {
                        throw new IllegalArgumentException("Inventory requires a valid reorder phone number");
                    }
                    break;
                case 2:
                    String website = null;
                    if (contentValues.containsKey(InventoryEntry.COLUMN_PRODUCT_REORDER_WEBSITE)) {
                        website = contentValues.getAsString(InventoryEntry.COLUMN_PRODUCT_REORDER_WEBSITE);
                    }
                    if (website == null) {
                        throw new IllegalArgumentException("Inventory requires a valid reorder website");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Invalid reorder method");
            }

        }



        // Check that the quantity is positive if present in contentValues
        if (contentValues.containsKey(InventoryEntry.COLUMN_PRODUCT_QUANTITY)) {
            Integer quantity = contentValues.getAsInteger(InventoryEntry.COLUMN_PRODUCT_QUANTITY);
            if (quantity != null && quantity < 0) {
                throw new IllegalArgumentException("Inventory requieres a valid quantity");
            }
        }

        // Check that the price is positive if present in contentValues
        if (contentValues.containsKey(InventoryEntry.COLUMN_PRODUCT_PRICE)) {
            Integer price = contentValues.getAsInteger(InventoryEntry.COLUMN_PRODUCT_PRICE);
            if (price != null && price < 0) {
                throw new IllegalArgumentException("Inventory requieres a valid price");
            }
        }


        // Check that the image is not null if present in contentValues
        if (contentValues.containsKey(InventoryEntry.COLUMN_PRODUCT_IMAGE)) {
            byte image[] = contentValues.getAsByteArray(InventoryEntry.COLUMN_PRODUCT_IMAGE);
            if (image == null) {
                throw new IllegalArgumentException("Inventory requires a name");
            }
        }


        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Insert the data to the database and get the number of rows updated
        int rowsUpdated = database.update(InventoryEntry.TABLE_NAME, contentValues, selection, selectionArgs);

        // If the rowsUpdated is 0, then the update failed. Log an error and return 0.
        if (rowsUpdated == 0)

        {
            Log.e(LOG_TAG, "Failed to update row(s) for " + uri);
        }

        if (rowsUpdated != 0) {
            if (getContext() != null) getContext().getContentResolver().notifyChange(uri, null);
        }
        // Once we know the ID of the new row in the table,
        // return the new URI with the ID appended to the end of it
        return rowsUpdated;
    }

    /**
     * Delete the data at the given selection and selection arguments.
     */
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        int rowsDeleted;
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(InventoryEntry.TABLE_NAME, selection, selectionArgs);
                if (rowsDeleted != 0) {
                    if (getContext() != null) getContext().getContentResolver().notifyChange(uri, null);
                }
                return rowsDeleted;
            case INVENTORY_ID:
                // Delete a single row given by the ID in the URI
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(InventoryEntry.TABLE_NAME, selection, selectionArgs);
                if (rowsDeleted != 0) {
                    if (getContext() != null) getContext().getContentResolver().notifyChange(uri, null);
                }
                return rowsDeleted;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
    }

    /**
     * Returns the MIME type of data for the content URI.
     */
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return InventoryEntry.CONTENT_LIST_TYPE;
            case INVENTORY_ID:
                return InventoryEntry.CONTENT_PRODUCT_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }
}

