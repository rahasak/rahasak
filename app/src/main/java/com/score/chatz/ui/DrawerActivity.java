package com.score.chatz.ui;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.score.chatz.R;
import com.score.chatz.pojo.DrawerItem;

import java.util.ArrayList;

/**
 * Created by eranga on 12/4/16.
 */

public class DrawerActivity extends AppCompatActivity implements View.OnClickListener {

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private RelativeLayout drawerContainer;
    private ListView drawerListView;
    private ActionBarDrawerToggle actionBarDrawerToggle;

    private ImageView homeView;
    private TextView titleText;
    private TextView homeUserText;

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
        loadRahas();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void setupDrawer() {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerContainer = (RelativeLayout) findViewById(R.id.drawer_container);
        //drawerLayout.setScrimColor(getResources().getColor(android.R.color.transparent));

        final LinearLayout frame = (LinearLayout) findViewById(R.id.content_view);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name, R.string.app_name) {
            @SuppressLint("NewApi")
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                float moveFactor = (drawerListView.getWidth() * slideOffset);
                float lastTranslate = 0.0f;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    frame.setTranslationX(moveFactor);
                } else {
                    TranslateAnimation anim = new TranslateAnimation(lastTranslate, moveFactor, 0.0f, 0.0f);
                    anim.setDuration(0);
                    anim.setFillAfter(true);
                    frame.startAnimation(anim);

                    lastTranslate = moveFactor;
                }
            }
        };
        drawerLayout.addDrawerListener(actionBarDrawerToggle);

        homeUserText = (TextView) findViewById(R.id.home_user_text);
        homeUserText.setTypeface(typeface);
        homeUserText.setText("@lambda");
    }

    /**
     * Initialize Drawer list
     */
    private void initDrawerList() {
        // initialize drawer content
        // need to determine selected item according to the currently selected sensor type
        drawerItemList = new ArrayList();
        drawerItemList.add(new DrawerItem("Rahas", R.drawable.rahaslogo, R.drawable.rahaslogo, true));
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

            //  reset content in drawer list
            for (DrawerItem drawerItem : drawerItemList) {
                drawerItem.setSelected(false);
            }

            if (position == 0) {
                drawerItemList.get(0).setSelected(true);
                titleText.setText("Rahas");
                loadRahas();
            } else if (position == 1) {
                drawerItemList.get(1).setSelected(true);
                titleText.setText("Friends");
                loadFriends();
            } else if (position == 2) {
                drawerItemList.get(2).setSelected(true);
                titleText.setText("Invite");
                loadInvite();
            }

            drawerAdapter.notifyDataSetChanged();
        }
    }


    /**
     * Load my sensor list fragment
     */
    private void loadRahas() {
        RecentChatListFragment fragment = new RecentChatListFragment();

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
        FriendListFragment fragment = new FriendListFragment();

        // fragment transitions
        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main, fragment);
        transaction.commit();
    }

    private void loadInvite() {
        InviteFragment fragment = new InviteFragment();

        // fragment transitions
        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main, fragment);
        transaction.commit();
    }

}
