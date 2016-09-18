package com.score.chatz.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.score.chatz.R;
import com.score.chatz.application.IntentProvider;
import com.score.chatz.asyncTasks.BitmapWorkerTask;
import com.score.chatz.pojo.BitmapTaskParams;
import com.score.chatz.utils.CameraUtils;
import com.score.senzc.pojos.Senz;

public class AudioFullScreenActivity extends AppCompatActivity {
    private TextView playingText;

    private View loadingText;
    private Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_full_screen);
        setupUi();
        activity = this;

        Intent intent = getIntent();
        if(intent.hasExtra("IMAGE")) {
            loadingText.setVisibility(View.INVISIBLE);
            playingText.setVisibility(View.VISIBLE);
        }else{
            loadingText.setVisibility(View.VISIBLE);
            playingText.setVisibility(View.INVISIBLE);
        }
    }

    private void setupUi(){
        playingText = (TextView) findViewById(R.id.playingText);
        loadingText = findViewById(R.id.loading_text);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerAllReceivers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterAllReceivers();
    }

    private void registerAllReceivers(){
        //Notify when user don't want to responsed to requests
        this.registerReceiver(newDataToDisplay, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.NEW_DATA_TO_DISPLAY));
        this.registerReceiver(userBusyNotifier, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.USER_BUSY));
    }

    private void unregisterAllReceivers(){
        this.unregisterReceiver(newDataToDisplay);
        this.unregisterReceiver(userBusyNotifier);
    }

    private BroadcastReceiver newDataToDisplay = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Senz senz = intent.getExtras().getParcelable("SENZ");
            loadingText.setVisibility(View.INVISIBLE);
            playingText.setVisibility(View.VISIBLE);
        }
    };

    private BroadcastReceiver userBusyNotifier = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Senz senz = intent.getExtras().getParcelable("SENZ");
            displayInformationMessageDialog( getResources().getString(R.string.sorry),  senz.getSender().getUsername() + " " + getResources().getString(R.string.is_busy_now));
        }
    };

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
        messageHeaderTextView.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/vegur_2.otf"));
        messageTextView.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/vegur_2.otf"));

        //set ok button
        Button okButton = (Button) dialog.findViewById(R.id.information_message_dialog_layout_ok_button);
        okButton.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/vegur_2.otf"));
        okButton.setTypeface(null, Typeface.BOLD);
        okButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.cancel();
                activity.finish();
            }
        });

        dialog.show();
    }
}