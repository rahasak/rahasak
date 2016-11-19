package com.score.chatz.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.siyamed.shapeimageview.RoundedImageView;
import com.score.chatz.R;
import com.score.chatz.asyncTasks.BitmapWorkerTask;
import com.score.chatz.pojo.BitmapTaskParams;
import com.score.chatz.pojo.Permission;
import com.score.chatz.pojo.SecretUser;
import com.score.chatz.utils.PhoneUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lakmal on 8/6/16.
 */
class FriendListAdapter extends ArrayAdapter<SecretUser> {
    Context context;
    private Typeface typeface;

    ArrayList<SecretUser> friendsList;

    public FriendListAdapter(Context _context, ArrayList<SecretUser> userPermsList) {
        super(_context, R.layout.single_user_card_row, R.id.user_name, userPermsList);
        context = _context;
        friendsList = userPermsList;
        typeface = Typeface.createFromAsset(context.getAssets(), "fonts/GeosansLight.ttf");
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

        final SecretUser secretUser = getItem(i);

        if (view == null) {
            //inflate sensor list row layout
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.single_user_card_row, viewGroup, false);

            //create view holder to store reference to child views
            holder = new ViewHolder();
            holder.userImageView = (RoundedImageView) view.findViewById(R.id.user_image);
            holder.usernameView = (TextView) view.findViewById(R.id.user_name);
            holder.phoneBookNameView = (TextView) view.findViewById(R.id.user_name_from_contacts);
            holder.userLocationPermView = (ImageView) view.findViewById(R.id.perm_locations);
            holder.userCameraPermView = (ImageView) view.findViewById(R.id.perm_camera);
            holder.userMicPermView = (ImageView) view.findViewById(R.id.perm_mic);

            view.setTag(holder);
        } else {
            //get view holder back_icon
            holder = (ViewHolder) view.getTag();
        }

        setUpRow(i, secretUser, view, holder);

        return view;
    }

    private void setUpRow(int i, SecretUser secretUser, View view, ViewHolder viewHolder) {
        // enable share and change color of view
        if (!secretUser.isActive()) {
            viewHolder.usernameView.setText("Friend request from @" + secretUser.getUsername());
        } else {
            viewHolder.usernameView.setText("@" + secretUser.getUsername());
        }
        viewHolder.usernameView.setTypeface(typeface, Typeface.NORMAL);
        viewHolder.phoneBookNameView.setTypeface(typeface, Typeface.NORMAL);

        // get permission (isGiven = false)
        Permission permission = secretUser.getRecvPermission();
        if (permission != null && permission.isCam()) {
            viewHolder.userCameraPermView.setImageDrawable(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.perm_camera_active, null));
        } else {
            viewHolder.userCameraPermView.setImageDrawable(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.perm_camera_deactive, null));
        }

        if (permission != null && permission.isMic()) {
            viewHolder.userMicPermView.setImageDrawable(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.perm_mic_active, null));
        } else {
            viewHolder.userMicPermView.setImageDrawable(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.perm_mic_deactive, null));
        }

        if (permission != null && permission.isLoc()) {
            viewHolder.userLocationPermView.setImageDrawable(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.perm_locations_active, null));
        } else {
            viewHolder.userLocationPermView.setImageDrawable(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.perm_locations_deactive, null));
        }

        viewHolder.userImageView.setImageDrawable(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.default_user, null));

        // extracting user image
        if (secretUser.getImage() != null) {
            loadBitmap(secretUser.getImage(), viewHolder.userImageView);
        }

        if (secretUser.getPhone() != null && !secretUser.getPhone().equalsIgnoreCase("") || secretUser.getPhone() != null) {
            viewHolder.phoneBookNameView.setVisibility(View.VISIBLE);
            viewHolder.phoneBookNameView.setText(new PhoneUtils().getDisplayNameFromNumber(secretUser.getPhone(), context));
        } else {
            viewHolder.phoneBookNameView.setVisibility(View.GONE);
        }

        if (!secretUser.isActive()) {
            viewHolder.userCameraPermView.setVisibility(View.GONE);
            viewHolder.userMicPermView.setVisibility(View.GONE);
            viewHolder.userLocationPermView.setVisibility(View.GONE);
        } else {
            viewHolder.userCameraPermView.setVisibility(View.VISIBLE);
            viewHolder.userMicPermView.setVisibility(View.VISIBLE);
            viewHolder.userLocationPermView.setVisibility(View.VISIBLE);
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
        RoundedImageView userImageView;
        TextView usernameView;
        TextView phoneBookNameView;
        ImageView userCameraPermView;
        ImageView userLocationPermView;
        ImageView userMicPermView;
    }

    private Permission getPermission(List<Permission> permissionList, boolean isGiven) {
        for (Permission permission : permissionList) {
            if (permission.isGiven() == isGiven) return permission;
        }

        return null;
    }

}

