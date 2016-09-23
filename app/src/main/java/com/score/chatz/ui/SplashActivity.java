package com.score.chatz.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.score.chatz.R;
import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.services.RemoteSenzService;
import com.score.chatz.utils.PreferenceUtils;

/**
 * Splash activity, send login query from here
 *
 * @author eranga herath(erangaeb@gmail.com)
 */
public class SplashActivity extends AppCompatActivity {
    private final int SPLASH_DISPLAY_LENGTH = 3000;
    private static final String TAG = SplashActivity.class.getName();

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.splash_layout);
        startService();
        initNavigation();
    }

    private void startService() {
        Intent serviceIntent = new Intent(this, RemoteSenzService.class);
        startService(serviceIntent);
    }

    /**
     * Determine where to go from here
     */
    private void initNavigation() {
        // determine where to go
        // start service
        try {
            PreferenceUtils.getUser(this);
            // have user, so move to home
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            finish();
            startActivity(intent);
        } catch (NoUserException e) {
            e.printStackTrace();
            navigateToSplash();
        }
    }

    /**
     * Switch to home activity
     * This method will be call after successful login
     */
    private void navigateToSplash() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                navigateRegistration();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }

    private void navigateRegistration() {
        // no user, so move to registration
        Intent intent = new Intent(this, RegistrationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        finish();
        startActivity(intent);
    }

    /**
     * {@inheritDoc}
     */
    protected void onResume() {
        super.onResume();
    }

    /**
     * Switch to home activity
     * This method will be call after successful login
     */
    public void navigateToHome() {
        Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
        SplashActivity.this.startActivity(intent);
        SplashActivity.this.finish();

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}