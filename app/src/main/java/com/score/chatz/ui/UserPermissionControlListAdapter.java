package com.score.chatz.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.score.chatz.R;
import com.score.chatz.pojo.UserPermission;

import java.util.ArrayList;

/**
 * Created by Lakmal on 8/6/16.
 */
public class UserPermissionControlListAdapter  extends ArrayAdapter<UserPermission> {
    Context context;
    ArrayList<UserPermission> userPermissionList;


    public UserPermissionControlListAdapter(Context _context, ArrayList<UserPermission> userPermsList) {
        super(_context, R.layout.single_user_permission_row, userPermsList);
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
            view = layoutInflater.inflate(R.layout.single_user_permission_row, viewGroup, false);

            //create view holder to store reference to child views
            holder = new ViewHolder();
            holder.userImageView = (ImageView) view.findViewById(R.id.user_image);
            holder.usernameView = (TextView) view.findViewById(R.id.user_name);
            holder.userLocationPermSwitchView = (Switch) view.findViewById(R.id.perm_location_switch);
            holder.userCameraPermSwitchView = (Switch) view.findViewById(R.id.perm_camera_switch);

            view.setTag(holder);
        } else {
            //get view holder back_icon
            holder = (ViewHolder) view.getTag();
        }

        setUpRow(i, userPerm, view, holder);

        return view;
    }

    private void setUpRow(int i, final UserPermission userPerm, View view, ViewHolder viewHolder) {
        // enable share and change color of view
        viewHolder.usernameView.setText(userPerm.getUser().getUsername());
        viewHolder.userCameraPermSwitchView.setChecked(userPerm.getCamPerm());
        viewHolder.userLocationPermSwitchView.setChecked(userPerm.getLocPerm());
        viewHolder.userCameraPermSwitchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked == true){
                    //Send permCam true to user
                    ((SettingsActivity)context).sendPermission(userPerm.getUser(), "true", null);
                }else{
                    //Send permCam false to user
                    ((SettingsActivity)context).sendPermission(userPerm.getUser(), "false", null);
                }
            }
        });
        viewHolder.userLocationPermSwitchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked == true){
                    //Send permLoc true to user
                    ((SettingsActivity)context).sendPermission(userPerm.getUser(), null, "true");
                }else{
                    //Send permLoc false to user
                    ((SettingsActivity)context).sendPermission(userPerm.getUser(), null, "false");
                }
            }
        });
        viewHolder.userImageView.setImageDrawable(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.default_user, null));
        //Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.default_user);
        //viewHolder.userImageView.setImageBitmap(largeIcon);
    }

    /**
     * Keep reference to children view to avoid unnecessary calls
     */
    static class ViewHolder {
        ImageView userImageView;
        TextView usernameView;
        Switch userLocationPermSwitchView;
        Switch userCameraPermSwitchView;
    }
}
