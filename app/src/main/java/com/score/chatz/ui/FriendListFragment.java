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
import com.score.chatz.enums.IntentType;
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

    private Typeface typeface;

    private BroadcastReceiver senzReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got new user from Senz service");

            if (intent.hasExtra("SENZ")) {
                Senz senz = intent.getExtras().getParcelable("SENZ");
                if (needToRefreshList(senz)) {
                    displayUserList();
                }
            }
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
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        typeface = Typeface.createFromAsset(getActivity().getAssets(), "fonts/GeosansLight.ttf");
        setupEmptyTextFont();
    }

    @Override
    public void onResume() {
        super.onResume();
        displayUserList();

        getActivity().registerReceiver(senzReceiver, IntentProvider.getIntentFilter(IntentType.SENZ));
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(senzReceiver);
    }

    private void setupEmptyTextFont() {
        ((TextView) getActivity().findViewById(R.id.empty_view_friend)).setTypeface(typeface);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final SecretUser secretUser = friendsList.get(position);
        if (secretUser.isActive()) {
            Intent intent = new Intent(this.getActivity(), ChatActivity.class);
            intent.putExtra("SENDER", secretUser.getUsername());
            startActivity(intent);
        } else {
            if (secretUser.isSMSRequester()) {
                String contactName = PhoneUtils.getDisplayNameFromNumber(secretUser.getPhone(), getActivity());
                ActivityUtils.displayConfirmationMessageDialog("Confirm", "Would you like to resend friend request to " + contactName + "?", getActivity(), typeface, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // start sharing again
                        // broadcast
                        Intent intent = new Intent(IntentProvider.ACTION_SMS_REQUEST_CONFIRM);
                        intent.putExtra("USERNAME", secretUser.getUsername());
                        intent.putExtra("PHONE", secretUser.getPhone());
                        getActivity().sendBroadcast(intent);
                        ActivityUtils.showCustomToast("Request sent", getActivity());
                    }
                });
            } else {
                String contactName = PhoneUtils.getDisplayNameFromNumber(secretUser.getPhone(), getActivity());
                ActivityUtils.displayConfirmationMessageDialog("Confirm", "Would you like to accept the request from " + contactName + "?", getActivity(), typeface, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // start getting public key and sending confirmation sms
                        // broadcast
                        Intent intent = new Intent(IntentProvider.ACTION_SMS_REQUEST_ACCEPT);
                        intent.putExtra("USERNAME", secretUser.getUsername());
                        intent.putExtra("PHONE", secretUser.getPhone());
                        getActivity().sendBroadcast(intent);
                        ActivityUtils.showCustomToast("Confirmation sent", getActivity());
                    }
                });
            }
        }
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

    private boolean needToRefreshList(Senz senz) {
        return senz.getSenzType() == SenzTypeEnum.SHARE ||
                senz.getSenzType() == SenzTypeEnum.DATA && (senz.getAttributes().containsKey("status") && senz.getAttributes().get("status").equalsIgnoreCase("701"));
    }
}
