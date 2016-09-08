package com.score.chatz.ui;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.Typeface;
import android.os.IBinder;
import android.os.RemoteException;
//import android.support.design.widget.FloatingActionButton;
//import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.score.chatz.R;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.utils.ActivityUtils;
import com.score.chatz.utils.NotificationUtils;
import com.score.senz.ISenzService;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = SplashActivity.class.getName();
    private Toolbar toolbar;
    //private TabLayout tabLayout;
    private ViewPager viewPager;
    private TextView headerTitle;
    private ImageView backBtn;

    // service interface
    private ISenzService senzService = null;

    // service connection
    private ServiceConnection senzServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d("TAG", "Connected with senz service");
            senzService = ISenzService.Stub.asInterface(service);
        }

        public void onServiceDisconnected(ComponentName className) {
            senzService = null;
            Log.d("TAG", "Disconnected from senz service");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);


        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        //displaying custom ActionBar
        getSupportActionBar().setCustomView(getLayoutInflater().inflate(R.layout.settings_action_bar, null));
        getSupportActionBar().setDisplayOptions(android.support.v7.app.ActionBar.DISPLAY_SHOW_CUSTOM);

        viewPager = (ViewPager) findViewById(R.id.pager);
        setupViewPager(viewPager);

        /*tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        setupTabLayouts();*/

        headerTitle = (TextView) findViewById(R.id.header_center_text);
        setupFonts();
        setupBackBtn();

    }

    private void setupTabLayouts() {
        //tabLayout.getTabAt(0).setCustomView(R.layout.settings_permission_tab);
        //tabLayout.getTabAt(1).setCustomView(R.layout.settings_person_tab);
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new PermissionControlListFragment(), getResources().getString(R.string.action_bar_permissions));
        adapter.addFragment(new UserProfileFragment(), getResources().getString(R.string.action_bar_profile));
        viewPager.setAdapter(adapter);
    }

    //Inner class -  View Pager Adapter
    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    private void setupFonts(){
        Typeface typefaceThin = Typeface.createFromAsset(getAssets(), "fonts/HelveticaNeue-Light.otf");
        headerTitle.setTypeface(typefaceThin, Typeface.NORMAL);
    }

    private void goBackToHome(){
        Log.d(TAG, "go home clicked");
        this.finish();
    }

    private void setupBackBtn(){
        backBtn = (ImageView) findViewById(R.id.goBackToHomeImg);
        backBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                goBackToHome();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop() {
        super.onStop();

        // Unbind from the service
        this.unbindService(senzServiceConnection);
    }


    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = new Intent();
        intent.setClassName("com.score.chatz", "com.score.chatz.services.RemoteSenzService");
        bindService(intent, senzServiceConnection, Context.BIND_AUTO_CREATE);


    }





    /**
     *
     */
    public void sendPermission(User receiver, String camPerm, String locPerm) {
        try {
            // create senz attributes
            HashMap<String, String> senzAttributes = new HashMap<>();
            senzAttributes.put("msg", "newPerm");
            if(camPerm != null) {
                senzAttributes.put("camPerm", camPerm); //Default Values, later in ui allow user to configure this on share
            }
            if(locPerm != null) {
                senzAttributes.put("locPerm", locPerm); //Dafault Values
            }
            senzAttributes.put("time", ((Long) (System.currentTimeMillis() / 1000)).toString());

            // new senz
            String id = "_ID";
            String signature = "_SIGNATURE";
            SenzTypeEnum senzType = SenzTypeEnum.DATA;
            Senz senz = new Senz(id, signature, senzType, null, receiver, senzAttributes);

            senzService.send(senz);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    public void notifyChildFragmentsOfNewData(){

    }

}
