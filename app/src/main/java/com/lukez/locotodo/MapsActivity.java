package com.lukez.locotodo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.location.Geocoder;
import android.location.Location;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.lukez.locoto_service.LocationUpdateService;
import com.schibsted.spain.parallaxlayerlayout.ParallaxLayerLayout;
import com.schibsted.spain.parallaxlayerlayout.SensorTranslationUpdater;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static com.lukez.locoto_service.LocationUpdateService.LOCATION_UPDATE_ACTION;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    static final int ADD_EVENT_REQUEST = 1;

    private GoogleMap mMap;
    private SensorTranslationUpdater mSensorTranslationUpdater;
    private ParallaxLayerLayout mParallax;
    private FloatingSearchView mSearchView;
    private LocationReceiver mLocationReceiver;
    private FloatingActionButton mFABMain, mFABAdd, mFABView;

    //Animation for the FABs
    private Animation fab_open, fab_close, rotate_downward, rotate_upward;
    private boolean isFABMainOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //Set to full screen
//        getWindow().getDecorView().setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

        //Link the sensor translation updater with the parallax layer
        mParallax = (ParallaxLayerLayout) (this.findViewById(R.id.parallax));
        mSensorTranslationUpdater = new SensorTranslationUpdater(this);
        mParallax.setTranslationUpdater(mSensorTranslationUpdater);

        //Link the search view
        mSearchView = (FloatingSearchView) this.findViewById(R.id.floating_search_view);
        mSearchView.setOnQueryChangeListener(onQueryChangeListener);
        mSearchView.setOnLeftMenuClickListener(onLeftMenuClickListener);
        mSearchView.setOnSearchListener(onSearchListener);

        //Link the FABs
        mFABMain = (FloatingActionButton) this.findViewById(R.id.floating_main_btn);
        mFABMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickFABMain(view);
            }
        });
        mFABAdd  = (FloatingActionButton) this.findViewById(R.id.floating_add_btn);
        mFABAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickFABAdd(view);
            }
        });
        mFABView = (FloatingActionButton) this.findViewById(R.id.floating_view_btn);
        mFABView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickFABView(view);
            }
        });
        fab_open        = AnimationUtils.loadAnimation(this, R.anim.fab_open);
        fab_close       = AnimationUtils.loadAnimation(this, R.anim.fab_close);
        rotate_downward = AnimationUtils.loadAnimation(this, R.anim.rotate_downward);
        rotate_upward   = AnimationUtils.loadAnimation(this, R.anim.rotate_upward);

        //Register the location broadcast receiver
        mLocationReceiver = new LocationReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocationReceiver, new IntentFilter(LOCATION_UPDATE_ACTION));

        //Start the location update activity
        Intent locationUpdateService = new Intent(this, LocationUpdateService.class);
        this.startService(locationUpdateService);


    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Register the onMapClickListener
        mMap.setOnMapClickListener(onMapClickListener);

        if (currentLocation == null)
            return;

        LatLng myLocation_latlng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation_latlng));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15.0f));

    }

    @Override
    public void onResume() {
        super.onResume();

        //Re-register the sensor manager
        mSensorTranslationUpdater.registerSensorManager();

        //Re-register the broadcast receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocationReceiver, new IntentFilter(LOCATION_UPDATE_ACTION));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ADD_EVENT_REQUEST) {
            if (resultCode == RESULT_OK) {
                if(data != null) {
                    Bundle extras = data.getExtras();
                    //Remove the current marker if we cancel the add activity
                    if (extras != null && extras.containsKey("Cancel"))
                        currentMarker.remove();
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        //Un-register the sensor manager
        mSensorTranslationUpdater.unregisterSensorManager();
    }

    @Override
    public void onStop() {
        super.onStop();

        //Un-register the broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocationReceiver);
    }

    Marker currentMarker;
    LatLng currentLatLng;
    Location currentLocation;

    private GoogleMap.SnapshotReadyCallback snapshotReadyCallback = new GoogleMap.SnapshotReadyCallback() {
        @Override
        public void onSnapshotReady(Bitmap bitmap) {
            //Create the dimension of crop image
            int mapWidth = bitmap.getWidth();
            int mapHeight = bitmap.getHeight();

            //Create the resized(cropped snapshot)
            Bitmap resizedSnapshot = Bitmap.createBitmap(bitmap, 0, mapHeight / 5, mapWidth, mapHeight / 5 * 3);

            //Compress the bitmap to byte array
            ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
            resizedSnapshot.compress(Bitmap.CompressFormat.PNG, 100, baoStream);
            byte[] compressedBytes = baoStream.toByteArray();

            //Start the activity
            Intent intentAdd = new Intent(getApplicationContext(), AddActivity.class);
            intentAdd.putExtra("LatLng", currentLatLng);
            intentAdd.putExtra("BMP", compressedBytes);
            startActivityForResult(intentAdd, ADD_EVENT_REQUEST);
        }
    };

    private List<android.location.Address> searchAddress(String address) {
        Geocoder geocoder = new Geocoder(getApplicationContext());
        List<android.location.Address> addressList = null;
        try {
            addressList = geocoder.getFromLocationName(address, 10);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return addressList;
    }

    private void onMapClickHelper(LatLng latLng) {
        //Add the temporary marker
        currentMarker = mMap.addMarker(new MarkerOptions().position(latLng));

        //Store the latlong
        currentLatLng = latLng;

        //Center map to the marker's location
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng), new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                //Take the snapshot
                mMap.snapshot(snapshotReadyCallback);
            }

            @Override
            public void onCancel() {

            }
        });
    }

    private GoogleMap.OnMapClickListener onMapClickListener = new GoogleMap.OnMapClickListener() {
        @Override
        public void onMapClick(LatLng latLng) {
            onMapClickHelper(latLng);
        }
    };

    private FloatingSearchView.OnQueryChangeListener onQueryChangeListener = new FloatingSearchView.OnQueryChangeListener() {
        @Override
        public void onSearchTextChanged(String oldQuery, String newQuery) {


        }
    };

    private FloatingSearchView.OnSearchListener onSearchListener = new FloatingSearchView.OnSearchListener() {
        @Override
        public void onSuggestionClicked(SearchSuggestion searchSuggestion) {

        }

        @Override
        public void onSearchAction(String currentQuery) {
            android.location.Address address = (searchAddress(currentQuery)).get(0);
            LatLng latlng = new LatLng(address.getLatitude(), address.getLongitude());
            onMapClickHelper(latlng);
        }
    };

    private FloatingSearchView.OnLeftMenuClickListener onLeftMenuClickListener = new FloatingSearchView.OnLeftMenuClickListener() {
        @Override
        public void onMenuOpened() {

        }

        @Override
        public void onMenuClosed() {

        }
    };

    protected void startMapSync() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    public class LocationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (currentLocation == null) {
                currentLocation = (Location) intent.getExtras().get("Location");
                startMapSync();
            } else {
                currentLocation = (Location) intent.getExtras().get("Location");
                if (mMap != null)
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude())));
            }
        }
    }

    public void onClickFABMain(View v) {
        if(isFABMainOpen){
            mFABMain.startAnimation(rotate_downward);
            mFABAdd.startAnimation(fab_close);
            mFABView.startAnimation(fab_close);
            mFABAdd.setClickable(false);
            mFABView.setClickable(false);
        }
        else{
            mFABMain.startAnimation(rotate_upward);
            mFABAdd.startAnimation(fab_open);
            mFABView.startAnimation(fab_open);
            mFABAdd.setClickable(true);
            mFABView.setClickable(true);
        }
        isFABMainOpen = !isFABMainOpen;
    }

    public void onClickFABView(View v){
        Intent intent = new Intent(getApplicationContext(), EventListActivity.class);
        startActivity(intent);
    }

    public void onClickFABAdd(View v){

    }
}
