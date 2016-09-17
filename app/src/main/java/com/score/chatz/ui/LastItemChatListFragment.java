package com.score.chatz.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.score.chatz.R;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.application.IntentProvider;
import com.score.chatz.pojo.Secret;
import com.score.chatz.utils.PreferenceUtils;
import com.score.chatz.utils.SecretsUtil;
import com.score.senzc.pojos.User;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by lakmalcaldera on 8/19/16.
 */
public class LastItemChatListFragment extends ListFragment implements AdapterView.OnItemClickListener {

    private static final String TAG = LastItemChatListFragment.class.getName();

    private ArrayList<Secret> allSecretsList;
    private LastItemChatListAdapter adapter;
    private User currentUser;
    SenzorsDbSource dbSource;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        try {
            currentUser = PreferenceUtils.getUser(this.getContext());
        } catch (NoUserException ex) {
            Log.e(TAG, "No user Found.");
        }
        dbSource = new SenzorsDbSource(getContext());
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.last_item_chat_list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getContext().registerReceiver(userSharedReceiver, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.DATA_SENZ));
        getListView().setOnItemClickListener(this);
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
        allSecretsList = (ArrayList<Secret>) dbSource.getAllOtherSercets(currentUser);
        adapter = new LastItemChatListAdapter(getContext(), allSecretsList);
        getListView().setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void removeOldItemsFromChat() {
        Iterator<Secret> iter = allSecretsList.iterator();
        while (iter.hasNext()) {
            Secret secret = iter.next();

            if (!SecretsUtil.isSecretToBeShown(secret)) {
                iter.remove();
                dbSource.deleteSecret(secret);
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this.getActivity(), ChatActivity.class);
        intent.putExtra("SENDER", allSecretsList.get(position).getWho().getUsername());
        startActivity(intent);
    }


}
