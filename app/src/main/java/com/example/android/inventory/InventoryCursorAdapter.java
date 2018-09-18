package com.example.android.inventory;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventory.data.InventoryContract.InventoryEntry;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * {@link InventoryCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of product data as its data source. This adapter knows
 * how to create list items for each row of product data in the {@link Cursor}.
 */
public class InventoryCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link InventoryCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public InventoryCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the product data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current product can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        // Find fields to populate in inflated template
        ImageView productImageView = (ImageView) view.findViewById(R.id.list_product_image);
        TextView nameTextView = (TextView) view.findViewById(R.id.list_product_name);
        TextView priceTextView = (TextView) view.findViewById(R.id.list_product_price);
        final TextView quantityTextView = (TextView) view.findViewById(R.id.list_product_quantity);
        Button saleButton = (Button) view.findViewById(R.id.list_sale_button);

        // Extract properties from cursor
        byte[] byteArray = cursor.getBlob(cursor.getColumnIndexOrThrow(InventoryEntry.COLUMN_PRODUCT_IMAGE));
        Drawable productImage = new BitmapDrawable(context.getResources(), BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length));
        String productName = cursor.getString(cursor.getColumnIndexOrThrow(InventoryEntry.COLUMN_PRODUCT_NAME));
        int intProductPrice = cursor.getInt(cursor.getColumnIndexOrThrow(InventoryEntry.COLUMN_PRODUCT_PRICE));
        BigDecimal bDProductPrice = BigDecimal.valueOf(intProductPrice).movePointLeft(2);
        String productPrice = "Price: " + NumberFormat.getCurrencyInstance(Locale.US).format(bDProductPrice);
        int productQuantityInt = cursor.getInt(cursor.getColumnIndexOrThrow(InventoryEntry.COLUMN_PRODUCT_QUANTITY));
        String productQuantity = "Qty: " + productQuantityInt;

        // Populate fields with extracted properties
        nameTextView.setText(productName);
        productImageView.setImageDrawable(productImage);
        priceTextView.setText(productPrice);
        quantityTextView.setText(productQuantity);

        // Set click listner on sale button
        final int productQtyInt = cursor.getInt(cursor.getColumnIndexOrThrow(InventoryEntry.COLUMN_PRODUCT_QUANTITY));
        final int row_Id = cursor.getInt(cursor.getColumnIndexOrThrow(InventoryEntry._ID));
        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (productQtyInt >= 1) {
                    // Create a new map of values, where column names are the keys
                    ContentValues values = new ContentValues();
                    // decrement quantity by one and match it with column key
                    int decrementedProductQuantity = productQtyInt - 1;
                    values.put(InventoryEntry.COLUMN_PRODUCT_QUANTITY, decrementedProductQuantity);
                    Uri currentInventoryUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, row_Id);
                    int rowsUpdated = context.getContentResolver().update(currentInventoryUri, values, null, null);

                    if (rowsUpdated != 0) {
                        Toast.makeText(context, context.getString(R.string.sale_complete), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, context.getString(R.string.error_updating_sale), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.invalid_sale),Toast.LENGTH_SHORT).show();
                }
                }
        });
    }
}