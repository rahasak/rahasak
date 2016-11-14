package com.score.chatz.ui;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.score.chatz.R;
import com.score.chatz.application.IntentProvider;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.exceptions.NoUserException;
import com.score.chatz.remote.SenzService;
import com.score.chatz.utils.ActivityUtils;
import com.score.chatz.utils.NetworkUtil;
import com.score.chatz.utils.NotificationUtils;
import com.score.chatz.utils.PreferenceUtils;
import com.score.chatz.utils.SenzUtils;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;
import com.score.senzc.pojos.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * First Activity after Splash screen!!!
 */
public class HomeActivity extends AppCompatActivity {

    private static final String TAG = HomeActivity.class.getName();

    //Ui elements
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private FloatingActionButton fab;

    private ImageView tabOneActive;
    private ImageView tabOneDeActive;
    private ImageView tabTwoActive;
    private ImageView tabTwoDeActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // setup activity, toolbar, actionbar and view pager, etc
        setupToolbar();
        setupActionBar();
        setupViewPager();
        setupTabLayouts();
        initFloatingButton();

        // Adding current user's name to the top right corner of the action bar!!!
        try {
            User user = PreferenceUtils.getUser(this);
            Log.i(TAG, "Registered User on Home page - " + user.getUsername());
            ((TextView) getSupportActionBar().getCustomView().findViewById(R.id.user_name)).setText("@" + user.getUsername());
            ((TextView) getSupportActionBar().getCustomView().findViewById(R.id.user_name)).setTypeface(Typeface.createFromAsset(getAssets(), "fonts/GeosansLight.ttf"), Typeface.BOLD);
        } catch (NoUserException ex) {
            Log.d(TAG, "No Registered User");
        }

        onNewIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void setupToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setCollapsible(false);
        toolbar.setOverScrollMode(Toolbar.OVER_SCROLL_NEVER);
        setSupportActionBar(toolbar);
    }

    private void setupActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setCustomView(getLayoutInflater().inflate(R.layout.home_action_bar, null));
        getSupportActionBar().setDisplayOptions(android.support.v7.app.ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
    }

    private void setupViewPager() {
        viewPager = (ViewPager) findViewById(R.id.pager);
        setupViewPager(viewPager);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {
            }

            @Override
            public void onPageScrolled(int pos, float arg1, int arg2) {
            }

            @Override
            public void onPageSelected(int pos) {
                if (pos == 0) {
                    fab.setVisibility(View.INVISIBLE);
                    activateTabOne();
                } else {
                    fab.setVisibility(View.VISIBLE);
                    activateTabTwo();
                }
            }
        });
    }

    private void initFloatingButton() {
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Click action
                Intent intent = new Intent(HomeActivity.this, AddUserActivity.class);
                startActivity(intent);
            }
        });
        fab.setVisibility(View.INVISIBLE);
    }

    private void setupTabLayouts() {
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(0).setCustomView(R.layout.home_rahas_tab);
        tabLayout.getTabAt(1).setCustomView(R.layout.home_friends_tab);
        tabOneActive = (ImageView) findViewById(R.id.tabOneActive);
        tabOneDeActive = (ImageView) findViewById(R.id.tabOneDeActive);
        tabTwoActive = (ImageView) findViewById(R.id.tabTwoActive);
        tabTwoDeActive = (ImageView) findViewById(R.id.tabTwoDeActive);
        activateTabOne();
    }

    private void activateTabOne() {
        tabOneActive.setVisibility(View.VISIBLE);
        tabOneDeActive.setVisibility(View.INVISIBLE);
        tabTwoActive.setVisibility(View.INVISIBLE);
        tabTwoDeActive.setVisibility(View.VISIBLE);
    }

    private void activateTabTwo() {
        tabOneActive.setVisibility(View.INVISIBLE);
        tabOneDeActive.setVisibility(View.VISIBLE);
        tabTwoActive.setVisibility(View.VISIBLE);
        tabTwoDeActive.setVisibility(View.INVISIBLE);

        // disable action bar icon
        ImageView delete = (ImageView) getSupportActionBar().getCustomView().findViewById(R.id.delete);
        delete.setVisibility(View.GONE);
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new LastItemChatListFragment(), getResources().getString(R.string.home_page_tab_one));
        adapter.addFragment(new FriendListFragment(), getResources().getString(R.string.home_page_tab_two));
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra("NOTIFICATION_ACCEPT")) {

            int notificationId = intent.getIntExtra("NOTIFICATION_ID", NotificationUtils.MESSAGE_NOTIFICATION_ID);
            String usernameToAdd = intent.getStringExtra("USERNAME_TO_ADD").trim();
            String senderPhone = intent.getStringExtra("SENDER_PHONE_NUMBER").trim();

            if (new SenzorsDbSource(this).getSecretUser(usernameToAdd).isActive()) {
                NotificationUtils.cancelNotification(notificationId, this);
                ActivityUtils.showCustomToast("This user has already been added", this);
            } else if (NetworkUtil.isAvailableNetwork(this)) {
                NotificationUtils.cancelNotification(notificationId, this);
                addUser(usernameToAdd, senderPhone);
            } else {
                ActivityUtils.showCustomToast(this.getResources().getString(R.string.no_internet), this);
            }
        }
    }

    public void addUser(String username, String phoneNumber){
        Intent smsReceivedIntent = IntentProvider.getAddUserIntent();
        smsReceivedIntent.putExtra("USERNAME_TO_ADD", username);
        smsReceivedIntent.putExtra("SENDER_PHONE_NUMBER", phoneNumber);
        this.sendBroadcast(smsReceivedIntent);
    }
}