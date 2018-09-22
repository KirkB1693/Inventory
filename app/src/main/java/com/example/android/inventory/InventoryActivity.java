package com.example.android.inventory;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.inventory.data.InventoryContract.InventoryEntry;

import java.io.ByteArrayOutputStream;

/**
 * Displays list of products that were entered and stored in the app.
 */
public class InventoryActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = InventoryActivity.class.getSimpleName();

    private static final int INVENTORY_LOADER = 0;

    // This is the Adapter being used to display the list's data
    InventoryCursorAdapter mCursorAdapter;

    // These are the Inventory rows that we will retrieve
    static final String[] PROJECTION = new String[] {InventoryEntry._ID,
            InventoryEntry.COLUMN_PRODUCT_IMAGE,
            InventoryEntry.COLUMN_PRODUCT_NAME,
            InventoryEntry.COLUMN_PRODUCT_PRICE,
            InventoryEntry.COLUMN_PRODUCT_QUANTITY};

    // This is the sort order for the data we will retrieve
    String sortOrder = InventoryEntry._ID + " ASC";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(InventoryActivity.this, EditorActivity.class);
                startActivity(intent);
            }

        });

        // Find the ListView which will be populated with the product data
        ListView productListView = (ListView) findViewById(R.id.list_view_inventory);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        productListView.setEmptyView(emptyView);

        // Create an empty adapter we will use to display the loaded data.
        // We pass null for the cursor, then update it in onLoadFinished()
        mCursorAdapter = new InventoryCursorAdapter(this, null);
        // Attach the adapter to the ListView
        productListView.setAdapter(mCursorAdapter);

        productListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Intent intent = new Intent(InventoryActivity.this, EditorActivity.class);
                Uri currentInventoryUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, id);
                Log.v(LOG_TAG, "Uri sent to editor " + currentInventoryUri);
                intent.setData(currentInventoryUri);
                startActivity(intent);
            }
        });

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(INVENTORY_LOADER, null, this);



    }



    /**
     * Helper method to insert hardcoded product data into the database. For debugging purposes only.
     */
    private void insertInventory() {
        Bitmap productImageBitmap = drawableToBitmap(ResourcesCompat.getDrawable(getResources(),R.drawable.placeholder_image, null));

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        productImageBitmap.compress(Bitmap.CompressFormat.WEBP, 25, stream);
        byte[] productImage = stream.toByteArray();
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_PRODUCT_NAME, "Sample Product");
        values.put(InventoryEntry.COLUMN_PRODUCT_PRICE, "1000");
        values.put(InventoryEntry.COLUMN_PRODUCT_REORDER_METHOD, 1);
        values.put(InventoryEntry.COLUMN_PRODUCT_REORDER_PHONE, "555-555-5555");
        values.put(InventoryEntry.COLUMN_PRODUCT_REORDER_WEBSITE, "www.sampleweb.com");
        values.put(InventoryEntry.COLUMN_PRODUCT_SUPPLIER, "Sample Supplier");
        values.put(InventoryEntry.COLUMN_PRODUCT_QUANTITY, 5);
        values.put(InventoryEntry.COLUMN_PRODUCT_IMAGE, productImage);


        // Insert the new row
        Uri uri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);

        if (uri != null) {
            Toast.makeText(this, getString(R.string.product_saved), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.error_saving_product), Toast.LENGTH_SHORT).show();
        }
    }


    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_inventory, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertInventory();
                return true;
            case R.id.action_sort_default:
                sortOrder = InventoryEntry._ID + " ASC";
                getLoaderManager().restartLoader(INVENTORY_LOADER, null, this);
                return true;
            case R.id.action_sort_a_to_z:
                sortOrder = InventoryEntry.COLUMN_PRODUCT_NAME + " ASC";
                getLoaderManager().restartLoader(INVENTORY_LOADER, null, this);
                return true;
            case R.id.action_sort_z_to_a:
                sortOrder = InventoryEntry.COLUMN_PRODUCT_NAME + " DESC";
                getLoaderManager().restartLoader(INVENTORY_LOADER, null, this);
                return true;
            case R.id.action_sort_price_low_to_high:
                sortOrder = InventoryEntry.COLUMN_PRODUCT_PRICE + " ASC";
                getLoaderManager().restartLoader(INVENTORY_LOADER, null, this);
                return true;
            case R.id.action_sort_price_high_to_low:
                sortOrder = InventoryEntry.COLUMN_PRODUCT_PRICE + " DESC";
                getLoaderManager().restartLoader(INVENTORY_LOADER, null, this);
                return true;
            case R.id.action_sort_quantity_low_to_high:
                sortOrder = InventoryEntry.COLUMN_PRODUCT_QUANTITY + " ASC";
                getLoaderManager().restartLoader(INVENTORY_LOADER, null, this);
                return true;
            case R.id.action_sort_quantity_high_to_low:
                sortOrder = InventoryEntry.COLUMN_PRODUCT_QUANTITY + " DESC";
                getLoaderManager().restartLoader(INVENTORY_LOADER, null, this);
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                showDeleteConfirmationDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }



    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_all_dialog_msg);
        builder.setPositiveButton(R.string.delete_all, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteAllInventoryItems();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Helper method to delete all products in the database.
     */
    private void deleteAllInventoryItems() {
        int rowsDeleted = getContentResolver().delete(InventoryEntry.CONTENT_URI, null, null);
        Log.v("InventoryActivity", rowsDeleted + " rows deleted from inventory database");
    }

    // Called when a new Loader needs to be created
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(this, InventoryEntry.CONTENT_URI,
                PROJECTION, null, null, sortOrder);
    }

    // Called when a previously created loader has finished loading
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mCursorAdapter.swapCursor(data);
    }

    // Called when a previously created loader is reset, making the data unavailable
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mCursorAdapter.swapCursor(null);
    }
}