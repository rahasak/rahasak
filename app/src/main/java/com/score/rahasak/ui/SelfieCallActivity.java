package com.score.rahasak.ui;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.score.rahasak.R;
import com.score.rahasak.application.IntentProvider;
import com.score.rahasak.enums.IntentType;
import com.score.rahasak.utils.ImageUtils;
import com.score.senzc.pojos.Senz;

public class SelfieCallActivity extends AppCompatActivity {

    private static final String TAG = SelfieCallActivity.class.getName();

    private View loadingView;
    private TextView callingText;
    private TextView usernameText;
    private ImageView imageView;
    private String imageData;
    private Typeface typeface;
    private ImageView waitingIcon;

    private static final int CLOSE_QUICK_VIEW_TIME = 2000;

    // senz message
    private BroadcastReceiver senzReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got message from Senz service");

            // extract senz
            if (intent.hasExtra("SENZ")) {
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
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_full_screen);

        initUi();
        initIntent();
    }

    private void initUi() {
        typeface = Typeface.createFromAsset(getAssets(), "fonts/GeosansLight.ttf");

        waitingIcon = (ImageView) findViewById(R.id.selfie_image);
        imageView = (ImageView) findViewById(R.id.imageView);
        loadingView = findViewById(R.id.selfie_loading_view);
        callingText = (TextView) findViewById(R.id.selfie_calling_text);
        usernameText = (TextView) findViewById(R.id.selfie_username);

        callingText.setTypeface(typeface, Typeface.NORMAL);
        usernameText.setTypeface(typeface, Typeface.BOLD);
    }

    private void initIntent() {
        Intent intent = getIntent();
        if (intent.hasExtra("IMAGE")) {
            loadingView.setVisibility(View.INVISIBLE);
            imageView.setVisibility(View.VISIBLE);
            imageData = intent.getStringExtra("IMAGE");
            imageView.setImageBitmap(new ImageUtils().decodeBitmap(imageData));
        } else {
            loadingView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.INVISIBLE);

            String sender = intent.getStringExtra("SENDER");
            usernameText.setText("@" + sender);

            startAnimatingWaitingIcon();
        }

        if (intent.hasExtra("QUICK_PREVIEW")) {
            startCloseViewTimer();
        }
    }

    private void startAnimatingWaitingIcon() {
        AnimationDrawable anim = (AnimationDrawable) waitingIcon.getBackground();
        anim.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(senzReceiver, IntentProvider.getIntentFilter(IntentType.SENZ));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(senzReceiver);
    }

    private void startCloseViewTimer() {
        new CountDownTimer(CLOSE_QUICK_VIEW_TIME, CLOSE_QUICK_VIEW_TIME) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                SelfieCallActivity.this.finish();
            }
        }.start();
    }

    private void onSenzReceived(Senz senz) {
        if (senz.getAttributes().containsKey("status")) {
            if (senz.getAttributes().get("status").equalsIgnoreCase("DELIVERED")) {
                // GET msg delivered
            } else if (senz.getAttributes().get("status").equalsIgnoreCase("CAM_BUSY")) {
                // user busy
                displayInformationMessageDialog("BUSY", "User busy at this moment");
            } else if (senz.getAttributes().get("status").equalsIgnoreCase("CAM_ERROR")) {
                // camera error
                displayInformationMessageDialog("ERROR", "Cam error");
            } else if (senz.getAttributes().get("status").equalsIgnoreCase("OFFLINE")) {
                // offline
                // offline
                displayInformationMessageDialog("OFFLINE", "User not available at this moment");
            }
        }
    }

    private void onSenzStreamReceived(Senz senz) {
        if (senz.getAttributes().containsKey("cam")) {
            // display stream
            loadingView.setVisibility(View.INVISIBLE);
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(new ImageUtils().decodeBitmap(senz.getAttributes().get("cam")));
            startCloseViewTimer();
        }
    }

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
        messageHeaderTextView.setTypeface(typeface, Typeface.BOLD);
        messageTextView.setTypeface(typeface);

        //set ok button
        Button okButton = (Button) dialog.findViewById(R.id.information_message_dialog_layout_ok_button);
        okButton.setTypeface(typeface, Typeface.BOLD);
        okButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.cancel();
                SelfieCallActivity.this.finish();
            }
        });

        dialog.show();
    }
}
