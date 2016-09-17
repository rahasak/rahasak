package com.score.chatz.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.score.chatz.R;
import com.score.chatz.pojo.BitmapTaskParams;
import com.score.chatz.asyncTasks.BitmapWorkerTask;
import com.score.chatz.utils.CameraUtils;

public class PhotoFullScreenActivity extends AppCompatActivity {

    private ImageView imageView;
    private Bitmap image;

    private static final int CLOSE_QUICK_VIEW_TIME = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_full_screen);

        Intent intent = getIntent();
        //image = intent.getStringExtra("IMAGE");
        image = CameraUtils.getPhotoCache(intent.getStringExtra("IMAGE_RES_ID"), getApplicationContext());

        if(intent.hasExtra("QUICK_PREVIEW")){
            startTimerToCloseView();
        }

        imageView = (ImageView) findViewById(R.id.imageView);
        //loadBitmap(image, imageView);
        imageView.setImageBitmap(image);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //If the activity is destroy on rotation
        imageView.setImageBitmap(image);
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

}
