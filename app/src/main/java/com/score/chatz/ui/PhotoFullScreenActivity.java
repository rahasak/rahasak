package com.score.chatz.ui;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
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

import com.score.chatz.R;
import com.score.chatz.application.IntentProvider;
import com.score.chatz.asyncTasks.BitmapWorkerTask;
import com.score.chatz.pojo.BitmapTaskParams;
import com.score.chatz.utils.CameraUtils;
import com.score.senzc.pojos.Senz;

public class PhotoFullScreenActivity extends AppCompatActivity {

    private static final String TAG = PhotoFullScreenActivity.class.getName();

    private ImageView imageView;
    private View loadingText;

    private String imageData;

    private static final int CLOSE_QUICK_VIEW_TIME = 2000;

    // senz message
    private BroadcastReceiver senzReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got message from Senz service");

            // extract senz
            Senz senz = intent.getExtras().getParcelable("SENZ");
            onSenzReceived(senz);
        }
    };

    // share senz
    private BroadcastReceiver senzStreamReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Share senz");

            Senz senz = intent.getExtras().getParcelable("SENZ");
            onSenzStreamReceived(senz);
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
        imageView = (ImageView) findViewById(R.id.imageView);
        loadingText = findViewById(R.id.loading_text);
    }

    private void initIntent() {
        Intent intent = getIntent();
        if (intent.hasExtra("IMAGE")) {
            loadingText.setVisibility(View.INVISIBLE);
            imageView.setVisibility(View.VISIBLE);
            imageData = intent.getStringExtra("IMAGE");
            imageView.setImageBitmap(CameraUtils.getBitmapFromBytes(imageData.getBytes()));
        } else {
            loadingText.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.INVISIBLE);
        }

        if (intent.hasExtra("QUICK_PREVIEW")) {
            startCloseViewTimer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(senzReceiver, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.DATA_SENZ));
        registerReceiver(senzStreamReceiver, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.STREAM_SENZ));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(senzStreamReceiver);
        unregisterReceiver(senzReceiver);
    }

    private void loadBitmap(String data, ImageView imageView) {
        BitmapWorkerTask task = new BitmapWorkerTask(imageView);
        //task.execute(new BitmapTaskParams(data, 100, 100));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (new BitmapTaskParams(data, 1000, 1000)));
        else
            task.execute(new BitmapTaskParams(data, 1000, 1000));
    }

    private void startCloseViewTimer() {
        new CountDownTimer(CLOSE_QUICK_VIEW_TIME, CLOSE_QUICK_VIEW_TIME) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                PhotoFullScreenActivity.this.finish();
            }
        }.start();
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

    private void onSenzStreamReceived(Senz senz) {
        if (senz.getAttributes().containsKey("cam")) {
            // display stream
            loadingText.setVisibility(View.INVISIBLE);
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(CameraUtils.getBitmapFromBytes(senz.getAttributes().get("cam").getBytes()));
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
        messageHeaderTextView.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/vegur_2.otf"));
        messageTextView.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/vegur_2.otf"));

        //set ok button
        Button okButton = (Button) dialog.findViewById(R.id.information_message_dialog_layout_ok_button);
        okButton.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/vegur_2.otf"));
        okButton.setTypeface(null, Typeface.BOLD);
        okButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.cancel();
                PhotoFullScreenActivity.this.finish();
            }
        });

        dialog.show();
    }
}
