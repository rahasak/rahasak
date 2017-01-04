package com.score.rahasak.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.score.rahasak.R;

/**
 * Created by eranga on 12/21/16.
 */
public class AboutFragment extends Fragment implements View.OnClickListener {

    private Typeface typeface;

    private RelativeLayout privacyLayout;
    private RelativeLayout termsLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.about_fragment_layout, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initUi();
    }

    private void initUi() {
        typeface = Typeface.createFromAsset(getActivity().getAssets(), "fonts/GeosansLight.ttf");

        ((TextView) getActivity().findViewById(R.id.about_rahasak)).setTypeface(typeface);
        ((TextView) getActivity().findViewById(R.id.version_text)).setTypeface(typeface, Typeface.NORMAL);
        ((TextView) getActivity().findViewById(R.id.terms_text)).setTypeface(typeface, Typeface.NORMAL);
        ((TextView) getActivity().findViewById(R.id.privacy_text)).setTypeface(typeface, Typeface.NORMAL);

        privacyLayout = (RelativeLayout) getActivity().findViewById(R.id.privacy_layout);
        termsLayout = (RelativeLayout) getActivity().findViewById(R.id.terms_layout);

        privacyLayout.setOnClickListener(this);
        termsLayout.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == privacyLayout) {
            Intent intent = new Intent(getActivity(), PrivacyTermsActivity.class);
            intent.putExtra("TYPE", "PRIVACY");
            startActivity(intent);
        } else if (v == termsLayout) {
            Intent intent = new Intent(getActivity(), PrivacyTermsActivity.class);
            intent.putExtra("TYPE", "TERMS");
            startActivity(intent);
        }
    }
}
