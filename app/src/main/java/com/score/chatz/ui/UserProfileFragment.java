package com.score.chatz.ui;

import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.score.chatz.R;
import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.utils.PreferenceUtils;

public class UserProfileFragment extends android.support.v4.app.Fragment {

    TextView appUser;
    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.user_profile_fragment, container, false);
        return root;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        appUser = (TextView) getActivity().findViewById(R.id.registered_user);
        try {
            appUser.setText(PreferenceUtils.getUser(this.getContext()).getUsername());
        } catch (NoUserException e) {
            appUser.setText(getResources().getString(R.string.no_registered_user));
        }
    }
}
