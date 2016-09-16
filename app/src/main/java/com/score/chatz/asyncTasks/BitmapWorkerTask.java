package com.score.chatz.asyncTasks;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Base64;
import android.widget.ImageView;

import com.score.chatz.pojo.BitmapTaskParams;
import com.score.chatz.utils.CameraUtils;

import java.lang.ref.WeakReference;

/**
 * Created by Lakmal on 8/29/16.
 */
public class BitmapWorkerTask extends AsyncTask<BitmapTaskParams, Void, Bitmap> {
    private final WeakReference<ImageView> imageViewReference;

    public BitmapWorkerTask(ImageView imageView) {
        // Use a WeakReference to ensure the ImageView can be garbage collected
        imageViewReference = new WeakReference<ImageView>(imageView);
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(BitmapTaskParams... params) {
        return CameraUtils.decodeBitmapFromByteArray(rotateImage(params[0].getData(), -90), params[0].getWidth(), params[0].getHeight());
    }

    private byte[] rotateImage(String base64EncodedString, int degrees){

        return Base64.decode(base64EncodedString, 0);
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (imageViewReference != null && bitmap != null) {
            final ImageView imageView = imageViewReference.get();
            if (imageView != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
}


