package com.score.rahasak.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import com.score.rahasak.R;

/**
 * Created by eranga on 12/22/16.
 */
public class PrivacyTermsActivity extends BaseActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_terms);

        String intentType = getIntent().getStringExtra("TYPE");

        setupToolbar();
        setupActionBar(intentType);
        initWebView(intentType);
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setCollapsible(false);
        toolbar.setOverScrollMode(Toolbar.OVER_SCROLL_NEVER);
        setSupportActionBar(toolbar);
    }

    private void setupActionBar(String intentType) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setCustomView(getLayoutInflater().inflate(R.layout.add_user_header, null));
        getSupportActionBar().setDisplayOptions(android.support.v7.app.ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);

        // title
        TextView titleText = (TextView) findViewById(R.id.title);
        titleText.setTypeface(typeface, Typeface.BOLD);

        if (intentType.equalsIgnoreCase("PRIVACY")) {
            titleText.setText("Privacy");
        } else {
            titleText.setText("Terms");
        }

        ImageView backBtn = (ImageView) findViewById(R.id.back_btn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void initWebView(String intentType) {
        webView = (WebView) findViewById(R.id.privacy_terms_web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new SenzWebViewClient());

        if (intentType.equalsIgnoreCase("PRIVACY"))
            webView.loadUrl("http://www.rahasak.com/privacy-policy/");
        else
            webView.loadUrl("http://www.rahasak.com/terms-and-conditions/");
    }

    private class SenzWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return false;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            view.loadUrl("about:blank");
            super.onReceivedError(view, errorCode, description, failingUrl);
        }
    }
}
