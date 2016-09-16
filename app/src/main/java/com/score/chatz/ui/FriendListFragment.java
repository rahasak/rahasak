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

import com.score.chatz.R;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.handlers.IntentProvider;
import com.score.chatz.pojo.UserPermission;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class FriendListFragment extends ListFragment implements AdapterView.OnItemClickListener {

    private static final String TAG = FriendListFragment.class.getName();

    private ArrayList<UserPermission> userPermissionList;
    private FriendListAdapter adapter;

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(userSharedReceiver);
    }

    private BroadcastReceiver userSharedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got new user from Senz service");
            handleSharedUser(intent);
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

        getContext().registerReceiver(userSharedReceiver, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.DATA_SENZ));
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
            adapter = new FriendListAdapter(getContext(), userPermissionList);
            adapter.notifyDataSetChanged();
            getListView().setAdapter(adapter);
        } else {
            adapter = new FriendListAdapter(getContext(), userPermissionList);
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

}
