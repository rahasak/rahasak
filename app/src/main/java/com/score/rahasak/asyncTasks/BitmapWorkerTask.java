package com.score.rahasak.asyncTasks;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.score.rahasak.pojo.BitmapTaskParams;
import com.score.rahasak.utils.ImageUtils;

import java.lang.ref.WeakReference;

/**
 * Created by Lakmal on 8/29/16.
 */
public class BitmapWorkerTask extends AsyncTask<BitmapTaskParams, Void, Bitmap> {
    private final WeakReference<ImageView> imageViewReference;

    public BitmapWorkerTask(ImageView imageView) {
        // Use a WeakReference to ensure the ImageView can be garbage collected
        imageViewReference = new WeakReference<>(imageView);
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(BitmapTaskParams... params) {
        String encodedBitmap = params[0].getData();
        return new ImageUtils().decodeBitmap(encodedBitmap);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (bitmap != null) {
            final ImageView imageView = imageViewReference.get();
            if (imageView != null) {
                imageView.setImageBitmap(bitmap);
                imageView.invalidate();
            }
        }
    }
}


