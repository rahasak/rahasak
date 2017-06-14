package com.score.rahasak.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.siyamed.shapeimageview.CircularImageView;
import com.github.siyamed.shapeimageview.RoundedImageView;
import com.score.rahasak.R;
import com.score.rahasak.pojo.Permission;
import com.score.rahasak.pojo.SecretUser;
import com.score.rahasak.utils.ImageUtils;
import com.score.rahasak.utils.PhoneBookUtil;

import java.util.ArrayList;


class FriendListAdapter extends ArrayAdapter<SecretUser> {
    Context context;
    private Typeface typeface;

    FriendListAdapter(Context _context, ArrayList<SecretUser> userList) {
        super(_context, R.layout.friend_list_row_layout, R.id.user_name, userList);
        context = _context;
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
            view = layoutInflater.inflate(R.layout.friend_list_row_layout, viewGroup, false);

            //create view holder to store reference to child views
            holder = new ViewHolder();
            holder.userImageView = (CircularImageView) view.findViewById(R.id.user_image);
            holder.selected = (ImageView) view.findViewById(R.id.selected);
            holder.usernameView = (TextView) view.findViewById(R.id.user_name);
            holder.phoneBookNameView = (TextView) view.findViewById(R.id.user_name_from_contacts);
            holder.userLocationPermView = (ImageView) view.findViewById(R.id.perm_locations);
            holder.userCameraPermView = (ImageView) view.findViewById(R.id.perm_camera);

            view.setTag(holder);
        } else {
            //get view holder back_icon
            holder = (ViewHolder) view.getTag();
        }

        setUpRow(i, secretUser, view, holder);

        return view;
    }

    private void setUpRow(int i, SecretUser secretUser, View view, ViewHolder viewHolder) {
        viewHolder.usernameView.setTypeface(typeface, Typeface.NORMAL);
        viewHolder.phoneBookNameView.setTypeface(typeface, Typeface.NORMAL);

        // get permission (isGiven = false)
        Permission permission = secretUser.getRecvPermission();
        if (permission != null && permission.isCam()) {
            viewHolder.userCameraPermView.setImageDrawable(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.perm_camera_active, null));
        } else {
            viewHolder.userCameraPermView.setImageDrawable(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.perm_camera_deactive, null));
        }

        if (permission != null && permission.isLoc()) {
            viewHolder.userLocationPermView.setImageDrawable(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.perm_locations_active, null));
        } else {
            viewHolder.userLocationPermView.setImageDrawable(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.perm_locations_deactive, null));
        }

        // extracting user image
        if (secretUser.getImage() != null) {
            viewHolder.userImageView.setImageBitmap(ImageUtils.decodeBitmap(secretUser.getImage()));
        } else {
            viewHolder.userImageView.setImageResource(R.drawable.default_user);
        }

        // request text
        if (secretUser.isActive()) {
            if (secretUser.getPhone() != null && !secretUser.getPhone().isEmpty()) {
                viewHolder.usernameView.setText(PhoneBookUtil.getContactName(context, secretUser.getPhone()));
                viewHolder.phoneBookNameView.setVisibility(View.VISIBLE);
                viewHolder.phoneBookNameView.setText("@" + secretUser.getUsername());
            } else {
                viewHolder.phoneBookNameView.setVisibility(View.GONE);
                viewHolder.usernameView.setText("@" + secretUser.getUsername());
            }

            viewHolder.userCameraPermView.setVisibility(View.VISIBLE);
            viewHolder.userLocationPermView.setVisibility(View.VISIBLE);
        } else {
            if (secretUser.getPhone() != null && !secretUser.getPhone().isEmpty()) {
                viewHolder.usernameView.setText(PhoneBookUtil.getContactName(context, secretUser.getPhone()) + " @" + secretUser.getUsername());
            } else {
                viewHolder.usernameView.setText("@" + secretUser.getUsername());
            }

            viewHolder.phoneBookNameView.setText(secretUser.isSMSRequester() ? "Sent friend request" : "Received friend request");

            viewHolder.userCameraPermView.setVisibility(View.GONE);
            viewHolder.userLocationPermView.setVisibility(View.GONE);
        }

        // selected
        if (secretUser.isSelected()) {
            viewHolder.selected.setVisibility(View.VISIBLE);
        } else {
            viewHolder.selected.setVisibility(View.GONE);
        }
    }

    /**
     * Keep reference to children view to avoid unnecessary calls
     */
    static class ViewHolder {
        CircularImageView userImageView;
        ImageView selected;
        TextView usernameView;
        TextView phoneBookNameView;
        ImageView userCameraPermView;
        ImageView userLocationPermView;
    }

}

