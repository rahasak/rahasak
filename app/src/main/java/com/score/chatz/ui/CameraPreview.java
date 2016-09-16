package com.score.chatz.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.score.chatz.R;
import com.score.chatz.handlers.SenzPhotoHandlerReceiving;
import com.score.chatz.pojo.SenzStream;
import com.score.chatz.utils.CameraUtils;
import com.score.senzc.pojos.Senz;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by lakmalcaldera on 8/16/16.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = CameraPreview.class.getName();

    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private SenzStream.SENZ_STEAM_TYPE streamType;

    private boolean isCameraBusy;

    private static CameraPreview instance;

    //Constructor that obtains context and camera
    public CameraPreview(Context _context, Camera camera, SenzStream.SENZ_STEAM_TYPE streamType) {
        super(_context);

        this.mCamera = camera;
        this.mSurfaceHolder = this.getHolder();
        this.mSurfaceHolder.addCallback(this); // we get notified when underlying surface is created and destroyed
        this.mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); //this is a deprecated method, is not requierd after 3.0
        this.streamType = streamType;
        instance = this;
    }

    public static CameraPreview getSingleton(Context _context, Camera camera, SenzStream.SENZ_STEAM_TYPE streamType){
        if(instance == null){
          return new CameraPreview(_context,  camera,  streamType);
        }else{
            return instance;
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w/h;

        if (sizes==null) return null;

        Camera.Size optimalSize = null;

        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Find size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        /*if (mCamera != null) {
            isCameraBusy = true;
            mCamera.release();
        }else {*/
        isCameraBusy =true;
            mCamera = getCameraInstant();
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
            parameters.setPreviewSize(getOptimalPreviewSize(sizes, 800, 600).width, getOptimalPreviewSize(sizes, 800, 600).height);
            mCamera.setParameters(parameters);
            try {
                mCamera.setPreviewDisplay(surfaceHolder);
                mCamera.setDisplayOrientation(90);

                mCamera.startPreview();
            } catch (IOException e) {
                // left blank for now
                Log.d(TAG, "Error setting camera preview: " + e.getMessage());

            }
        //}
    }

    public void takePhoto(final PhotoActivity activity, final Senz originalSenz) {
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {

                byte[] resizedImage = null;
                bytes = resizeBitmapByteArray(bytes, 90);
                if (streamType == SenzStream.SENZ_STEAM_TYPE.CHATZPHOTO) {
                    //Scaled down image
                    resizedImage = CameraUtils.getCompressedImage(bytes, 110); //Compress image ~ 5kbs
                } else if (streamType == SenzStream.SENZ_STEAM_TYPE.PROFILEZPHOTO) {
                    resizedImage = CameraUtils.getCompressedImage(bytes, 110); //Compress image ~ 50kbs
                }

                SenzPhotoHandlerReceiving.getInstance().sendPhoto(resizedImage, originalSenz, getContext());

                activity.finish();
            }
        });
    }

    public void takePhotoManually(final PhotoActivity activity, final Senz originalSenz) {
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {

                byte[] resizedImage = null;
                bytes = resizeBitmapByteArray(bytes, 90);
                if (streamType == SenzStream.SENZ_STEAM_TYPE.CHATZPHOTO) {
                    //Scaled down image
                    resizedImage = CameraUtils.getCompressedImage(bytes, 110); //Compress image ~ 5kbs
                } else if (streamType == SenzStream.SENZ_STEAM_TYPE.PROFILEZPHOTO) {
                    resizedImage = CameraUtils.getCompressedImage(bytes, 110); //Compress image ~ 50kbs
                }

                isCameraBusy = false;

                SenzPhotoHandlerReceiving.getInstance().sendPhoto(resizedImage, originalSenz, getContext());
                Intent i = new Intent(activity, PhotoFullScreenActivity.class);
                i.putExtra("IMAGE", Base64.encodeToString(resizedImage, 0));
                i.putExtra("QUICK_PREVIEW", "true");
                activity.startActivity(i);
                activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                activity.finish();
            }
        });
    }

    private byte[] resizeBitmapByteArray(byte[] data, int deg) {
        Bitmap decodedBitmap = CameraUtils.decodeBase64(Base64.encodeToString(data, 0));
        // set max width ~ 600px
        Bitmap rotatedBitmap = CameraUtils.getAdjustedImage(decodedBitmap, deg, 1000);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (mCamera != null) {
            Log.d(TAG, "Stopping preview in SurfaceDestroyed().");
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            isCameraBusy =false;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format,
                               int width, int height) {
        if (mSurfaceHolder.getSurface() == null) {
            //preview surface does not exist
            return;
        }
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            //ignore: tried to stop a non-existent preview
        }

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.setDisplayOrientation(90);
            mCamera.startPreview();
        } catch (Exception e) {
            // intentionally left blank for a test
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    /**
     * Helper method to access the camera returns null if
     * it cannot get the camera or does not exist
     *
     * @return
     */
    private Camera getCameraInstant() {
        Camera camera = null;
        try {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        } catch (Exception e) {
            // cannot get camera or does not exist
        }
        return camera;
    }

    public boolean isCameraBusy(){
        return isCameraBusy;
    }
}
