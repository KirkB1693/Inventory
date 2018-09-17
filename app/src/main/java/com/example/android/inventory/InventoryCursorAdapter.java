package com.example.android.inventory;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

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
    public void bindView(View view, Context context, Cursor cursor) {
        // Find fields to populate in inflated template
        ImageView productImageView = (ImageView) view.findViewById(R.id.list_product_image);
        TextView nameTextView = (TextView) view.findViewById(R.id.list_product_name);
        TextView priceTextView = (TextView) view.findViewById(R.id.list_product_price);
        TextView quantityTextView = (TextView) view.findViewById(R.id.list_product_quantity);
        // Extract properties from cursor
        byte[] byteArray = cursor.getBlob(cursor.getColumnIndexOrThrow(InventoryEntry.COLUMN_PRODUCT_IMAGE));
        Drawable productImage = new BitmapDrawable(context.getResources(), BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length));
        String productName = cursor.getString(cursor.getColumnIndexOrThrow(InventoryEntry.COLUMN_PRODUCT_NAME));
        int intProductPrice = cursor.getInt(cursor.getColumnIndexOrThrow(InventoryEntry.COLUMN_PRODUCT_PRICE));
        BigDecimal bDProductPrice = BigDecimal.valueOf(intProductPrice).movePointLeft(2);
        String productPrice = "Price: " + NumberFormat.getCurrencyInstance(Locale.US).format(bDProductPrice);
        String productQuantity = "Qty: " + cursor.getInt(cursor.getColumnIndexOrThrow(InventoryEntry.COLUMN_PRODUCT_QUANTITY));

        // Populate fields with extracted properties
        nameTextView.setText(productName);
        productImageView.setImageDrawable(productImage);
        priceTextView.setText(productPrice);
        quantityTextView.setText(productQuantity);
    }
}