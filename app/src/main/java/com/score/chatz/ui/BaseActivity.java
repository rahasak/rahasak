package com.score.chatz.ui;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.score.chatz.R;
import com.score.chatz.application.IntentProvider;
import com.score.chatz.interfaces.ISendingComHandler;
import com.score.chatz.services.RemoteSenzService;
import com.score.chatz.utils.ActivityUtils;
import com.score.chatz.utils.NetworkUtil;
import com.score.senz.ISenzService;
import com.score.senzc.pojos.Senz;

/**
 * This the mother of all activities.. its contains things that are reusable by all activities.. things such as
 *  1.  popups,
 *  2.  fonts,
 *  3.  communications,
 *  4.  Common messages,
 *  5.  Initiating background service, etc...
 *
 * Created by lakmal.caldera on 9/11/2016.
 */
public class BaseActivity extends AppCompatActivity implements ISendingComHandler {

    private static final String TAG = BaseActivity.class.getName();

    // Font types used in app
    protected Typeface typeface;
    protected Typeface typefaceThin;
    protected Typeface typefaceUltraThin;

    // service interface
    protected ISenzService senzService = null;
    protected boolean isServiceBound = false;

    // service connection
    protected ServiceConnection senzServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "Connected with senz service");
            isServiceBound = true;
            senzService = ISenzService.Stub.asInterface(service);
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "Disconnected from senz service");
            senzService = null;
            isServiceBound = false;
        }
    };

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Unbind from service
        if (isServiceBound == true) unbindService(senzServiceConnection);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerAllReceivers();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterAllReceivers();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Setup Fonts!!!
        setupFonts();

        startService();
        if(!isServiceBound) {
            bindToSerice();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startService();
    }

    private void bindToSerice(){
        Intent intent = new Intent();
        intent.setClassName("com.score.chatz", "com.score.chatz.services.RemoteSenzService");
        bindService(intent, senzServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * Start long running background service
     */
    private void startService(){
        // start service from here
        Intent serviceIntent = new Intent(this, RemoteSenzService.class);
        startService(serviceIntent);

        Log.d(TAG, "Service requested to start!!!");
    }

    private void setupFonts(){
        typeface = Typeface.createFromAsset(getAssets(), "fonts/vegur_2.otf");
        typefaceThin = Typeface.createFromAsset(getAssets(), "fonts/HelveticaNeue-Light.otf");
        typefaceUltraThin = Typeface.createFromAsset(getAssets(), "fonts/HelveticaNeue-UltraLight.otf");
    }

    /**
     * Display message dialog when user request(click) to delete invoice
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

    /**
     * Generic display confirmation pop up
     * @param message - Message to ask
     * @param okClicked - instance of View.OnClickListener to handle okbutton clicked
     */
    public void displayConfirmationMessageDialog(String message, final View.OnClickListener okClicked) {
        final Dialog dialog = new Dialog(this);

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
        okButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                okClicked.onClick(v);
                dialog.cancel();
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

    private void registerAllReceivers(){
        //Notify when user don't want to responsed to requests
        this.registerReceiver(userBusyNotifier, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.USER_BUSY));
    }

    private void unregisterAllReceivers(){
        this.unregisterReceiver(userBusyNotifier);
    }

    private BroadcastReceiver userBusyNotifier = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Senz senz = intent.getExtras().getParcelable("SENZ");
            displayInformationMessageDialog( getResources().getString(R.string.sorry),  senz.getSender().getUsername() + " " + getResources().getString(R.string.is_busy_now));
        }
    };

    @Override
    public void send(Senz senz) {
        if (!NetworkUtil.isAvailableNetwork(this)) {
            ActivityUtils.showToast("No network connection available.", this);
        }else{
            try {
                if(isServiceBound == true) {
                    senzService.send(senz);
                }else{
                    ActivityUtils.showToast("Failed to connected to service.", this);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
