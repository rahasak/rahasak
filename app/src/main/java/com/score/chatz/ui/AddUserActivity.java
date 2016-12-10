package com.score.chatz.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.score.chatz.R;
import com.score.chatz.utils.NetworkUtil;

public class AddUserActivity extends AppCompatActivity {

    private static final String TAG = AddUserActivity.class.getName();

    // Ui elements
    private TextView invite_text;
    private Button addFriendBtn;
    private Button openContactsBtn;
    private Toolbar toolbar;

    private Typeface typeface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user);
        typeface = Typeface.createFromAsset(getAssets(), "fonts/GeosansLight.ttf");

        setupToolbar();
        setupUiElements();
        setupActionBar();
        setupAddUsersBtn();
        setupOpenContactsBtn();
        setupBackBtn();
    }

    private void setupUiElements() {
        invite_text = (TextView) findViewById(R.id.textView);
        invite_text.setTypeface(typeface, Typeface.NORMAL);
    }

    private void setupToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setCollapsible(false);
        toolbar.setOverScrollMode(Toolbar.OVER_SCROLL_NEVER);
        setSupportActionBar(toolbar);
    }

    private void setupActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setCustomView(getLayoutInflater().inflate(R.layout.add_user_header, null));
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

    private void setupOpenContactsBtn() {
        openContactsBtn = (Button) findViewById(R.id.add_from_contacts_btn);
        openContactsBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Click action
                Intent intent = new Intent(AddUserActivity.this, ContactUserListActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setupAddUsersBtn() {
        addFriendBtn = (Button) findViewById(R.id.add_friend_btn);
        addFriendBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (NetworkUtil.isAvailableNetwork(AddUserActivity.this)) {
                    // Click action
                    Intent intent = new Intent(AddUserActivity.this, AddUsernameActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(AddUserActivity.this, getResources().getString(R.string.no_internet), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
