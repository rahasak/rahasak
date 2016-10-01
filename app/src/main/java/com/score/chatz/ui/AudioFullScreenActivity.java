package com.score.chatz.ui;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.score.chatz.R;
import com.score.chatz.application.IntentProvider;
import com.score.chatz.asyncTasks.RahasPlayer;
import com.score.chatz.interfaces.IRahasPlayListener;
import com.score.senzc.pojos.Senz;

public class AudioFullScreenActivity extends AppCompatActivity implements IRahasPlayListener {

    private static final String TAG = AudioFullScreenActivity.class.getName();

    private View playingText;
    private View loadingText;

    private Typeface typeface;

    // senz message
    private BroadcastReceiver senzReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got message from Senz service");

            // extract senz
            Senz senz = intent.getExtras().getParcelable("SENZ");
            switch (senz.getSenzType()) {
                case DATA:
                    onSenzReceived(senz);
                    break;
                case STREAM:
                    onSenzStreamReceived(senz);
                    break;
                default:
                    break;
            }
        }
    };

    private void onSenzStreamReceived(Senz senz) {
        if (senz.getAttributes().containsKey("mic")) {
            // display stream
            loadingText.setVisibility(View.INVISIBLE);
            playingText.setVisibility(View.VISIBLE);
            new RahasPlayer(Base64.decode(senz.getAttributes().get("mic"), 0), getApplicationContext(), this).execute("Rahsa");
        }
    }

    private void onSenzReceived(Senz senz) {
        if (senz.getAttributes().containsKey("status")) {
            if (senz.getAttributes().get("status").equalsIgnoreCase("801")) {
                // user busy
                displayInformationMessageDialog("info", "user busy");
            } else if (senz.getAttributes().get("status").equalsIgnoreCase("802")) {
                // camera error
                displayInformationMessageDialog("error", "cam error");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_full_screen);

        setupUi();
        initIntent();
        setUpFonts();
    }

    private void setUpFonts(){
        typeface = Typeface.createFromAsset(getAssets(), "fonts/GeosansLight.ttf");
    }

    private void setupUi() {
        playingText = findViewById(R.id.playingText);
        loadingText = findViewById(R.id.loading_text);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(senzReceiver, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.SENZ));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(senzReceiver);
    }

    private void initIntent() {
        Intent intent = getIntent();
        if (intent.hasExtra("SOUND")) {
            loadingText.setVisibility(View.INVISIBLE);
            playingText.setVisibility(View.VISIBLE);
            new RahasPlayer(Base64.decode(intent.getStringExtra("SOUND"), 0), getApplicationContext(), this).execute("Rahsa");
        } else {
            loadingText.setVisibility(View.VISIBLE);
            playingText.setVisibility(View.INVISIBLE);
        }
    }

    public void displayInformationMessageDialog(String title, String message) {
        final Dialog dialog = new Dialog(this);

        //set layout for dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.information_message_dialog);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(true);

        // set dialog texts
        TextView messageHeaderTextView = (TextView) dialog.findViewById(R.id.information_message_dialog_layout_message_header_text);
        TextView messageTextView = (TextView) dialog.findViewById(R.id.information_message_dialog_layout_message_text);
        messageHeaderTextView.setText(title);
        messageTextView.setText(Html.fromHtml(message));

        // set custom font
        messageHeaderTextView.setTypeface(typeface, Typeface.BOLD);
        messageTextView.setTypeface(typeface);

        //set ok button
        Button okButton = (Button) dialog.findViewById(R.id.information_message_dialog_layout_ok_button);
        okButton.setTypeface(typeface, Typeface.BOLD);
        okButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.cancel();
                AudioFullScreenActivity.this.finish();
            }
        });

        dialog.show();
    }

    @Override
    public void onFinishPlay() {
        this.finish();
    }
}