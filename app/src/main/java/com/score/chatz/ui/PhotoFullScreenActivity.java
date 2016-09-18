package com.score.chatz.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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
import com.score.chatz.pojo.BitmapTaskParams;
import com.score.chatz.asyncTasks.BitmapWorkerTask;
import com.score.chatz.utils.CameraUtils;
import com.score.senzc.pojos.Senz;

public class PhotoFullScreenActivity extends AppCompatActivity {

    private ImageView imageView;
    private String image;

    private View loadingText;
    private Activity activity;

    private static final int CLOSE_QUICK_VIEW_TIME = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_full_screen);
        setupUi();
        activity = this;


        Intent intent = getIntent();
        if(intent.hasExtra("IMAGE")) {
            loadingText.setVisibility(View.INVISIBLE);
            imageView.setVisibility(View.VISIBLE);
            image = intent.getStringExtra("IMAGE");
            imageView.setImageBitmap(CameraUtils.getBitmapFromBytes(image.getBytes()));
        }else{
            loadingText.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.INVISIBLE);
        }
        //image = CameraUtils.getPhotoCache(intent.getStringExtra("IMAGE_RES_ID"), getApplicationContext());

        if(intent.hasExtra("QUICK_PREVIEW")){
            startTimerToCloseView();
        }


        //imageView.setImageBitmap(image);
    }

    private void setupUi(){
        imageView = (ImageView) findViewById(R.id.imageView);
        loadingText = findViewById(R.id.loading_text);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerAllReceivers();
    }

    private void loadBitmap(String data, ImageView imageView) {
        BitmapWorkerTask task = new BitmapWorkerTask(imageView);
        //task.execute(new BitmapTaskParams(data, 100, 100));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (new BitmapTaskParams(data, 1000, 1000)));
        else
            task.execute(new BitmapTaskParams(data, 1000, 1000));
    }

    private void startTimerToCloseView(){
        final Activity _this = this;
        new CountDownTimer(CLOSE_QUICK_VIEW_TIME, CLOSE_QUICK_VIEW_TIME) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                _this.finish();
            }
        }.start();
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
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(CameraUtils.getBitmapFromBytes(senz.getAttributes().get("chatzphoto").getBytes()));
            startTimerToCloseView();
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
