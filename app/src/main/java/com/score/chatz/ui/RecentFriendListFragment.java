package com.score.chatz.ui;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.siyamed.shapeimageview.CircularImageView;
import com.score.chatz.R;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.pojo.UserPermission;
import com.score.chatz.utils.ActivityUtils;
import com.score.chatz.utils.NotificationUtils;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class RecentFriendListFragment extends ListFragment implements AdapterView.OnItemClickListener {

    private static final String TAG = RecentFriendListFragment.class.getName();
    private ArrayList<UserPermission> userPermissionList;
    private UserPermissionListAdapter adapter;
    public RecentFriendListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(userSharedReceiver);
        //getContext().unregisterReceiver(permissionSharedReceiver);
    }

    private BroadcastReceiver userSharedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got new user from Senz service");
            handleSharedUser(intent);
        }
    };

    private BroadcastReceiver permissionSharedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got new permission change from Senz service");
            handleSharedPermission(intent);
        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.recent_friend_list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setOnItemClickListener(this);
        getContext().registerReceiver(userSharedReceiver, new IntentFilter("com.score.chatz.USER_UPDATE"));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this.getActivity(), UserProfileActivity.class);
        intent.putExtra("SENDER", userPermissionList.get(position).getUser().getUsername());
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        displayUserList();
    }



    /**
     * Display sensor list
     * Basically setup list adapter if have items to display otherwise display empty view
     */
    private void displayUserList() {
        // get User from db
        userPermissionList = (ArrayList<UserPermission>) new SenzorsDbSource(this.getActivity()).getUsersAndTheirPermissions();
        // construct list adapter
        if (userPermissionList.size() > 0) {
            adapter = new UserPermissionListAdapter(getContext(), userPermissionList);
            adapter.notifyDataSetChanged();
            getListView().setAdapter(adapter);
        } else {
            adapter = new UserPermissionListAdapter(getContext(), userPermissionList);
            getListView().setAdapter(adapter);
            //sensorListView.setEmptyView(emptyView);
        }
    }


    /**
     * Handle broadcast message receives
     *
     *
     * @param intent intent
     */
    private void handleSharedUser(Intent intent) {
        displayUserList();
    }


    /**
     * Handle broadcast message receives
     *
     *
     * @param intent intent
     */
    private void handleSharedPermission(Intent intent) {
        displayUserList();
    }


}
