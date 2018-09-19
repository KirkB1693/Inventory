package com.example.android.inventory;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.android.inventory.data.InventoryContract.InventoryEntry;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Allows user to create a new product or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * EditText field to enter the product's name
     */
    private EditText mNameEditText;

    /**
     * EditText field to enter the product's price
     */
    private EditText mPriceEditText;

    /**
     * EditText field to enter the product's quantity
     */
    private EditText mQuantityEditText;

    /**
     * EditText field to enter the product's supplier
     */
    private EditText mSupplierEditText;

    /**
     * EditText field to enter the product's reorder phone
     */
    private EditText mPhoneEditText;

    /**
     * EditText field to enter the product's reorder website
     */
    private EditText mWebsiteEditText;

    /**
     * EditText field to enter the product's reorder method
     */
    private Spinner mReorderMethodSpinner;

    private FloatingActionButton mImageFAB;

    private static final int PICK_IMAGE_ID = 5; // the number doesn't matter

    private ImageView mProductImage;

    /**
     * Reorder method of the product. The possible values are:
     * 0 for unknown method, 1 for phone, 2 for website.
     */
    private int mReorderMethod = 0;

    private static final int PRODUCT_LOADER = 0;

    private boolean mInventoryHasChanged = false;

    Uri currentInventoryUri;

    // OnTouchListener that listens for any user touches on a View, implying that they are modifying
    // the view, and we change the mInventoryHasChanged boolean to true.

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mInventoryHasChanged = true;
            return false;
        }
    };

    private View.OnKeyListener mKeyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            mInventoryHasChanged = true;
            return false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Intent intent = getIntent();
        currentInventoryUri = intent.getData();

        if (currentInventoryUri == null) {
            setTitle(getString(R.string.editor_activity_title_new_product));
            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a product that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            setTitle(getString(R.string.editor_activity_title_edit_product));
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_product_name);
        mPriceEditText = (EditText) findViewById(R.id.edit_product_price);
        mQuantityEditText = (EditText) findViewById(R.id.edit_product_quantity);
        mReorderMethodSpinner = (Spinner) findViewById(R.id.spinner_method);
        mImageFAB = (FloatingActionButton) findViewById(R.id.fab_add_image);
        mProductImage = (ImageView) findViewById(R.id.edit_product_image);
        mSupplierEditText = (EditText) findViewById(R.id.edit_product_supplier);
        mPhoneEditText = (EditText) findViewById(R.id.edit_product_reorder_phone);
        mWebsiteEditText = (EditText) findViewById(R.id.edit_product_reorder_website);

        mNameEditText.setOnTouchListener(mTouchListener);
        mNameEditText.setOnKeyListener(mKeyListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnKeyListener(mKeyListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnKeyListener(mKeyListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnKeyListener(mKeyListener);
        mReorderMethodSpinner.setOnTouchListener(mTouchListener);
        mImageFAB.setOnTouchListener(mTouchListener);

        // Setup FAB to open ImagePicker
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_add_image);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent chooseImageIntent = ImagePicker.getPickImageIntent(EditorActivity.this);
                startActivityForResult(chooseImageIntent, PICK_IMAGE_ID);
            }


        });

        setupSpinner();

        if (currentInventoryUri != null) {
            // Prepare the loader. Either re-connect with an existing one,
            // or start a new one.
            getLoaderManager().initLoader(PRODUCT_LOADER, null, this);
        }
    }

    /**
     * Setup the dropdown spinner that allows the user to select the reorder method of the product.
     */
    private void setupSpinner() {
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        ArrayAdapter genderSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_gender_options, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        genderSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mReorderMethodSpinner.setAdapter(genderSpinnerAdapter);

        // Set the integer mSelected to the constant values
        mReorderMethodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.reorder_method_phone))) {
                        mReorderMethod = InventoryEntry.REORDER_PHONE; // Phone
                        mPhoneEditText.setVisibility(View.VISIBLE);
                        mWebsiteEditText.setVisibility(View.INVISIBLE);
                    } else if (selection.equals(getString(R.string.reorder_method_website))) {
                        mReorderMethod = InventoryEntry.REORDER_WEBSITE; // Website
                        mPhoneEditText.setVisibility(View.INVISIBLE);
                        mWebsiteEditText.setVisibility(View.VISIBLE);
                    } else {
                        mReorderMethod = InventoryEntry.REORDER_UNKNOWN; // Unknown
                        mPhoneEditText.setVisibility(View.INVISIBLE);
                        mWebsiteEditText.setVisibility(View.INVISIBLE);
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mReorderMethod = 0; // Unknown
            }
        });
    }

    // Gets user input and saves a new product into the database
    private boolean saveInventory() {
        String productName = mNameEditText.getText().toString().trim();
        String productPriceString = mPriceEditText.getText().toString().trim();
        String productQuantityString = mQuantityEditText.getText().toString().trim();
        String productSupplier = mSupplierEditText.getText().toString().trim();
        String productReorderPhone = mPhoneEditText.getText().toString().trim();
        String productReorderWebsite = mWebsiteEditText.getText().toString().trim();

        if (productName.isEmpty() && productPriceString.isEmpty() && productQuantityString.isEmpty() && productSupplier.isEmpty() && (mReorderMethod == InventoryEntry.REORDER_UNKNOWN)) {
            return false;
        }

        if (productName.isEmpty()) {
            // if productName is empty, prompt user to enter a product name and return to edit screen.
            Toast.makeText(this, R.string.toast_enter_product_name, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (productQuantityString.isEmpty()) {
            // if productQuantity is empty, prompt user to enter a quantity and return to edit screen.
            Toast.makeText(this, R.string.toast_enter_quantity, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (productPriceString.isEmpty()) {
            // if productPriceString is empty, prompt user to enter a price and return to edit screen.
            Toast.makeText(this, R.string.toast_enter_price, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (productSupplier.isEmpty()) {
            // if productSupplier is empty, prompt user to enter a supplier and return to edit screen.
            Toast.makeText(this, R.string.toast_enter_supplier, Toast.LENGTH_SHORT).show();
            return false;
        }



        Bitmap productImageBitmap = drawableToBitmap(mProductImage.getDrawable());

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        productImageBitmap.compress(Bitmap.CompressFormat.WEBP, 25, stream);
        byte[] productImage = stream.toByteArray();

        // convert price to integer (remove the decimal place to represent pennies)
        int productPrice = 0;
        if (productPriceString != null) {
            String newStr = productPriceString.replaceAll("[^\\d.]+", "");
            int decimalCount = 0;
            if (newStr.contains(".")) {
                decimalCount = newStr.length() - newStr.indexOf(".") - 1;
            }
            String newIntStr = newStr.replace(".", "");
            switch (decimalCount) {
                case 0:
                    productPrice = Integer.parseInt(newIntStr) * 100;
                    break;
                case 1:
                    productPrice = Integer.parseInt(newIntStr) * 10;
                    break;
                default:
                    productPrice = Integer.parseInt(newIntStr);
            }

        }

        int productQuantity = Integer.parseInt(productQuantityString);


        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_PRODUCT_NAME, productName);
        values.put(InventoryEntry.COLUMN_PRODUCT_PRICE, productPrice);
        values.put(InventoryEntry.COLUMN_PRODUCT_QUANTITY, productQuantity);
        values.put(InventoryEntry.COLUMN_PRODUCT_SUPPLIER, productSupplier);
        values.put(InventoryEntry.COLUMN_PRODUCT_IMAGE, productImage);
        values.put(InventoryEntry.COLUMN_PRODUCT_REORDER_METHOD, mReorderMethod);
        values.put(InventoryEntry.COLUMN_PRODUCT_REORDER_PHONE, productReorderPhone);
        values.put(InventoryEntry.COLUMN_PRODUCT_REORDER_WEBSITE, productReorderWebsite);

        Uri uri = null;
        int rowsUpdated = 0;
        if (currentInventoryUri == null) {
            // Insert the new row, returning the primary key value of the new row
            uri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);
        } else {
            rowsUpdated = getContentResolver().update(currentInventoryUri, values, null, null);
        }


        if (uri != null || rowsUpdated != 0) {
            Toast.makeText(this, getString(R.string.product_saved), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.error_saving_product), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

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

    public void reorderProductClicked(View view) {
        switch (mReorderMethod) {
            case 0:
                Toast.makeText(this, R.string.toast_select_order_method, Toast.LENGTH_SHORT).show();
                break;
            case 1:
                String phone = mPhoneEditText.getText().toString().trim().replaceAll("[^\\d]+", "");
                if (phone != null) {
                    Intent intentPhone = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                    if (intentPhone.resolveActivity(getPackageManager()) != null) {
                        startActivity(intentPhone);
                    } else {
                        Toast.makeText(this, R.string.toast_missing_app, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, R.string.toast_enter_phone, Toast.LENGTH_SHORT).show();
                }
                break;
            case 2:
                String website = mWebsiteEditText.getText().toString().trim();
                if (website != null) {
                    if (!website.startsWith("http://") && !website.startsWith("https://")) {
                        website = "http://" + website;
                    }
                    Intent intentWeb = new Intent(Intent.ACTION_VIEW, Uri.parse(website));
                    if (intentWeb.resolveActivity(getPackageManager()) != null) {
                        startActivity(intentWeb);
                    } else {
                        Toast.makeText(this, R.string.toast_missing_web_app, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, R.string.toast_enter_website, Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                Toast.makeText(this, R.string.toast_invalid_order_method, Toast.LENGTH_SHORT).show();
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICK_IMAGE_ID:
                Bitmap bitmap = ImagePicker.getImageFromResult(this, resultCode, data);
                mProductImage.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new product, hide the "Delete" menu item.
        if (currentInventoryUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteInventory();
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
     * Perform the deletion of the product in the database.
     */
    private void deleteInventory() {
        if (currentInventoryUri != null) {
            int linesDeleted = getContentResolver().delete(currentInventoryUri, null, null);
            if (linesDeleted != 0) {
                Toast.makeText(EditorActivity.this, R.string.editor_delete_product_successful, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(EditorActivity.this, R.string.editor_delete_product_failed, Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
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

    @Override
    public void onBackPressed() {
        // If the product hasn't changed, continue with handling back button press
        if (!mInventoryHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save product to database
                if (!saveInventory()) {
                    return false;
                };
                // Exit activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the product hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mInventoryHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(this, currentInventoryUri,
                null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || (cursor != null && cursor.getCount() == 0)) {
            return;
        }
        cursor.moveToFirst();
        mNameEditText.setText(cursor.getString(cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_NAME)));
        mPriceEditText.setText(formatPriceToString(cursor.getInt(cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_PRICE))));
        mReorderMethodSpinner.setSelection(cursor.getInt(cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_REORDER_METHOD)));
        mQuantityEditText.setText(cursor.getString(cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_QUANTITY)));
        mSupplierEditText.setText(cursor.getString(cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_SUPPLIER)));
        byte[] byteArray = cursor.getBlob(cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_IMAGE));
        mProductImage.setImageDrawable(new BitmapDrawable(getResources(), BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length)));
        switch (mReorderMethod) {
            case 0:
                mPhoneEditText.setVisibility(View.INVISIBLE);
                mWebsiteEditText.setVisibility(View.INVISIBLE);
                break;
            case 1:
                mPhoneEditText.setVisibility(View.VISIBLE);

                mWebsiteEditText.setVisibility(View.INVISIBLE);
                break;
            case 2:
                mPhoneEditText.setVisibility(View.INVISIBLE);
                mWebsiteEditText.setVisibility(View.VISIBLE);
                break;
            default:
                mPhoneEditText.setVisibility(View.INVISIBLE);
                mWebsiteEditText.setVisibility(View.INVISIBLE);
        }
        mPhoneEditText.setText(cursor.getString(cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_REORDER_PHONE)));
        mWebsiteEditText.setText(cursor.getString(cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_REORDER_WEBSITE)));
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNameEditText.setText("");
        mPriceEditText.setText("");
        mSupplierEditText.setText("");
        mReorderMethodSpinner.setSelection(0);
        mQuantityEditText.setText("");
        mProductImage.setImageResource(R.drawable.placeholder_image);
        mPhoneEditText.setText("");
        mWebsiteEditText.setText("");
    }


    public String formatPriceToString(int price) {
        final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        numberFormat.setGroupingUsed(true);
        numberFormat.setMaximumFractionDigits(2);
        numberFormat.setMinimumFractionDigits(2);
        return numberFormat.format(BigDecimal.valueOf(price).scaleByPowerOfTen(-2));
    }

    public void incrementQuantity (View view) {
        String productQuantityString = mQuantityEditText.getText().toString().trim();
        String result = "1";
        if (!productQuantityString.isEmpty()) {
            result = Integer.toString(Integer.parseInt(productQuantityString) + 1);
        }
        mQuantityEditText.setText(result);
    }

    public void decrementQuantity (View view) {
        String productQuantityString = mQuantityEditText.getText().toString().trim();
        String result = "0";
        if (!productQuantityString.isEmpty()) {
            int productQuantity = Integer.parseInt(productQuantityString);
            if (productQuantity > 0){
            result = Integer.toString(Integer.parseInt(productQuantityString) - 1);} else {
                Toast.makeText(view.getContext(),"Quantity can't be less than 0", Toast.LENGTH_SHORT).show();
            }
        }
        mQuantityEditText.setText(result);
    }

}