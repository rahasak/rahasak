package com.score.chatz.ui;

import android.database.Cursor;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import com.score.chatz.R;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.utils.ActivityUtils;
import com.score.chatz.utils.PreferenceUtils;

public class ContactsListActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {

    private static final String TAG = ContactsListActivity.class.getName();

    // Ui Elements
    private EditText inputSearch;
    private Toolbar toolbar;
    private ListView mContactsList;

    private ContactsListAdapter mCursorAdapter;

    /*
     * Defines an array that contains column names to move from
     * the Cursor to the ListView.
     */
    private final static String[] FROM_COLUMNS = {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? ContactsContract.Contacts.DISPLAY_NAME_PRIMARY : ContactsContract.Contacts.DISPLAY_NAME
    };

    /*
     * Defines an array that contains resource ids for the layout views
     * that get the Cursor column contents. The id is pre-defined in
     * the Android framework, so it is prefaced with "android.R.id"
     */
    private final static int[] TO_IDS = {
            android.R.id.text1
    };

    // Defines the text expression
    private static final String SELECTION = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " LIKE ? AND " + ContactsContract.Contacts.HAS_PHONE_NUMBER + " =1" : ContactsContract.Contacts.DISPLAY_NAME + " LIKE ? AND " + ContactsContract.Contacts.HAS_PHONE_NUMBER + " =1";

    // Defines a variable for the search string, by default list loads nothing, thus empty string
    private String mSearchString = "%%";
    private String[] mSelectionArgs = {mSearchString};

    // Content loader identifier
    private static final Integer CONTENT_LOADER_ID = 0;

    // Sort list by SORT_ORDER
    private static final String SORT_ORDER = ContactsContract.Contacts.DISPLAY_NAME + " ASC ";

    // Define what you want to get out from provider
    private static final String[] PROJECTION = {
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? ContactsContract.Contacts.DISPLAY_NAME_PRIMARY : ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.HAS_PHONE_NUMBER
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts_list);

        setupUiForSearch();
        setupToolbar();
        setupActionBar();
        setupBackBtn();
        setupContactsList();
        setupContactsListAdapter();

        // Initializes the loader
        getSupportLoaderManager().initLoader(CONTENT_LOADER_ID, null, this);
    }

    private void setupContactsList() {
        mContactsList = (ListView) this.findViewById(R.id.contacts_list);
        mContactsList.setOnItemClickListener(this);
    }

    private void setupContactsListAdapter() {
        mCursorAdapter = new ContactsListAdapter(this, R.layout.contacts_list_item, null, FROM_COLUMNS, TO_IDS, 0);
        mContactsList.setAdapter(mCursorAdapter);
    }

    private void setupToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setCollapsible(false);
        toolbar.setOverScrollMode(Toolbar.OVER_SCROLL_NEVER);
        setSupportActionBar(toolbar);
    }

    private void setupActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setCustomView(getLayoutInflater().inflate(R.layout.search_contacts_header, null));
        getSupportActionBar().setDisplayOptions(android.support.v7.app.ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
    }

    private void setupBackBtn() {
        ImageView backBtn = (ImageView) findViewById(R.id.back_btn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setupUiForSearch() {
        inputSearch = (EditText) findViewById(R.id.inputSearch);
        inputSearch.getBackground().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN);

        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                // When user changed the Text
                mSelectionArgs[0] = "%" + cs + "%";
                getSupportLoaderManager().restartLoader(CONTENT_LOADER_ID, null, ContactsListActivity.this);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                // Nothing to do in this case
            }

            @Override
            public void afterTextChanged(Editable arg0) {
                // Nothing to do in this case
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View item, int position, long rowID) {
        Cursor cursor = (Cursor) parent.getItemAtPosition(position);
        int itemId = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts._ID));
        String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        boolean hasPhoneNumber = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0;

        if (hasPhoneNumber) {
            handleOnClickWithPhone(itemId, displayName);
        } else {
            ActivityUtils.showCustomToastShort("This user has no mobile number", this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        return new CursorLoader(this, ContactsContract.Contacts.CONTENT_URI, PROJECTION, SELECTION, mSelectionArgs, SORT_ORDER);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mCursorAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    private void handleOnClickWithPhone(int itemId, String displayName) {
        // Using the item_ID now we will get contact phone number
        final Cursor cursorPhone = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{String.valueOf(itemId)},
                null);

        cursorPhone.moveToFirst();
        final String phoneNo = cursorPhone.getString(cursorPhone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

        // check existing secret user with given phone no
        if (!new SenzorsDbSource(this).isExistingUserWithPhoneNo(phoneNo)) {
            String confirmationMessage = "<font size=10>Are you sure you want to share your rahsak username to </font> <font color=#F88F8C>" + "<b>" + displayName + "</b>" + "</font> (" + phoneNo + "), via sms?";
            try {
                final String username = PreferenceUtils.getUser(this).getUsername();
                displayConfirmationMessageDialog(confirmationMessage, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String message = "#Rahasak #request\nI'm using Rahasak app(http://play.google.com/store/apps/details?id=com.score.chatz) #username " + username + " #code 41r33";
                        //String message = "#Rahasak #request\nI'm using Rahasak App(https://goo.gl/KYreLa). #username " + username + " #code 41r33";

                        sendSMS(phoneNo, message);

                        ActivityUtils.showCustomToastShort("Request sent via SMS", ContactsListActivity.this);
                    }
                });
            } catch (NoUserException ex) {
                ex.printStackTrace();
            }
        } else {
            ActivityUtils.showCustomToastShort("This user already added in you secret contact list", this);
        }
    }

    private void sendSMS(String phoneNumber, String message) {
        SmsManager sms = SmsManager.getDefault();
        Log.i(TAG, "SMS Body -> " + message);
        sms.sendTextMessage(phoneNumber, null, message, null, null);
    }

}
