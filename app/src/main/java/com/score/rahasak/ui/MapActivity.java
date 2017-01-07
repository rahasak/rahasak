package com.score.rahasak.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.score.rahasak.R;
import com.score.senzc.pojos.Senz;

/**
 * Senz map activity
 */
public class MapActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MapActivity.class.getName();

    private ImageView btnBack;

    private Typeface typeface;

    private GoogleMap map;
    private Marker marker;

    private Toolbar toolbar;
    private ActionBar actionBar;

    private GoogleApiClient mGoogleApiClient;
    private LatLng thisUserLatLng;
    private Senz thisSenz;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.secret_list_fragment_layout);

        initLoc();
        initExtra();
        initUi();
        initToolbar();
        initActionBar();
        initMap();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // no permission
            Log.e(TAG, "No location permission");
        } else {
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (location != null) {
                displayMyLocation(new LatLng(location.getLatitude(), location.getLongitude()));
            } else {
                Log.e(TAG, "No location available");
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mGoogleApiClient != null) mGoogleApiClient.disconnect();
    }

    private void initLoc() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

            // connect to location service
            mGoogleApiClient.connect();
        }
    }

    private void initExtra() {
        if (getIntent().hasExtra("SENZ")) {
            thisSenz = getIntent().getExtras().getParcelable("SENZ");

            // location coordinate
            double lat = Double.parseDouble(thisSenz.getAttributes().get("lat"));
            double lan = Double.parseDouble(thisSenz.getAttributes().get("lon"));
            thisUserLatLng = new LatLng(lat, lan);
        }
    }

    private void initUi() {
        typeface = Typeface.createFromAsset(getAssets(), "fonts/GeosansLight.ttf");
    }

    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.map_toolbar);
        toolbar.setCollapsible(false);
        toolbar.setOverScrollMode(Toolbar.OVER_SCROLL_NEVER);
        setSupportActionBar(toolbar);
    }

    private void initActionBar() {
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setCustomView(getLayoutInflater().inflate(R.layout.map_header, null));
        actionBar.setDisplayOptions(android.support.v7.app.ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setDisplayShowCustomEnabled(true);

        TextView header = ((TextView) findViewById(R.id.title));
        header.setTypeface(typeface, Typeface.BOLD);
        header.setText("@" + thisSenz.getSender().getUsername());

        btnBack = (ImageView) getSupportActionBar().getCustomView().findViewById(R.id.back_btn);
        btnBack.setOnClickListener(this);
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call  once when {@link #map} is not null.
     * <p/>
     * If it isn't installed {@link com.google.android.gms.maps.SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void initMap() {
        // Do a null check to confirm that we have not already instantiated the map
        if (map == null) {
            // Try to obtain the map from the SupportMapFragment
            // disable zoom controller
            Log.d(TAG, "SetUpMapIfNeeded: map is empty, so set up it");
            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            map = googleMap;
                            // Check if we were successful in obtaining the map.
                            if (map != null) {
                                moveToLocation();
                            }
                        }
                    });
        }
    }

    /**
     * Move map to given location
     */
    private void moveToLocation() {
        Log.d(TAG, "MoveToLocation: move map to given location");

        // remove existing markers
        if (marker != null) marker.remove();

        // add location marker
        try {
            marker = map.addMarker(new MarkerOptions().position(this.thisUserLatLng).title("@" + thisSenz.getSender().getUsername()).icon(BitmapDescriptorFactory.fromResource(R.drawable.pin2)));
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(this.thisUserLatLng, 10));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid location", Toast.LENGTH_LONG).show();
            Log.d(TAG, "MoveToLocation: invalid lat lon parameters");
        }
    }

    /**
     * Add marker to my location and set up zoom level
     *
     * @param latLng
     */
    private void displayMyLocation(LatLng latLng) {
        marker = map.addMarker(new MarkerOptions().position(latLng).title("@Me").icon(BitmapDescriptorFactory.fromResource(R.drawable.pin3)));

        // set zoom level
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(latLng);
        builder.include(this.thisUserLatLng);
        LatLngBounds bounds = builder.build();

        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 200);
        map.animateCamera(cu);
    }

    @Override
    public void onClick(View v) {
        if (v == btnBack) {
            finish();
        }
    }

}
