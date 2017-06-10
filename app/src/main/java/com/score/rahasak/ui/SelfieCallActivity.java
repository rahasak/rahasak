package com.score.rahasak.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.score.rahasak.R;
import com.score.rahasak.application.IntentProvider;
import com.score.rahasak.enums.IntentType;
import com.score.senzc.pojos.Senz;
import com.squareup.picasso.Picasso;

import java.io.File;

public class SelfieCallActivity extends AppCompatActivity {

    private static final String TAG = SelfieCallActivity.class.getName();

    private View loadingView;
    private TextView callingText;
    private TextView usernameText;
    private ImageView imageView;
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
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.selfie_call_activity_layout);

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
        if (intent.hasExtra("UID")) {
            loadingView.setVisibility(View.INVISIBLE);
            imageView.setVisibility(View.VISIBLE);

            String uid = intent.getStringExtra("UID");
            loadBitmap(imageView, uid);
        } else {
            loadingView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.INVISIBLE);

            String sender = intent.getStringExtra("SENDER");
            usernameText.setText(sender);

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
            if (senz.getAttributes().get("status").equalsIgnoreCase("CAM_BUSY")) {
                // user busy
                Toast.makeText(this, "User busy", Toast.LENGTH_LONG).show();
                SelfieCallActivity.this.finish();
            } else if (senz.getAttributes().get("status").equalsIgnoreCase("CAM_ERROR")) {
                // camera error
                Toast.makeText(this, "Busy", Toast.LENGTH_LONG).show();
                SelfieCallActivity.this.finish();
            } else if (senz.getAttributes().get("status").equalsIgnoreCase("OFFLINE")) {
                // offline
                Toast.makeText(this, "User offline", Toast.LENGTH_LONG).show();
                SelfieCallActivity.this.finish();
            }
        }
    }

    private void onSenzStreamReceived(Senz senz) {
        if (senz.getAttributes().containsKey("cam")) {
            // display stream
            loadingView.setVisibility(View.INVISIBLE);
            imageView.setVisibility(View.VISIBLE);
            loadBitmap(imageView, senz.getAttributes().get("uid"));

            startCloseViewTimer();
        }
    }

    private void loadBitmap(ImageView view, String uid) {
        // load image via picasso
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/Rahasak/" + uid + ".jpg");
        Picasso.with(this)
                .load(file)
                .error(R.drawable.rahaslogo_3)
                .into(view);
    }
}
