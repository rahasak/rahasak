package com.score.chatz.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.score.chatz.R;
import com.score.chatz.exceptions.InvalidInputFieldsException;
import com.score.chatz.application.IntentProvider;
import com.score.chatz.utils.ActivityUtils;
import com.score.chatz.utils.NetworkUtil;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.util.HashMap;

public class AddUserActivity extends BaseActivity {

    private static final String TAG = AddUserActivity.class.getName();

    // Ui elements
    private TextView invite_text_part_1;
    private TextView invite_text_part_2;
    private TextView invite_text_part_3;
    private TextView invite_text_part_4;
    private Button addFriendBtn;
    private EditText editTextUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user);

        setupUiElements();
        setupActionBar();
        setupAddUsersBtn();
        setupFonts();
        //startService();
    }

    private void setupUiElements() {
        invite_text_part_1 = (TextView) findViewById(R.id.textView);
        invite_text_part_2 = (TextView) findViewById(R.id.textView2);
        invite_text_part_3 = (TextView) findViewById(R.id.textView3);
        invite_text_part_4 = (TextView) findViewById(R.id.textView4);
        editTextUserId = (EditText) findViewById(R.id.friend_id);
    }

    private void setupActionBar() {
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#636363")));
        getSupportActionBar().setTitle("Invite");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.registerReceiver(senzDataReceiver, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.DATA_SENZ)); //Incoming data share
    }

    @Override
    public void onStop() {
        super.onStop();
        if (senzDataReceiver != null) unregisterReceiver(senzDataReceiver);
    }

    private void setupAddUsersBtn() {
        addFriendBtn = (Button) findViewById(R.id.add_friend_btn);
        addFriendBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (NetworkUtil.isAvailableNetwork(AddUserActivity.this)) {
                    onClickShare();
                } else {
                    Toast.makeText(AddUserActivity.this, "No network connection available", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void onClickShare() {
        String username = editTextUserId.getText().toString().trim();
        User registeringUser = new User("0", username);
        try {
            ActivityUtils.isValidRegistrationFields(registeringUser);
            String confirmationMessage = "<font color=#000000>Are you sure you want to share secrets with </font> <font color=#ffc027>" + "<b>" + registeringUser.getUsername() + "</b>" + "</font>";
            displayConfirmationMessageDialog(confirmationMessage, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityUtils.showProgressDialog(AddUserActivity.this, "Please wait...");
                    share();
                }
            });
        } catch (InvalidInputFieldsException e) {
            Toast.makeText(this, "Invalid username", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void setupFonts() {
        invite_text_part_1.setTypeface(typefaceUltraThin, Typeface.NORMAL);
        invite_text_part_2.setTypeface(typefaceUltraThin, Typeface.NORMAL);
        invite_text_part_3.setTypeface(typefaceUltraThin, Typeface.NORMAL);
        invite_text_part_4.setTypeface(typefaceUltraThin, Typeface.NORMAL);
    }

    /**
     * Share current sensor
     * Need to send share query to server via web socket
     */
    private void share() {
        // create senz attributes
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put(getResources().getString(R.string.lat), getResources().getString(R.string.lat));
        senzAttributes.put(getResources().getString(R.string.lon), getResources().getString(R.string.lon));
        senzAttributes.put(getResources().getString(R.string.msg), getResources().getString(R.string.msg));
        senzAttributes.put(getResources().getString(R.string.chatzphoto), getResources().getString(R.string.chatzphoto));
        senzAttributes.put(getResources().getString(R.string.chatzmsg), getResources().getString(R.string.chatzmsg));
        senzAttributes.put(getResources().getString(R.string.camPerm), "false"); //Default Values, later in ui allow user to configure this on share
        senzAttributes.put(getResources().getString(R.string.locPerm), "false"); //Dafault Values
        senzAttributes.put(getResources().getString(R.string.time), ((Long) (System.currentTimeMillis() / 1000)).toString());

        // new senz
        String id = "_ID";
        String signature = "_SIGNATURE";
        SenzTypeEnum senzType = SenzTypeEnum.SHARE;
        User receiver = new User("", editTextUserId.getText().toString().trim());
        Senz senz = new Senz(id, signature, senzType, null, receiver, senzAttributes);

        // send to service
        send(senz);
    }

    /**
     * Clear input fields and reset activity components
     */
    private void onPostShare(Senz senz) {
        this.goBackToHome();
        ActivityUtils.showToast("Successfully added " + editTextUserId.getText().toString().trim(), this);
        editTextUserId.setText("");
    }


    private void goBackToHome() {
        Log.d(TAG, "go home clicked");
        this.finish();
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


    private BroadcastReceiver senzDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got message from Senz service");
            handleMessage(intent);
        }
    };

    /**
     * Handle broadcast message receives
     * Need to handle registration success failure here
     *
     * @param intent intent
     */
    private void handleMessage(Intent intent) {
        String action = intent.getAction();
        if (action.equalsIgnoreCase("com.score.chatz.DATA_SENZ")) {
            Senz senz = intent.getExtras().getParcelable("SENZ");
            if (senz.getAttributes().containsKey("msg")) {
                // msg response received
                ActivityUtils.cancelProgressDialog();
                String msg = senz.getAttributes().get("msg");
                if (msg != null && msg.equalsIgnoreCase("ShareDone")) {
                    onPostShare(senz);
                } else {
                    String user = editTextUserId.getText().toString().trim();
                    String message = "<font color=#000000>Seems we couldn't connect you with </font> <font color=#eada00>" + "<b>" + user + "</b>" + "</font>";
                    displayInformationMessageDialog("Fail", message);
                }
            }
        }
    }
}
