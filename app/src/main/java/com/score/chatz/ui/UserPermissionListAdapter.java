package com.score.chatz.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.github.siyamed.shapeimageview.CircularImageView;

import com.score.chatz.R;
import com.score.chatz.pojo.UserPermission;
import com.score.chatz.utils.BitmapTaskParams;
import com.score.chatz.utils.BitmapWorkerTask;
import com.score.chatz.utils.CameraUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

/**
 * Created by Lakmal on 8/6/16.
 */
    class UserPermissionListAdapter extends ArrayAdapter<UserPermission> {
        Context context;
        ArrayList<UserPermission> userPermissionList;

        public UserPermissionListAdapter(Context _context, ArrayList<UserPermission> userPermsList) {
            super(_context, R.layout.single_user_card_row, R.id.user_name, userPermsList);
            context = _context;
            userPermissionList = userPermsList;
        }


        /**
         * Create list row view
         *
         * @param i         index
         * @param view      current list item view
         * @param viewGroup parent
         * @return view
         */
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            // A ViewHolder keeps references to children views to avoid unnecessary calls
            // to findViewById() on each row.
            final ViewHolder holder;

            final UserPermission userPerm = (UserPermission) getItem(i);

            if (view == null) {
                //inflate sensor list row layout
                LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = layoutInflater.inflate(R.layout.single_user_card_row, viewGroup, false);

                //create view holder to store reference to child views
                holder = new ViewHolder();
                holder.userImageView = (CircularImageView) view.findViewById(R.id.user_image);
                holder.usernameView = (TextView) view.findViewById(R.id.user_name);
                holder.userLocationPermView = (ImageView) view.findViewById(R.id.perm_locations);
                holder.userCameraPermView = (ImageView) view.findViewById(R.id.perm_camera);
                holder.userMicPermView = (ImageView) view.findViewById(R.id.perm_mic);

                view.setTag(holder);
            } else {
                //get view holder back_icon
                holder = (ViewHolder) view.getTag();
            }

            setUpRow(i, userPerm, view, holder);

            return view;
        }

        private void setUpRow(int i, UserPermission userPerm, View view, ViewHolder viewHolder) {
            // enable share and change color of view
            viewHolder.usernameView.setText("@"+userPerm.getUser().getUsername());

            if(userPerm.getCamPerm() == true){
                viewHolder.userCameraPermView.setImageDrawable(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.perm_camera_active, null));
            }else {
                viewHolder.userCameraPermView.setImageDrawable(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.perm_camera_deactive, null));
            }

            if(userPerm.getMicPerm() == true){
                viewHolder.userMicPermView.setImageDrawable(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.perm_mic_active, null));
            }else {
                viewHolder.userMicPermView.setImageDrawable(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.perm_mic_deactive, null));
            }

            if(userPerm.getLocPerm() == true){
                viewHolder.userLocationPermView.setImageDrawable(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.perm_locations_active, null));
            }else{
                viewHolder.userLocationPermView.setImageDrawable(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.perm_locations_deactive, null));
            }
            viewHolder.userImageView.setImageDrawable(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.default_user, null));
            //Extracting user image
            if(userPerm.getUser().getUserImage() != null) {
                //viewHolder.userImageView.setImageBitmap(CameraUtils.getRotatedImage(CameraUtils.getBitmapFromBytes(userPerm.getUser().getUserImage().getBytes()), -90));
                //loadBitmap(CameraUtils.getBytesFromImage(CameraUtils.getRotatedImage(CameraUtils.getBitmapFromBytes(Base64.decode(userPerm.getUser().getUserImage(), 0)), -90)), viewHolder.userImageView);

                loadBitmap(userPerm.getUser().getUserImage(), viewHolder.userImageView);
            }
        }

    private void loadBitmap(String data, ImageView imageView) {
        BitmapWorkerTask task = new BitmapWorkerTask(imageView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (new BitmapTaskParams(data, 100, 100)));
        else
            task.execute(new BitmapTaskParams(data, 100, 100));
    }

        /**
         * Keep reference to children view to avoid unnecessary calls
         */
        static class ViewHolder {
            CircularImageView userImageView;
            TextView usernameView;
            ImageView userCameraPermView;
            ImageView userLocationPermView;
            ImageView userMicPermView;
        }



    }

