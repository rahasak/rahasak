package com.score.rahasak.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.score.rahasak.R;

/**
 * Created by eranga on 12/21/16.
 */
public class AboutFragment extends Fragment {

    private Typeface typeface;

    private WebView webView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.about_layout, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        typeface = Typeface.createFromAsset(getActivity().getAssets(), "fonts/GeosansLight.ttf");
        initWebView();
    }

    private void initWebView() {
        webView = (WebView) getActivity().findViewById(R.id.about_web_view);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new SenzWebViewClient());
        webView.loadUrl("http://www.rahasak.com/privacy-policy/");
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
