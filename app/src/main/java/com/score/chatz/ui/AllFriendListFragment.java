package com.score.chatz.ui;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.score.chatz.R;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.pojo.UserPermission;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class AllFriendListFragment  extends ListFragment implements AdapterView.OnItemClickListener {

    private static final String TAG = AllFriendListFragment.class.getName();
    private ArrayList<UserPermission> userPermissionList;
    private UserPermissionListAdapter adapter;
    public AllFriendListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.recent_friend_list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //ArrayAdapter adapter = ArrayAdapter.createFromResource(getActivity(), R.array.users, R.layout.single_user_card_row);
        //adapter = new UserPermissionListAdapter(getContext(), userPermissionList);
        //setListAdapter(adapter);
        getListView().setOnItemClickListener(this);
        getContext().registerReceiver(userSharedReceiver, new IntentFilter("com.score.chatz.USER_UPDATE"));
    }

    private BroadcastReceiver userSharedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got new user from Senz service");
            handleSharedUser(intent);
        }
    };

    private void handleSharedUser(Intent intent) {
        displayUserList();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Intent intent = new Intent(this.getActivity(), UserProfileActivity.class);

        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        displayUserList();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(userSharedReceiver);
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



}
