package com.score.chatz.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;

/**
 * Created by Lakmal on 8/27/16.
 */
public class CameraUtils {

    protected static final String TAG = CameraUtils.class.getName();
    protected static final int THUMB_NAIL_SIZE = 50;

    /**
     * Rotate bitmap to your degree
     * @param imageBitmap original bitmap image
     * @param degrees amount you want to turn
     * @return rotated bitmap
     */
    public static Bitmap getRotatedImage(Bitmap imageBitmap, int degrees){
        Bitmap rotatedBitmap = null;
        if(imageBitmap != null) {
            //Setup matrix rotate image
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);


            //Work on the bitmap
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(imageBitmap, imageBitmap.getWidth(), imageBitmap.getHeight(), true);
            rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
        }

        return rotatedBitmap;
    }

    /**
     * Rotate bitmap, and scale to width and height according to ratio
     * @param imageBitmap original bitmap image
     * @param degrees amount you want to turn
     * @return rotated bitmap
     */
    public static Bitmap getAdjustedImage(Bitmap imageBitmap, int degrees, int maxWidth){
        Bitmap rotatedBitmap = null;
        if(imageBitmap != null) {

            //Setup matrix rotate image
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);

            //Resize
            int width = imageBitmap.getWidth();
            int height = imageBitmap.getHeight();

            float scaleWidth =  width /(float) maxWidth;
            int newHeight = (int) ((int) height/ scaleWidth);


            //Work on the bitmap
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(imageBitmap, maxWidth, newHeight, true);
            rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
        }

        return rotatedBitmap;
    }

    /**
     * Resize and compress image
     * @param image original image
     * @param sizeX how small or big you want the frame to look. Original lengths are divided by this value, maintaining same ratio.
     * @param imageCompression quality of the image. 100 - best, 0 - worst
     * @return image as a byte array
     */
    public static byte[] getResizedImage(byte[] image, int sizeX, int sizeY, int imageCompression) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(image , 0, image.length);

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap resizeBitmap =  Bitmap.createScaledBitmap(bitmap, sizeX, sizeY, true);

        ByteArrayOutputStream baos= new ByteArrayOutputStream();
        resizeBitmap.compress(Bitmap.CompressFormat.JPEG, imageCompression, baos);

        return baos.toByteArray();
    }

    public static byte[] getThumnail(byte[] image){
        return getResizedImage(image, THUMB_NAIL_SIZE, THUMB_NAIL_SIZE, 50);
    }

    /**
     * Maintain same hight and width
     * compress image to threshold kbs
     * @param threshold
     * @return
     */
    public static byte[] getCompressedImage(byte[] imageArray, int threshold){
        int kbs = imageArray.length/1024;
        int thresholdCompression = (100 * threshold) / kbs;

        //If the required compression is greater than what is supplied then take full  quality
        thresholdCompression = thresholdCompression > 100 ? 100 : thresholdCompression;

        Bitmap bitmap = BitmapFactory.decodeByteArray(imageArray , 0, imageArray.length);
        ByteArrayOutputStream baos= new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, thresholdCompression, baos);

        return baos.toByteArray();
    }

    /**
     * utility method to get bytes from Bitmap
     * @param image
     * @return
     */
    public static byte[] getBytesFromImage(Bitmap image) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        byte[] byteArray = stream.toByteArray();
        return byteArray;
    }

    /**
     * utility method to get Bitmap from bytes
     * @param byteArray
     * @return
     */
    public static Bitmap getBitmapFromBytes(byte[] byteArray) {
        Log.i(TAG, "IMAGE BYTE ARRAY - " + byteArray.toString());
        byte[] imageAsBytes = Base64.decode(byteArray, Base64.DEFAULT);
        Bitmap imgBitmap= BitmapFactory.decodeByteArray(imageAsBytes,0,imageAsBytes.length);

        //Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray , 0, byteArray.length);
        return imgBitmap;
    }

    public static String encodeToBase64(Bitmap image, Bitmap.CompressFormat compressFormat, int quality)
    {
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        image.compress(compressFormat, quality, byteArrayOS);
        return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.DEFAULT);
    }

    public static Bitmap decodeBase64(String input)
    {
        byte[] decodedBytes = Base64.decode(input, 0);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    public static String resizeBase64Image(String base64image){
        byte [] encodeByte=Base64.decode(base64image.getBytes(),Base64.DEFAULT);
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inPurgeable = true;
        Bitmap image = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length,options);

        image = Bitmap.createScaledBitmap(image, THUMB_NAIL_SIZE, THUMB_NAIL_SIZE, false);

        ByteArrayOutputStream baos=new  ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG,100, baos);

        byte [] b=baos.toByteArray();
        System.gc();
        return Base64.encodeToString(b, Base64.NO_WRAP);

    }

    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeBitmapFromByteArray(byte[] data, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int maxWidth) {

        int width = bm.getWidth();
        int height = bm.getHeight();

        float scaleWidth = ((float) maxWidth) / width;
        float scaleHeight = ((float) height) * ((float) maxWidth) / (float) width;

        // create a matrix for the manipulation
        Matrix matrix = new Matrix();

        // resize the bit map
        matrix.postScale(scaleWidth, scaleHeight);

        // recreate the new Bitmap
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);

        return resizedBitmap;

    }

    public static boolean isCameraFrontAvailable(Context context) {
        if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            return true;
        } else {
            return false;
        }
    }

    public static void shootSound(Context context)
    {
        AudioManager meng = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int volume = meng.getStreamVolume( AudioManager.STREAM_NOTIFICATION);
        MediaPlayer _shootMP = null;
        if (volume != 0)
        {
            if (_shootMP == null)
                _shootMP = MediaPlayer.create(context, Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
                _shootMP.start();
        }
    }
}
