package com.score.chatz.ui;

import android.app.ActionBar;
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
import android.support.v7.widget.Toolbar;
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
import com.score.chatz.services.RemoteSenzService;
import com.score.chatz.utils.ActivityUtils;
import com.score.chatz.utils.NetworkUtil;
import com.score.chatz.utils.NotificationUtils;
import com.score.chatz.utils.RSAUtils;
import com.score.senz.ISenzService;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;

public class AddUserActivity extends AppCompatActivity {

    private static final String TAG = AddUserActivity.class.getName();
    private TextView headerTitle;
    private ImageView backBtn;
    private TextView invite_text_part_1;
    private TextView invite_text_part_2;
    private TextView invite_text_part_3;
    private TextView invite_text_part_4;
    private TextView invite_text_part_5;
    private TextView invite_text_part_6;
    private Button addFriendBtn;
    private EditText editTextUserId;
    private User registeringUser;
    protected Typeface typeface;
    protected Typeface typefaceThin;
    protected Typeface typefaceUltraThin;
    private  boolean isServiceBound;



    // service interface
    private ISenzService senzService = null;

    // service connection
    private ServiceConnection senzServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d("TAG", "Connected with senz service");
            senzService = ISenzService.Stub.asInterface(service);
            isServiceBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            senzService = null;
            isServiceBound = false;
            Log.d("TAG", "Disconnected from senz service");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_user_activity);
        getSupportActionBar().setCustomView(R.layout.add_user_action_bar);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayOptions(android.support.v7.app.ActionBar.DISPLAY_SHOW_CUSTOM);
        headerTitle = (TextView) findViewById(R.id.header_center_text);
        getSupportActionBar().setDisplayShowCustomEnabled(true);

        Toolbar toolbar=(Toolbar) getSupportActionBar().getCustomView().getParent();
        toolbar.setContentInsetsAbsolute(0, 0);
        toolbar.getContentInsetEnd();
        toolbar.setPadding(0, 0, 0, 0);


        /*
         * Main text on the page is broken down into parts for customization.
         */
        invite_text_part_1 = (TextView) findViewById(R.id.textView);
        invite_text_part_2 = (TextView) findViewById(R.id.textView2);
        invite_text_part_3 = (TextView) findViewById(R.id.textView3);
        invite_text_part_4 = (TextView) findViewById(R.id.textView4);
        invite_text_part_5 = (TextView) findViewById(R.id.textView5);
        invite_text_part_6 = (TextView) findViewById(R.id.textView6);
        invite_text_part_6 = (TextView) findViewById(R.id.textView6);


        editTextUserId = (EditText) findViewById(R.id.friend_id);

        setupFonts();
        setupBackBtn();
        setupAddUsersBtn();


    }



    private BroadcastReceiver senzDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got message from Senz service");
            handleMessage(intent);
        }
    };


    @Override
    protected void onStart() {
        super.onStart();
        // bind with senz service
        // bind to service from here as well

    }


    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent();
        intent.setClassName("com.score.chatz", "com.score.chatz.services.RemoteSenzService");
        bindService(intent, senzServiceConnection, Context.BIND_AUTO_CREATE);

        this.registerReceiver(senzDataReceiver, new IntentFilter("com.score.chatz.DATA_SENZ")); //Incoming data share
        //this.registerReceiver(senzMessageReceiver, new IntentFilter("com.score.chatz.SENZ_SHARE")); //Incoming senz share
        //registerReceiver(senzShareReceiver, new IntentFilter("com.score.chatz.SENZ_SHARE"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop() {
        super.onStop();

        // Unbind from the service
        this.unbindService(senzServiceConnection);
        //if (senzShareReceiver != null) unregisterReceiver(senzShareReceiver);
        if (senzDataReceiver != null) unregisterReceiver(senzDataReceiver);
        //if (senzMessageReceiver != null) unregisterReceiver(senzMessageReceiver);
    }


    private void setupAddUsersBtn(){
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

    private void onClickShare(){
        String username = editTextUserId.getText().toString().trim();
        registeringUser = new User("0", username);
        try {
            ActivityUtils.isValidRegistrationFields(registeringUser);
            String confirmationMessage = "<font color=#000000>Are you sure you want to register on SenZ with </font> <font color=#ffc027>" + "<b>" + registeringUser.getUsername() + "</b>" + "</font>";
            displayConfirmationMessageDialog(confirmationMessage);
        } catch (InvalidInputFieldsException e) {
            Toast.makeText(this, "Invalid username", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void setupFonts(){
        typeface = Typeface.createFromAsset(getAssets(), "fonts/vegur_2.otf");
        typefaceThin = Typeface.createFromAsset(getAssets(), "fonts/HelveticaNeue-Light.otf");
        typefaceUltraThin = Typeface.createFromAsset(getAssets(), "fonts/HelveticaNeue-UltraLight.otf");
        headerTitle.setTypeface(typefaceThin, Typeface.NORMAL);
        invite_text_part_1.setTypeface(typefaceUltraThin, Typeface.NORMAL);
        invite_text_part_2.setTypeface(typefaceUltraThin, Typeface.NORMAL);
        invite_text_part_3.setTypeface(typefaceUltraThin, Typeface.NORMAL);
        invite_text_part_4.setTypeface(typefaceUltraThin, Typeface.NORMAL);
        invite_text_part_5.setTypeface(typefaceUltraThin, Typeface.NORMAL);
        invite_text_part_6.setTypeface(typefaceUltraThin, Typeface.NORMAL);
    }




    /**
     * Share current sensor
     * Need to send share query to server via web socket
     */
    private void share() {


        if(isServiceBound == true) {
            try {
                // create senz attributes
                HashMap<String, String> senzAttributes = new HashMap<>();
                senzAttributes.put("lat", "lat");
                senzAttributes.put("lon", "lon");
                senzAttributes.put("msg", "msg");
                senzAttributes.put("chatzphoto", "chatzphoto");
                senzAttributes.put("chatzmsg", "chatzmsg");
                senzAttributes.put("camPerm", "false"); //Default Values, later in ui allow user to configure this on share
                senzAttributes.put("locPerm", "false"); //Dafault Values
                senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());

                // new senz
                String id = "_ID";
                String signature = "_SIGNATURE";
                SenzTypeEnum senzType = SenzTypeEnum.SHARE;
                User receiver = new User("", editTextUserId.getText().toString().trim());
                Senz senz = new Senz(id, signature, senzType, null, receiver, senzAttributes);

                senzService.send(senz);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }else{
            Toast.makeText(getApplicationContext(), "Establishing connection to server. Please wait.", Toast.LENGTH_LONG).show();
        }
    }



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
                    String message = "<font color=#000000>Seems we couldn't share the senz with </font> <font color=#eada00>" + "<b>" + user + "</b>" + "</font>";
                    displayInformationMessageDialog("#Share Fail", message);
                }
            }
        }
    }


    /**
     * Clear input fields and reset activity components
     */
    private void onPostShare(Senz senz) {
        // Create user with senz sender(he is a friend)
        //SenzorsDbSource dbSource = new SenzorsDbSource(this);
        //dbSource.getOrCreateUser(senz.getSender().getUsername());

        editTextUserId.setText("");
        this.goBackToHome();
        Toast.makeText(this, "Successfully shared SenZ", Toast.LENGTH_LONG).show();
    }




    private void goBackToHome(){
        Log.d(TAG, "go home clicked");
        this.finish();
    }

    private void setupBackBtn() {
        backBtn = (ImageView) findViewById(R.id.goBackToHomeImg);
        backBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                goBackToHome();
            }
        });
    }





    /**
     * Display message dialog when user request(click) to register
     *
     * @param message message to be display
     */
    public void displayConfirmationMessageDialog(String message) {
        final Dialog dialog = new Dialog(AddUserActivity.this);

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
                if (NetworkUtil.isAvailableNetwork(AddUserActivity.this)) {
                    dialog.cancel();
                    ActivityUtils.showProgressDialog(AddUserActivity.this, "Please wait...");
                    share();
                } else {
                    Toast.makeText(AddUserActivity.this, "No network connection available", Toast.LENGTH_LONG).show();
                }
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
