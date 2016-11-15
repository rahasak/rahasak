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
import com.score.chatz.application.IntentProvider;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.pojo.SecretUser;
import com.score.chatz.utils.ActivityUtils;
import com.score.chatz.utils.PhoneUtils;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class FriendListFragment extends ListFragment implements AdapterView.OnItemClickListener {

    private static final String TAG = FriendListFragment.class.getName();

    private ArrayList<SecretUser> friendsList;
    private FriendListAdapter adapter;

    private BroadcastReceiver senzReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got new user from Senz service");

            Senz senz = null;
            if (intent.hasExtra("SENZ")) {
                senz = intent.getExtras().getParcelable("SENZ");
                if (senz.getSenzType() == SenzTypeEnum.SHARE) {
                    displayUserList();
                } else if (senz.getSenzType() == SenzTypeEnum.DATA && senz.getAttributes().containsKey("status") && senz.getAttributes().get("status").equalsIgnoreCase("701")) {
                    // New user added to list via user action after an sms
                    ActivityUtils.cancelProgressDialog();
                    displayUserList();
                }
            } else if (senz == null && intent.hasExtra("SMS_RECEIVED")) {
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
        final int pos = position;
        if (friendsList.get(position).isActive()) {
            Intent intent = new Intent(this.getActivity(), ChatActivity.class);
            intent.putExtra("SENDER", friendsList.get(position));
            startActivity(intent);
        } else {
            ActivityUtils.displayConfirmationMessageDialog("Confirm User", "Would you like to add this user(" + new PhoneUtils().getDisplayNameFromNumber(friendsList.get(position).getPhone(), getActivity()) + ") to your friends list?", getActivity(), Typeface.createFromAsset(getActivity().getAssets(), "fonts/GeosansLight.ttf"), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityUtils.showProgressDialog(getActivity(), "Please wait...");
                    ((HomeActivity) getActivity()).addUser(friendsList.get(pos).getUsername(), friendsList.get(pos).getPhone());
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        displayUserList();

        getActivity().registerReceiver(senzReceiver, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.SENZ));
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(senzReceiver);
    }

    /**
     * Display sensor list
     * Basically setup list adapter if have items to display otherwise display empty view
     */
    private void displayUserList() {
        // get User from db
        friendsList = new SenzorsDbSource(this.getActivity()).getSecretUserList();
        // construct list adapter
        if (friendsList.size() > 0) {
            adapter = new FriendListAdapter(getContext(), friendsList);
            adapter.notifyDataSetChanged();
            getListView().setAdapter(adapter);
        } else {
            adapter = new FriendListAdapter(getContext(), friendsList);
            getListView().setAdapter(adapter);
        }
    }
}
