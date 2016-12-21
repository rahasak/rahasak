package com.score.rahasak.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.score.rahasak.R;
import com.score.rahasak.application.IntentProvider;
import com.score.rahasak.db.SenzorsDbSource;
import com.score.rahasak.enums.IntentType;
import com.score.rahasak.pojo.Permission;
import com.score.rahasak.pojo.SecretUser;
import com.score.rahasak.utils.ActivityUtils;
import com.score.rahasak.utils.ImageUtils;
import com.score.rahasak.utils.NetworkUtil;
import com.score.rahasak.utils.SenzUtils;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.util.HashMap;

public class UserProfileActivity extends BaseActivity implements Switch.OnCheckedChangeListener, View.OnClickListener {

    private static final String TAG = UserProfileActivity.class.getName();

    private Switch cameraSwitch;
    private Switch locationSwitch;
    private Switch micSwitch;

    private ImageView backImageView;
    private ImageView userImageView;
    private ImageView getPicImageView;

    private TextView camText;
    private TextView locText;
    private TextView micText;

    private SecretUser secretUser;
    private String selectedPermission;

    private SenzorsDbSource dbSource;

    private BroadcastReceiver senzReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got message from Senz service");
            Senz senz = intent.getExtras().getParcelable("SENZ");
            switch (senz.getSenzType()) {
                case DATA:
                    handleDataSenz(senz);
                    break;
                case STREAM:
                    handleStreamSenz(senz);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        dbSource = new SenzorsDbSource(this);

        initUser();
        initUi();
        initToolbar();
        initPermissions();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "Bind to senz service");
        bindToService();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // unbind from service
        if (isServiceBound) {
            Log.d(TAG, "Unbind to senz service");
            unbindService(senzServiceConnection);

            isServiceBound = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(senzReceiver, IntentProvider.getIntentFilter(IntentType.SENZ));
    }

    @Override
    public void onPause() {
        super.onStop();
        if (senzReceiver != null) unregisterReceiver(senzReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putParcelable("SECRET_USER", secretUser);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            // Restore value of members from saved state
            secretUser = savedInstanceState.getParcelable("SECRET_USER");
        }
    }

    private void initUser() {
        if (getIntent().getExtras() != null)
            secretUser = getIntent().getExtras().getParcelable("SECRET_USER");
    }

    private void initUi() {
        cameraSwitch = (Switch) findViewById(R.id.perm_camera_switch);
        micSwitch = (Switch) findViewById(R.id.perm_mic_switch);
        locationSwitch = (Switch) findViewById(R.id.perm_location_switch);

        camText = (TextView) findViewById(R.id.perm_cam_text);
        micText = (TextView) findViewById(R.id.perm_mic_text);
        locText = (TextView) findViewById(R.id.perm_loc_text);

        camText.setTypeface(typeface);
        micText.setTypeface(typeface);
        locText.setTypeface(typeface);

        userImageView = (ImageView) findViewById(R.id.clickable_image);
        if (secretUser.getImage() != null)
            userImageView.setImageBitmap(new ImageUtils().decodeBitmap(secretUser.getImage()));

        getPicImageView = (ImageView) findViewById(R.id.profile_camera_icon);
        getPicImageView.setOnClickListener(this);
    }

    private void initPermissions() {
        Permission permission = secretUser.getGivenPermission();
        cameraSwitch.setChecked(permission.isCam());
        micSwitch.setChecked(permission.isLoc());
        locationSwitch.setChecked(permission.isMic());

        cameraSwitch.setOnCheckedChangeListener(this);
        locationSwitch.setOnCheckedChangeListener(this);
        micSwitch.setOnCheckedChangeListener(this);
    }

    private void initToolbar() {
        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle("@" + secretUser.getUsername());
        collapsingToolbar.setCollapsedTitleTextColor(getResources().getColor(R.color.colorPrimary));
        collapsingToolbar.setExpandedTitleColor(getResources().getColor(R.color.colorPrimary));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        View header = getLayoutInflater().inflate(R.layout.profile_header, null);
        toolbar.setContentInsetsAbsolute(0, 0);
        toolbar.addView(header);

        backImageView = (ImageView) findViewById(R.id.back_btn);
        backImageView.setOnClickListener(this);
    }

    private void getProfilePhoto() {
        if (NetworkUtil.isAvailableNetwork(UserProfileActivity.this)) {
            if (isServiceBound) {
                // create senz attributes
                HashMap<String, String> senzAttributes = new HashMap<>();
                String timestamp = ((Long) (System.currentTimeMillis() / 1000)).toString();
                senzAttributes.put("time", timestamp);
                senzAttributes.put("cam", "");
                senzAttributes.put("uid", SenzUtils.getUid(this, timestamp));

                // new senz
                String id = "_ID";
                String signature = "_SIGNATURE";
                SenzTypeEnum senzType = SenzTypeEnum.GET;
                Senz senz = new Senz(id, signature, senzType, null, new User(secretUser.getId(), secretUser.getUsername()), senzAttributes);

                ActivityUtils.showProgressDialog(this, "Calling selfie...");
                send(senz);
            } else {
                Toast.makeText(this, "Cannot connect with service", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
        }
    }

    public void sharePermission(String permName, String permValue) {
        if (NetworkUtil.isAvailableNetwork(UserProfileActivity.this)) {
            if (isServiceBound) {
                // create senz attributes
                HashMap<String, String> senzAttributes = new HashMap<>();
                if (permName.equalsIgnoreCase("loc")) {
                    senzAttributes.put("lat", permValue);
                    senzAttributes.put("lon", permValue);
                } else {
                    senzAttributes.put(permName, permValue);
                }

                String timestamp = ((Long) (System.currentTimeMillis() / 1000)).toString();
                senzAttributes.put("time", timestamp);
                senzAttributes.put("uid", SenzUtils.getUid(this, timestamp));

                // new senz
                String id = "_ID";
                String signature = "_SIGNATURE";
                SenzTypeEnum senzType = SenzTypeEnum.SHARE;
                Senz senz = new Senz(id, signature, senzType, null, new User(secretUser.getId(), secretUser.getUsername()), senzAttributes);

                ActivityUtils.showProgressDialog(this, "Please wait...");
                send(senz);
            } else {
                Toast.makeText(this, "Cannot connect with service", Toast.LENGTH_LONG).show();
                resetPermission();
            }
        } else {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
            resetPermission();
        }
    }

    private void handleDataSenz(Senz senz) {
        if (senz.getAttributes().containsKey("status")) {
            if (senz.getAttributes().get("status").equalsIgnoreCase("PERMISSION_SHARED")) {
                ActivityUtils.cancelProgressDialog();

                updatePermission();
            } else if (senz.getAttributes().get("status").equalsIgnoreCase("OFFLINE")) {
                ActivityUtils.cancelProgressDialog();
                Toast.makeText(this, "@" + secretUser.getUsername() + " not available at this moment", Toast.LENGTH_LONG).show();

                resetPermission();
            }
        }
    }

    private void handleStreamSenz(Senz senz) {
        if (senz.getSender().getUsername().equalsIgnoreCase(secretUser.getUsername()) && senz.getAttributes().containsKey("cam")) {
            ActivityUtils.cancelProgressDialog();

            // save profile picture in db
            String encodedImage = senz.getAttributes().get("cam");
            dbSource.updateSecretUser(secretUser.getUsername(), "image", encodedImage);

            // display image
            userImageView.setImageBitmap(new ImageUtils().decodeBitmap(encodedImage));
        }
    }

    private void updatePermission() {
        if (selectedPermission.equalsIgnoreCase("CAM")) {
            boolean updateTo = !secretUser.getGivenPermission().isCam();
            secretUser.getGivenPermission().setCam(updateTo);
            dbSource.updatePermission(secretUser.getGivenPermission().getId(), "cam", updateTo);
        } else if (selectedPermission.equalsIgnoreCase("MIC")) {
            boolean updateTo = !secretUser.getGivenPermission().isMic();
            secretUser.getGivenPermission().setMic(updateTo);
            dbSource.updatePermission(secretUser.getGivenPermission().getId(), "mic", updateTo);
        } else if (selectedPermission.equalsIgnoreCase("LOC")) {
            boolean updateTo = !secretUser.getGivenPermission().isLoc();
            secretUser.getGivenPermission().setLoc(updateTo);
            dbSource.updatePermission(secretUser.getGivenPermission().getId(), "loc", updateTo);
        }
    }

    private void resetPermission() {
        if (selectedPermission.equalsIgnoreCase("CAM")) {
            cameraSwitch.setChecked(secretUser.getGivenPermission().isCam());
        } else if (selectedPermission.equalsIgnoreCase("MIC")) {
            micSwitch.setChecked(secretUser.getGivenPermission().isMic());
        } else if (selectedPermission.equalsIgnoreCase("LOC")) {
            locationSwitch.setChecked(secretUser.getGivenPermission().isLoc());
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == cameraSwitch) {
            if (isChecked) sharePermission("cam", "on");
            else sharePermission("cam", "off");
            selectedPermission = "CAM";
        } else if (buttonView == locationSwitch) {
            if (isChecked) sharePermission("loc", "on");
            else sharePermission("loc", "off");
            selectedPermission = "LOC";
        } else if (buttonView == micSwitch) {
            if (isChecked) sharePermission("mic", "on");
            else sharePermission("mic", "off");
            selectedPermission = "MIC";
        }
    }

    @Override
    public void onClick(View v) {
        if (v == getPicImageView) {
            getProfilePhoto();
        } else if (v == backImageView) {
            finish();
        }
    }

}
