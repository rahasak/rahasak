package com.score.chatz.ui;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteConstraintException;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.score.chatz.R;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.exceptions.InvalidInputFieldsException;
import com.score.chatz.handlers.SenzHandler;
import com.score.chatz.services.RemoteSenzService;
import com.score.chatz.utils.ActivityUtils;
import com.score.chatz.utils.NetworkUtil;
import com.score.chatz.utils.NotificationUtils;
import com.score.chatz.utils.PreferenceUtils;
import com.score.chatz.utils.RSAUtils;
import com.score.senz.ISenzService;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;

public class RegistrationActivity extends AppCompatActivity {
    private static final String TAG = SplashActivity.class.getName();

    //UI controls
    private Button registerBtn;
    private EditText editTextUserId;
    private User registeringUser;
    private TextView welcomeTextView;

    //Type faces
    private Typeface batangFont;
    private Typeface typeface;

    // service interface
    private ISenzService senzService = null;
    private boolean isServiceBound = false;

    // service connection
    private ServiceConnection senzServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d("TAG", "Connected with senz service");

            isServiceBound = true;
            senzService = ISenzService.Stub.asInterface(service);
            doPreRegistration();
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d("TAG", "Disconnected from senz service");

            senzService = null;
            isServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.registration_activity);

        setupUI();
        setupRegisterBtn();

        SenzHandler.getInstance(getApplicationContext());

        //Register all receivers
        registerReceivers();
    }

    /**
     * Register all intent receivers
     */
    private void registerReceivers(){
        registerReceiver(senzMessageReceiver, new IntentFilter("com.score.chatz.DATA_SENZ"));
    }

    private void unRegisterReceivers(){
        if (senzMessageReceiver != null) unregisterReceiver(senzMessageReceiver);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Unbind from service
        if (isServiceBound == true) unbindService(senzServiceConnection);

        //remove all registers
        unRegisterReceivers();
    }

    private void setupUI(){
        batangFont = Typeface.createFromAsset(getAssets(), "fonts/batang.ttf");
        typeface = Typeface.createFromAsset(getAssets(), "fonts/vegur_2.otf");

        editTextUserId = (EditText) findViewById(R.id.registering_user_id);
        welcomeTextView = (TextView) findViewById(R.id.welcome_text);

        welcomeTextView.setTypeface(typeface);
    }

    private void setupRegisterBtn(){
        registerBtn = (Button) findViewById(R.id.register_btn);
        registerBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (NetworkUtil.isAvailableNetwork(RegistrationActivity.this)) {
                    onClickRegister();
                } else {
                    Toast.makeText(RegistrationActivity.this, "No network connection available", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Sign-up button action,
     * create user and validate fields from here
     */
    private void onClickRegister() {
        // crate user
        String username = editTextUserId.getText().toString().trim();
        registeringUser = new User("0", username);
        try {
            ActivityUtils.isValidRegistrationFields(registeringUser);
            String confirmationMessage = "<font color=#000000>Are you sure you want to register with Rahask using </font> <font color=#ffc027>" + "<b>" + registeringUser.getUsername() + "</b>" + "</font>";
            displayConfirmationMessageDialog(confirmationMessage);
        } catch (InvalidInputFieldsException e) {
            Toast.makeText(this, "Invalid username", Toast.LENGTH_LONG).show();
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
            if (!isServiceBound) {
                // init keys
                RSAUtils.initKeys(this);

                // start service from here
                Intent serviceIntent = new Intent(RegistrationActivity.this, RemoteSenzService.class);
                startService(serviceIntent);

                Log.d(TAG, "service started from reg");

                // bind to service from here as well
                Intent intent = new Intent();
                intent.setClassName("com.score.chatz", "com.score.chatz.services.RemoteSenzService");
                bindService(intent, senzServiceConnection, Context.BIND_AUTO_CREATE);
            } else {
                // start to send senz to server form here
                doRegistration();
            }
        } catch (NoSuchProviderException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create register senz
     * Send register senz to senz service via service binder
     */
    private void doRegistration() {
        try {
            // first create create senz
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());
            senzAttributes.put("pubkey", PreferenceUtils.getRsaKey(this, RSAUtils.PUBLIC_KEY));

            // new senz
            String id = "_ID";
            String signature = "";
            SenzTypeEnum senzType = SenzTypeEnum.SHARE;
            User sender = new User("", registeringUser.getUsername());
            User receiver = new User("", "senzswitch");
            Senz senz = new Senz(id, signature, senzType, sender, receiver, senzAttributes);

            senzService.send(senz);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
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
                    Toast.makeText(this, "Successfully registered", Toast.LENGTH_LONG).show();
                    // save user
                    // navigate home
                    PreferenceUtils.saveUser(this, registeringUser);
                    navigateToHome();
                } else if(msg != null && msg.equalsIgnoreCase("REG_FAIL")){
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

    /**
     * Display message dialog when user request(click) to register
     *
     * @param message message to be display
     */
    public void displayConfirmationMessageDialog(String message) {
        final Dialog dialog = new Dialog(RegistrationActivity.this);

        //set layout for dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.share_confirm_message_dialog);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(true);

        // set dialog texts
        TextView messageHeaderTextView = (TextView) dialog.findViewById(R.id.information_message_dialog_layout_message_header_text);
        TextView messageTextView = (TextView) dialog.findViewById(R.id.information_message_dialog_layout_message_text);
        messageHeaderTextView.setText("Confirm username");
        messageTextView.setText(Html.fromHtml(message));

        // set custom font
        messageHeaderTextView.setTypeface(typeface);
        messageTextView.setTypeface(typeface);

        //set ok button
        Button okButton = (Button) dialog.findViewById(R.id.information_message_dialog_layout_ok_button);
        okButton.setTypeface(typeface);
        okButton.setTypeface(null, Typeface.BOLD);
        okButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.cancel();
                ActivityUtils.showProgressDialog(RegistrationActivity.this, "Please wait...");
                doPreRegistration();
            }
        });

        // cancel button
        Button cancelButton = (Button) dialog.findViewById(R.id.information_message_dialog_layout_cancel_button);
        cancelButton.setTypeface(typeface);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        dialog.show();
    }

    /**
     * Display message dialog with registration status
     *
     * @param message message to be display
     */
    public void displayInformationMessageDialog(String title, String message) {
        final Dialog dialog = new Dialog(this);

        //set layout for dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.information_message_dialog);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(true);

        // set dialog texts
        TextView messageHeaderTextView = (TextView) dialog.findViewById(R.id.information_message_dialog_layout_message_header_text);
        TextView messageTextView = (TextView) dialog.findViewById(R.id.information_message_dialog_layout_message_text);
        messageHeaderTextView.setText(title);
        messageTextView.setText(Html.fromHtml(message));

        // set custom font
        messageHeaderTextView.setTypeface(typeface);
        messageTextView.setTypeface(typeface);

        //set ok button
        Button okButton = (Button) dialog.findViewById(R.id.information_message_dialog_layout_ok_button);
        okButton.setTypeface(typeface);
        okButton.setTypeface(null, Typeface.BOLD);
        okButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        dialog.show();
    }
}
