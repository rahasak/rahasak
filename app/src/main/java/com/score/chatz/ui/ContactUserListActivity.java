package com.score.chatz.ui;

import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
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
import android.widget.TextView;

import com.score.chatz.R;
import com.score.chatz.asyncTasks.ContactReader;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.interfaces.IContactReaderListener;
import com.score.chatz.pojo.Contact;
import com.score.chatz.utils.ActivityUtils;
import com.score.chatz.utils.PreferenceUtils;

import java.util.ArrayList;

public class ContactUserListActivity extends BaseActivity implements IContactReaderListener {

    private EditText searchView;

    private ListView contactListView;
    private ContactUserListAdapter adapter;

    private static final String TAG = ContactUserListActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts_list);
        typeface = Typeface.createFromAsset(getAssets(), "fonts/GeosansLight.ttf");

        setupToolbar();
        setupActionBar();
        setupSearchView();
        fetchContacts();
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setCollapsible(false);
        toolbar.setOverScrollMode(Toolbar.OVER_SCROLL_NEVER);
        setSupportActionBar(toolbar);
    }

    private void setupActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setCustomView(getLayoutInflater().inflate(R.layout.search_contacts_header, null));
        getSupportActionBar().setDisplayOptions(android.support.v7.app.ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);

        // title
        TextView titleText = (TextView) findViewById(R.id.title);
        titleText.setTypeface(typeface, Typeface.BOLD);

        // back button
        ImageView backBtn = (ImageView) findViewById(R.id.back_btn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setupSearchView() {
        searchView = (EditText) findViewById(R.id.inputSearch);
        searchView.getBackground().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN);

        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void initContactList(ArrayList<Contact> contactList) {
        contactListView = (ListView) findViewById(R.id.contacts_list);
        contactListView.setTextFilterEnabled(true);
        adapter = new ContactUserListAdapter(this, contactList);
        contactListView.setAdapter(adapter);

        // click listener
        contactListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Contact contact = (Contact) adapter.getItem(position);
                onContactItemClick(contact);
            }
        });
    }

    private void fetchContacts() {
        ActivityUtils.showProgressDialog(this, "Loading...");

        ContactReader contactReader = new ContactReader(this, this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            contactReader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            contactReader.execute("PLAY");
        }
    }

    private void onContactItemClick(final Contact contact) {
        // check existing secret user with given phone no
        if (!new SenzorsDbSource(this).isExistingUserWithPhoneNo(contact.getPhoneNo())) {
            String confirmationMessage = "<font size=10>Are you sure you want to share your rahsak username to </font> <font color=#F88F8C>" + "<b>" + contact.getName() + "</b>" + "</font> (" + contact.getPhoneNo() + "), via sms?";
            try {
                final String username = PreferenceUtils.getUser(this).getUsername();
                displayConfirmationMessageDialog(confirmationMessage, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String message = "#Rahasak #request\nI'm using Rahasak app(http://play.google.com/store/apps/details?id=com.score.chatz) #username " + username + " #code 41r33";
                        sendSMS(contact.getPhoneNo(), message);

                        ActivityUtils.showCustomToastShort("Request sent via SMS", ContactUserListActivity.this);
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

    @Override
    public void onPostRead(ArrayList<Contact> contactList) {
        ActivityUtils.cancelProgressDialog();
        initContactList(contactList);
    }
}
