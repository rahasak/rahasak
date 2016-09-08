package com.score.chatz.ui;

import android.content.Intent;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.score.chatz.R;
import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.utils.BitmapTaskParams;
import com.score.chatz.utils.BitmapWorkerTask;
import com.score.chatz.utils.CameraUtils;
import com.score.chatz.utils.PreferenceUtils;

public class PhotoFullScreenActivity extends AppCompatActivity {

    ImageView imageView;
    String image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_full_screen);

        Intent intent = getIntent();
        image = intent.getStringExtra("IMAGE");

        imageView = (ImageView) findViewById(R.id.imageView);
        //loadBitmap(image, imageView);
        imageView.setImageBitmap(CameraUtils.getBitmapFromBytes(image.getBytes()));
    }

    @Override
    protected void onResume() {
        super.onResume();

        //If the activity is destroy on rotation
        imageView.setImageBitmap(CameraUtils.getBitmapFromBytes(image.getBytes()));
    }

    private void loadBitmap(String data, ImageView imageView) {
        BitmapWorkerTask task = new BitmapWorkerTask(imageView);
        //task.execute(new BitmapTaskParams(data, 100, 100));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (new BitmapTaskParams(data, 1000, 1000)));
        else
            task.execute(new BitmapTaskParams(data, 1000, 1000));
    }

}
