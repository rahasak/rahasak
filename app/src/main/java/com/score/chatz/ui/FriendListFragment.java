package com.score.chatz.ui;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.score.chatz.R;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.pojo.UserPermission;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class FriendListFragment extends ListFragment implements AdapterView.OnItemClickListener {

    private static final String TAG = FriendListFragment.class.getName();

    private ArrayList<UserPermission> userPermissionList;
    private FriendListAdapter adapter;

    private BroadcastReceiver senzReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got new user from Senz service");

            Senz senz = intent.getExtras().getParcelable("SENZ");
            if (senz.getSenzType() == SenzTypeEnum.SHARE) {
                displayUserList();
            }
        }
    };

    private void setupEmptyTextFont() {
        ((TextView) getActivity().findViewById(R.id.empty_view_friend)).setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/GeosansLight.ttf"));
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
        getListView().setOnItemClickListener(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupEmptyTextFont();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this.getActivity(), ChatActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("SENDER", userPermissionList.get(position).getUser().getUsername());
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        displayUserList();

        //getActivity().registerReceiver(senzReceiver, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.SENZ));
    }

    @Override
    public void onPause() {
        super.onPause();

        //getActivity().unregisterReceiver(senzReceiver);
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
        }
    }

}
