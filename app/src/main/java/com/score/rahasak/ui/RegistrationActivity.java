package com.score.rahasak.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.score.rahasak.R;
import com.score.rahasak.application.IntentProvider;
import com.score.rahasak.enums.IntentType;
import com.score.rahasak.exceptions.InvalidInputFieldsException;
import com.score.rahasak.utils.ActivityUtils;
import com.score.rahasak.utils.CryptoUtils;
import com.score.rahasak.utils.NetworkUtil;
import com.score.rahasak.utils.PreferenceUtils;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;

public class RegistrationActivity extends BaseActivity {

    private static final String TAG = RegistrationActivity.class.getName();

    // ui controls
    private Button registerBtn;
    private EditText editTextUserId;
    private User registeringUser;
    private Toolbar toolbar;

    private BroadcastReceiver senzReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got message from Senz service");
            if (intent.hasExtra("SENZ")) {
                Senz senz = intent.getExtras().getParcelable("SENZ");
                handleSenz(senz);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        setupToolbar();
        setupUi();
        setupRegisterBtn();
        setupActionBar();

        // generate RSA keys
        doPreRegistration();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "Bind to senz service");
        bindToService();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // unbind from service
        if (isServiceBound) {
            Log.d(TAG, "Unbind to senz service");
            unbindService(senzServiceConnection);

            isServiceBound = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(senzReceiver, IntentProvider.getIntentFilter(IntentType.SENZ));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (senzReceiver != null) unregisterReceiver(senzReceiver);
    }

    private void setupActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setCustomView(getLayoutInflater().inflate(R.layout.registration_header, null));
        getSupportActionBar().setDisplayOptions(android.support.v7.app.ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
    }

    private void setupToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setCollapsible(false);
        toolbar.setOverScrollMode(Toolbar.OVER_SCROLL_NEVER);
        setSupportActionBar(toolbar);
    }

    private void setupUi() {
        editTextUserId = (EditText) findViewById(R.id.registering_user_id);
        editTextUserId.setTypeface(typefaceThin, Typeface.NORMAL);
        editTextUserId.getBackground().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN);
        ((TextView) findViewById(R.id.welcome_message)).setTypeface(typefaceThin, Typeface.NORMAL);
    }

    private void setupRegisterBtn() {
        registerBtn = (Button) findViewById(R.id.register_btn);
        registerBtn.setTypeface(typefaceThin, Typeface.BOLD);
        registerBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (NetworkUtil.isAvailableNetwork(RegistrationActivity.this)) {
                    onClickRegister();
                } else {
                    Toast.makeText(RegistrationActivity.this, "No internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Sign-up button action,
     * create user and validate fields from here
     */
    private void onClickRegister() {
        // create user
        String username = editTextUserId.getText().toString().toLowerCase().trim();
        registeringUser = new User("0", username);
        try {
            ActivityUtils.isValidRegistrationFields(registeringUser);
            String confirmationMessage = "<font size=10>Are you sure you want to register with Rahasak using </font> <font color=#F88F8C>" + "<b>" + registeringUser.getUsername() + "</b>" + "</font>";
            displayConfirmationMessageDialog(confirmationMessage, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityUtils.showProgressDialog(RegistrationActivity.this, "Please wait...");
                    doRegistration();
                }
            });
        } catch (InvalidInputFieldsException e) {
            ActivityUtils.showCustomToast("Invalid inputs in username field", this);
            e.printStackTrace();
        }
    }

    /**
     * Create user
     * First initialize key pair
     * start service
     * bind service
     */
    private void doPreRegistration() {
        try {
            CryptoUtils.initKeys(this);
        } catch (NoSuchProviderException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize registering user
     * username will generated via public key hash
     */
    private void initRegUser() {

    }

    /**
     * Create register senz
     * Send register senz to senz service via service binder
     */
    private void doRegistration() {
        // first create create senz
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put(getResources().getString(R.string.time), ((Long) (System.currentTimeMillis() / 1000)).toString());
        senzAttributes.put(getResources().getString(R.string.pubkey), PreferenceUtils.getRsaKey(this, CryptoUtils.PUBLIC_KEY));

        // new senz
        String id = "_ID";
        String signature = "";
        SenzTypeEnum senzType = SenzTypeEnum.SHARE;
        User sender = new User("", registeringUser.getUsername());
        User receiver = new User("", getResources().getString(R.string.switch_name));
        Senz senz = new Senz(id, signature, senzType, sender, receiver, senzAttributes);

        // Sending senz to service
        send(senz);
    }

    /**
     * Handle broadcast message receives
     * Need to handle registration success failure here
     *
     * @param senz intent
     */
    private void handleSenz(Senz senz) {
        if (senz.getAttributes().containsKey("status")) {
            // msg response received
            ActivityUtils.cancelProgressDialog();
            String msg = senz.getAttributes().get("status");
            if (msg != null && (msg.equalsIgnoreCase("REG_DONE") || msg.equalsIgnoreCase("REG_ALR"))) {
                ActivityUtils.showCustomToast("Successfully registered", this);
                // save user
                // navigate home
                PreferenceUtils.saveUser(this, registeringUser);
                navigateToHome();
            } else if (msg != null && msg.equalsIgnoreCase("REG_FAIL")) {
                String informationMessage = "<font size=10>Seems username </font> <font color=#F88F8C>" + "<b>" + registeringUser.getUsername() + "</b>" + "</font> <font> already obtained by some other user, try a different username</font>";
                displayInformationMessageDialog("Registration fail", informationMessage);
            }
        }
    }

    /**
     * Switch to home activity
     * This method will be call after successful login
     */
    private void navigateToHome() {
        Intent intent = new Intent(RegistrationActivity.this, DrawerActivity.class);
        RegistrationActivity.this.startActivity(intent);
        RegistrationActivity.this.finish();
    }
}
