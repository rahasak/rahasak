package com.score.chatz.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
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

import com.score.chatz.R;
import com.score.chatz.application.IntentProvider;
import com.score.chatz.asyncTasks.BitmapWorkerTask;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.enums.IntentType;
import com.score.chatz.pojo.BitmapTaskParams;
import com.score.chatz.pojo.Permission;
import com.score.chatz.pojo.SecretUser;
import com.score.chatz.utils.ActivityUtils;
import com.score.chatz.utils.ImageUtils;
import com.score.chatz.utils.NetworkUtil;
import com.score.chatz.utils.SenzUtils;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.util.HashMap;
import java.util.List;

public class UserProfileActivity extends BaseActivity implements Switch.OnCheckedChangeListener {
    private static final String TAG = UserProfileActivity.class.getName();

    private Switch cameraSwitch;
    private Switch locationSwitch;
    private Switch micSwitch;
    private Switch waitingForResponseSwitch;
    private ImageView userImageView;

    private SecretUser secretUser;

    private Senz currentSenz;

    SenzorsDbSource dbSource;

    private final static String SECRET_USER = "SECRET_USER";

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putParcelable(SECRET_USER, secretUser);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            // Restore value of members from saved state
            secretUser = savedInstanceState.getParcelable(SECRET_USER);
        }
    }

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

        initUi();
        initThisUser();
        initPermissions();
        setupGetProfileImageBtn();
        setupCoordinator();
        setupBackBtn();
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

    private void initUi() {
        userImageView = (ImageView) findViewById(R.id.clickable_image);
        cameraSwitch = (Switch) findViewById(R.id.perm_camera_switch);
        micSwitch = (Switch) findViewById(R.id.perm_mic_switch);
        locationSwitch = (Switch) findViewById(R.id.perm_location_switch);

        ((TextView) findViewById(R.id.perm_loc_text)).setTypeface(typeface);
        ((TextView) findViewById(R.id.perm_cam_text)).setTypeface(typeface);
        ((TextView) findViewById(R.id.perm_mic_text)).setTypeface(typeface);
    }

    private void initThisUser() {
//        String username = getIntent().getExtras().getString("SENDER");
//        secretUser = new SenzorsDbSource(this).getSecretUser(username);
        secretUser = getIntent().getExtras().getParcelable("SECRET_USER");
    }

    private void initPermissions() {
        if (secretUser.getImage() != null)
            userImageView.setImageBitmap(new ImageUtils().decodeBitmap(secretUser.getImage()));

        Permission permission = secretUser.getGivenPermission();
        cameraSwitch.setChecked(permission.isCam());
        micSwitch.setChecked(permission.isLoc());
        locationSwitch.setChecked(permission.isMic());

        cameraSwitch.setOnCheckedChangeListener(this);
        locationSwitch.setOnCheckedChangeListener(this);
        micSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == cameraSwitch) {
            if (NetworkUtil.isAvailableNetwork(UserProfileActivity.this)) {
                if (isServiceBound) {
                    if (isChecked) {
                        //Send permCam true to user
                        sendPermission("cam", "on");
                    } else {
                        //Send permCam false to user
                        sendPermission("cam", "off");
                    }
                    waitingForResponseSwitch = cameraSwitch;
                } else {
                    ActivityUtils.showCustomToast(getResources().getString(R.string.connecting_with_server_error), getBaseContext());
                    resetStateOnSwitch(cameraSwitch, isChecked);
                }
            } else {
                Toast.makeText(UserProfileActivity.this, R.string.no_internet, Toast.LENGTH_SHORT).show();
                resetStateOnSwitch(cameraSwitch, isChecked);
            }
        } else if (buttonView == locationSwitch) {
            if (NetworkUtil.isAvailableNetwork(UserProfileActivity.this)) {
                if (isServiceBound) {
                    if (isChecked) {
                        //Send permLoc true to user
                        sendPermission("loc", "on");
                    } else {
                        //Send permLoc false to user
                        sendPermission("loc", "off");
                    }
                    waitingForResponseSwitch = locationSwitch;
                } else {
                    ActivityUtils.showCustomToast(getResources().getString(R.string.connecting_with_server_error), getBaseContext());
                    resetStateOnSwitch(locationSwitch, isChecked);
                }
            } else {
                Toast.makeText(UserProfileActivity.this, R.string.no_internet, Toast.LENGTH_SHORT).show();
                resetStateOnSwitch(locationSwitch, isChecked);
            }
        } else if (buttonView == micSwitch) {
            if (NetworkUtil.isAvailableNetwork(UserProfileActivity.this)) {
                if (isServiceBound) {
                    if (isChecked) {
                        //Send permMic true to user
                        sendPermission("mic", "on");
                    } else {
                        //Send permMic false to user
                        sendPermission("mic", "off");
                    }
                    waitingForResponseSwitch = micSwitch;
                } else {
                    ActivityUtils.showCustomToast(getResources().getString(R.string.connecting_with_server_error), getBaseContext());
                    resetStateOnSwitch(micSwitch, isChecked);
                }
            } else {
                Toast.makeText(UserProfileActivity.this, R.string.no_internet, Toast.LENGTH_SHORT).show();
                resetStateOnSwitch(micSwitch, isChecked);
            }
        }
    }

    private void resetStateOnSwitch(Switch swt, boolean currentState) {
        // Remove listener
        swt.setOnCheckedChangeListener(null);

        // Change view state
        if (currentState) {
            swt.setChecked(false);
        } else {
            swt.setChecked(true);
        }

        // Attach listener again
        swt.setOnCheckedChangeListener(this);
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

    private void setupGetProfileImageBtn() {
        ImageView imgBtn = (ImageView) findViewById(R.id.profile_camera_icon);
        imgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (NetworkUtil.isAvailableNetwork(getBaseContext())) {
                    if (isServiceBound) {
                        getProfilePhoto();
                    } else {
                        ActivityUtils.showCustomToast(getResources().getString(R.string.connecting_with_server_error), getBaseContext());
                    }
                } else {
                    ActivityUtils.showCustomToast(getResources().getString(R.string.no_internet), getBaseContext());
                }
            }
        });
    }

    /*
     * Get photo of user
     */
    private void getProfilePhoto() {
        ActivityUtils.showProgressDialog(this, "Calling selfie...");

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

        send(senz);
    }

    private void setupCoordinator() {
        CollapsingToolbarLayout collapsingToolbar =
                (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle("@" + secretUser.getUsername());
        collapsingToolbar.setCollapsedTitleTextColor(getResources().getColor(R.color.colorPrimary));
        collapsingToolbar.setExpandedTitleColor(getResources().getColor(R.color.colorPrimary));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        View header = getLayoutInflater().inflate(R.layout.profile_header, null);
        toolbar.setContentInsetsAbsolute(0, 0);
        toolbar.addView(header);
    }

    private void setupBackBtn() {
        ((ImageView) findViewById(R.id.back_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadBitmap(String data, ImageView imageView) {
        BitmapWorkerTask task = new BitmapWorkerTask(imageView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (new BitmapTaskParams(data, 1000, 1000)));
        else
            task.execute(new BitmapTaskParams(data, 1000, 1000));
    }

    public void sendPermission(String permName, String permValue) {
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

        currentSenz = senz;

        ActivityUtils.showProgressDialog(this, "Please wait...");
        send(senz);
    }

    private void handleDataSenz(Senz senz) {
        if (senz.getAttributes().containsKey("status")) {
            // status response received
            if (senz.getAttributes().get("status").equalsIgnoreCase("DELIVERED")) {
                // delivery message
            } else if (senz.getAttributes().get("status").equalsIgnoreCase("701")) {
                ActivityUtils.cancelProgressDialog();
                waitingForResponseSwitch = null;

                // permission sharing done
                if (currentSenz.getAttributes().get("uid").equalsIgnoreCase(senz.getAttributes().get("uid"))) {
                    updatePermission(currentSenz);
                }
            } else if (senz.getAttributes().get("status").equalsIgnoreCase("801")) {
                // user busy
                ActivityUtils.cancelProgressDialog();
                displayInformationMessageDialog("info", "user busy");
            } else if (senz.getAttributes().get("status").equalsIgnoreCase("offline")) {
                if (waitingForResponseSwitch != null) {
                    resetStateOnSwitch(waitingForResponseSwitch, waitingForResponseSwitch.isChecked() ? true : false);
                    waitingForResponseSwitch = null;
                }

                ActivityUtils.cancelProgressDialog();
                Toast.makeText(this, "@" + secretUser.getUsername() + " not available at this moment", Toast.LENGTH_LONG).show();
            } else {
                //ActivityUtils.cancelProgressDialog();
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

    private void updatePermission(Senz senz) {
        if (senz.getAttributes().containsKey("lat")) {
            dbSource.updatePermission(secretUser.getGivenPermission().getId(), "loc", senz.getAttributes().get("lat").equalsIgnoreCase("on"));
        }
        if (senz.getAttributes().containsKey("cam")) {
            dbSource.updatePermission(secretUser.getGivenPermission().getId(), "cam", senz.getAttributes().get("cam").equalsIgnoreCase("on"));
        }
        if (senz.getAttributes().containsKey("mic")) {
            dbSource.updatePermission(secretUser.getGivenPermission().getId(), "mic", senz.getAttributes().get("mic").equalsIgnoreCase("on"));
        }
    }

    private Permission getPermission(List<Permission> permissionList, boolean isGiven) {
        for (Permission permission : permissionList) {
            if (permission.isGiven() == isGiven) return permission;
        }

        return null;
    }

}
