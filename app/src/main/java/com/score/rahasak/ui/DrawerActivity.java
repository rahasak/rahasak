package com.score.rahasak.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.score.rahasak.R;
import com.score.rahasak.db.SenzorsDbSource;
import com.score.rahasak.exceptions.NoUserException;
import com.score.rahasak.interfaces.IFragmentTransitionListener;
import com.score.rahasak.pojo.DrawerItem;
import com.score.rahasak.utils.PreferenceUtils;
import com.score.senzc.pojos.User;

import java.util.ArrayList;


public class DrawerActivity extends AppCompatActivity implements View.OnClickListener, IFragmentTransitionListener {

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private RelativeLayout drawerContainer;
    private ListView drawerListView;
    private ActionBarDrawerToggle actionBarDrawerToggle;

    private ImageView homeView;
    private TextView titleText;
    private TextView homeUserText;

    private RelativeLayout aboutLayout;
    private TextView aboutText;

    // drawer components
    private ArrayList<DrawerItem> drawerItemList;
    private DrawerAdapter drawerAdapter;

    private Typeface typeface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_layout);
        typeface = Typeface.createFromAsset(getAssets(), "fonts/GeosansLight.ttf");

        setupToolbar();
        setupActionBar();
        setupDrawer();
        initDrawerList();

        // load initial fragment
        if (new SenzorsDbSource(this).isAvailableUsers()) loadRahas();
        else loadInvite();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra("SENDER")) {
            loadFriends();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void setupDrawer() {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerContainer = (RelativeLayout) findViewById(R.id.drawer_container);

        final LinearLayout frame = (LinearLayout) findViewById(R.id.content_view);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name, R.string.app_name) {
            @SuppressLint("NewApi")
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                float moveFactor = (drawerListView.getWidth() * slideOffset);

                frame.setTranslationX(moveFactor);
            }
        };
        drawerLayout.addDrawerListener(actionBarDrawerToggle);

        homeUserText = (TextView) findViewById(R.id.home_user_text);
        homeUserText.setTypeface(typeface);
        try {
            User user = PreferenceUtils.getUser(this);
            homeUserText.setText("@" + user.getUsername());
        } catch (NoUserException ex) {
            Log.d("TAG", "No Registered User");
        }

        aboutLayout = (RelativeLayout) findViewById(R.id.about_layout);
        aboutLayout.setOnClickListener(this);

        aboutText = (TextView) findViewById(R.id.about_text);
        aboutText.setTypeface(typeface);
    }

    /**
     * Initialize Drawer list
     */
    private void initDrawerList() {
        // initialize drawer content
        // need to determine selected item according to the currently selected sensor type
        drawerItemList = new ArrayList();
        drawerItemList.add(new DrawerItem("Secrets", R.drawable.rahaslogo, R.drawable.rahaslogo, true));
        drawerItemList.add(new DrawerItem("Friends", R.drawable.rahaslogo, R.drawable.rahaslogo, false));
        drawerItemList.add(new DrawerItem("Invite", R.drawable.rahaslogo, R.drawable.rahaslogo, false));

        drawerAdapter = new DrawerAdapter(this, drawerItemList);
        drawerListView = (ListView) findViewById(R.id.drawer);

        if (drawerListView != null)
            drawerListView.setAdapter(drawerAdapter);

        drawerListView.setOnItemClickListener(new DrawerItemClickListener());
    }

    private void setupToolbar() {
        toolbar = (Toolbar) findViewById(R.id.home_toolbar);
        setSupportActionBar(toolbar);
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();

        ActionBar.LayoutParams params = new
                ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.MATCH_PARENT, Gravity.CENTER);
        actionBar.setCustomView(getLayoutInflater().inflate(R.layout.home_action_bar_layout, null), params);
        actionBar.setDisplayOptions(android.support.v7.app.ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setDisplayShowCustomEnabled(true);

        homeView = (ImageView) findViewById(R.id.home_view);
        homeView.setOnClickListener(this);

        titleText = (TextView) findViewById(R.id.title_text);
        titleText.setTypeface(typeface, Typeface.BOLD);
    }

    @Override
    public void onClick(View v) {
        if (v == homeView) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(drawerContainer);
            } else {
                drawerLayout.openDrawer(drawerContainer);
            }
        } else if (v == aboutLayout) {
            loadAbout();

        }
    }

    @Override
    public void onTransition(String type) {
        if (type.equalsIgnoreCase("secret")) {
            loadRahas();
        } else if (type.equalsIgnoreCase("friends")) {
            loadFriends();
        } else if (type.equalsIgnoreCase("invite")) {
            loadInvite();
        }
    }

    /**
     * Drawer click event handler
     */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // Highlight the selected item, update the title, and close the drawer
            // update selected item and title, then close the drawer
            drawerLayout.closeDrawer(drawerContainer);

            if (position == 0) {
                loadRahas();
            } else if (position == 1) {
                loadFriends();
            } else if (position == 2) {
                loadInvite();
            }
        }
    }

    /**
     * Load my sensor list fragment
     */
    private void loadRahas() {
        titleText.setText("Secrets");
        clearAboutText();

        unSelectDrawerItems();
        drawerItemList.get(0).setSelected(true);
        drawerAdapter.notifyDataSetChanged();

        SecretListFragment fragment = new SecretListFragment();

        // fragment transitions
        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main, fragment);
        transaction.commit();
    }

    /**
     * Load my sensor list fragment
     */
    private void loadFriends() {
        titleText.setText("Friends");
        clearAboutText();

        unSelectDrawerItems();
        drawerItemList.get(1).setSelected(true);
        drawerAdapter.notifyDataSetChanged();

        FriendListFragment fragment = new FriendListFragment();

        // fragment transitions
        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main, fragment);
        transaction.commit();
    }

    public void loadInvite() {
        titleText.setText("Invite");
        clearAboutText();

        unSelectDrawerItems();
        drawerItemList.get(2).setSelected(true);
        drawerAdapter.notifyDataSetChanged();

        InviteFragment fragment = new InviteFragment();

        // fragment transitions
        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main, fragment);
        transaction.commit();
    }

    private void loadAbout() {
        titleText.setText("About");
        selectAboutText();

        drawerLayout.closeDrawer(drawerContainer);
        unSelectDrawerItems();
        drawerAdapter.notifyDataSetChanged();

        AboutFragment fragment = new AboutFragment();

        // fragment transitions
        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main, fragment);
        transaction.commit();
    }

    private void unSelectDrawerItems() {
        //  reset content in drawer list
        for (DrawerItem drawerItem : drawerItemList) {
            drawerItem.setSelected(false);
        }
    }

    private void selectAboutText() {
        aboutText.setTextColor(Color.parseColor("#F88F8C"));
        aboutText.setTypeface(typeface, Typeface.BOLD);
    }

    private void clearAboutText() {
        aboutText.setTextColor(Color.parseColor("#636363"));
        aboutText.setTypeface(typeface, Typeface.NORMAL);
    }

}
