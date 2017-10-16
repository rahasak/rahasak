package com.score.rahasak.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.score.rahasak.R;

public class InviteFragment extends Fragment {

    // Ui elements
    private TextView invite_text;
    private Button addFriendBtn;
    private Button openContactsBtn;

    private Typeface typeface;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.invite_fragment_layout, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        typeface = Typeface.createFromAsset(getActivity().getAssets(), "fonts/GeosansLight.ttf");
        setupUiElements();
        setupOpenContactsBtn();
        setupAddUsersBtn();
    }

    private void setupUiElements() {
        invite_text = (TextView) getActivity().findViewById(R.id.textView);
        invite_text.setTypeface(typeface, Typeface.NORMAL);
    }

    private void setupOpenContactsBtn() {
        openContactsBtn = (Button) getActivity().findViewById(R.id.add_from_contacts_btn);
        openContactsBtn.setTypeface(typeface, Typeface.BOLD);
        openContactsBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Click action
                Intent intent = new Intent(getActivity(), ContactListActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setupAddUsersBtn() {
        addFriendBtn = (Button) getActivity().findViewById(R.id.add_friend_btn);
        addFriendBtn.setTypeface(typeface, Typeface.BOLD);
        addFriendBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AddUsernameActivity.class);
                startActivity(intent);
            }
        });
    }
}
