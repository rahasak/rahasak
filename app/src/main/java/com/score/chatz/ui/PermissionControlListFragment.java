package com.score.chatz.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import com.score.chatz.R;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.pojo.UserPermission;
import com.score.chatz.utils.ActivityUtils;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.util.ArrayList;

public class PermissionControlListFragment extends ListFragment implements AdapterView.OnItemClickListener {


    private static final String TAG = PermissionControlListFragment.class.getName();
    private ArrayList<UserPermission> userPermissionList;
    private UserPermissionControlListAdapter adapter;
    public PermissionControlListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(userSharedReceiver);
        if (senzDataReceiver != null) getContext().unregisterReceiver(senzDataReceiver);
    }

    private BroadcastReceiver userSharedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got user shared intent from Senz service");
            handleSharedUser(intent);
        }
    };

    private BroadcastReceiver senzDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got message from Senz service");
            handleMessage(intent);
        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.permission_control_list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //ArrayAdapter adapter = ArrayAdapter.createFromResource(getActivity(), R.array.users, R.layout.single_user_card_row);
        //MyUsersAdapter adapter = new MyUsersAdapter(getContext(), userList);
        //setListAdapter(adapter);
        getListView().setOnItemClickListener(this);
        getListView().setOnItemClickListener(this);
        //Register for new users added
        getContext().registerReceiver(userSharedReceiver, new IntentFilter("com.score.chatz.USER_SHARED"));
        //Register for fail messages, incase other user is not online
        getContext().registerReceiver(senzDataReceiver, new IntentFilter("com.score.chatz.DATA_SENZ")); //Incoming data share
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Toast.makeText(getActivity(), "Item " + position, Toast.LENGTH_SHORT).show();
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
        userPermissionList = (ArrayList<UserPermission>) new SenzorsDbSource(this.getActivity()).getUsersAndTheirConfigurablePermissions();
        // construct list adapter
        if (userPermissionList.size() > 0) {
            adapter = new UserPermissionControlListAdapter(getContext(), userPermissionList);
            adapter.notifyDataSetChanged();
            getListView().setAdapter(adapter);
        } else {
            adapter = new UserPermissionControlListAdapter(getContext(), userPermissionList);
            getListView().setAdapter(adapter);
            //sensorListView.setEmptyView(emptyView);
        }
    }

    private void handleMessage(Intent intent) {
        String action = intent.getAction();
        if (action.equalsIgnoreCase("com.score.chatz.DATA_SENZ")) {
            Senz senz = intent.getExtras().getParcelable("SENZ");
            if (senz.getAttributes().containsKey("msg")) {
                // msg response received
                ActivityUtils.cancelProgressDialog();
                String msg = senz.getAttributes().get("msg");
                if (msg != null && msg.equalsIgnoreCase("USER_NOT_ONLINE")) {
                    //Reset display
                    displayUserList();
                }
            }
        }
    }


    private void handleSharedUser(Intent intent) {
        displayUserList();
    }


}
