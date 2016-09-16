package com.score.chatz.ui;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.score.chatz.R;
import com.score.chatz.exceptions.InvalidInputFieldsException;
import com.score.chatz.handlers.IntentProvider;
import com.score.chatz.handlers.SenzHandler;
import com.score.chatz.utils.ActivityUtils;
import com.score.chatz.utils.PreferenceUtils;
import com.score.chatz.utils.RSAUtils;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;

public class RegistrationActivity extends BaseActivity {

    private static final String TAG = RegistrationActivity.class.getName();

    //UI controls
    private Button registerBtn;
    private EditText editTextUserId;
    private User registeringUser;
    private TextView welcomeTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        setupUI();
        setupRegisterBtn();

        SenzHandler.getInstance(getApplicationContext());

        //Register all receivers
        registerReceivers();

        //Generate RSA keys
        doPreRegistration();
    }

    /**
     * Register all intent receivers
     */
    private void registerReceivers() {
        registerReceiver(senzMessageReceiver, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.DATA_SENZ));
    }

    private void unRegisterReceivers() {
        if (senzMessageReceiver != null) unregisterReceiver(senzMessageReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //remove all registers
        unRegisterReceivers();
    }

    private void setupUI() {
        editTextUserId = (EditText) findViewById(R.id.registering_user_id);
        welcomeTextView = (TextView) findViewById(R.id.welcome_text);
        welcomeTextView.setTypeface(typeface);
    }

    private void setupRegisterBtn() {
        registerBtn = (Button) findViewById(R.id.register_btn);
        registerBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onClickRegister();
            }
        });
    }

    /**
     * Sign-up button action,
     * create user and validate fields from here
     */
    private void onClickRegister() {
        // create user
        String username = editTextUserId.getText().toString().trim();
        registeringUser = new User("0", username);
        try {
            ActivityUtils.isValidRegistrationFields(registeringUser);
            String confirmationMessage = "<font color=#000000>Are you sure you want to register with Rahask using </font> <font color=#ffc027>" + "<b>" + registeringUser.getUsername() + "</b>" + "</font>";
            displayConfirmationMessageDialog(confirmationMessage, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityUtils.showProgressDialog(RegistrationActivity.this, "Please wait...");
                    doRegistration();
                }
            });
        } catch (InvalidInputFieldsException e) {
            ActivityUtils.showToast("Invalid username", this);
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
            RSAUtils.initKeys(this);
        } catch (NoSuchProviderException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create register senz
     * Send register senz to senz service via service binder
     */
    private void doRegistration() {

        // first create create senz
        HashMap<String, String> senzAttributes = new HashMap<>();
        senzAttributes.put(getResources().getString(R.string.time), ((Long) (System.currentTimeMillis() / 1000)).toString());
        senzAttributes.put(getResources().getString(R.string.pubkey), PreferenceUtils.getRsaKey(this, RSAUtils.PUBLIC_KEY));

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

    private BroadcastReceiver senzMessageReceiver = new BroadcastReceiver() {
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
        if (action.equals("com.score.chatz.DATA_SENZ")) {
            Senz senz = intent.getExtras().getParcelable("SENZ");
            if (senz.getAttributes().containsKey("msg")) {
                // msg response received
                ActivityUtils.cancelProgressDialog();
                String msg = senz.getAttributes().get("msg");
                if (msg != null && msg.equalsIgnoreCase("REG_DONE")) {
                    ActivityUtils.showToast("Successfully registered", this);
                    // save user
                    // navigate home
                    PreferenceUtils.saveUser(this, registeringUser);
                    navigateToHome();
                } else if (msg != null && msg.equalsIgnoreCase("REG_FAIL")) {
                    String informationMessage = "<font color=#4a4a4a>Seems username </font> <font color=#eada00>" + "<b>" + registeringUser.getUsername() + "</b>" + "</font> <font color=#4a4a4a> already obtained by some other user, try a different username</font>";
                    displayInformationMessageDialog("Registration fail", informationMessage);
                }
            }
        }
    }

    /**
     * Switch to home activity
     * This method will be call after successful login
     */
    private void navigateToHome() {
        Intent intent = new Intent(RegistrationActivity.this, HomeActivity.class);
        RegistrationActivity.this.startActivity(intent);
        RegistrationActivity.this.finish();
    }
}
