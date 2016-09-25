package com.score.chatz.ui;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/**
 * Created by lakmalcaldera on 8/16/16.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = CameraPreview.class.getName();

    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private boolean isCameraBusy;

    //Constructor that obtains context and camera
    public CameraPreview(Context context, Camera camera) {
        super(context);

        this.mCamera = camera;
        this.mSurfaceHolder = this.getHolder();
        this.mSurfaceHolder.addCallback(this); // we get notified when underlying surface is created and destroyed
        this.mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); //this is a deprecated method, is not requierd after 3.0
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;

        if (sizes == null) return null;

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
        isCameraBusy = true;
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
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (mCamera != null) {
            Log.d(TAG, "Stopping preview in SurfaceDestroyed().");
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            isCameraBusy = false;
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

    public boolean isCameraBusy() {
        return isCameraBusy;
    }
}
