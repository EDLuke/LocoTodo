package com.lukez.locotodo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.google.android.gms.identity.intents.Address;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.schibsted.spain.parallaxlayerlayout.ParallaxLayerLayout;
import com.schibsted.spain.parallaxlayerlayout.SensorTranslationUpdater;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    static final int ADD_EVENT_REQUEST = 1;

    private GoogleMap mMap;
    private SensorTranslationUpdater mSensorTranslationUpdater;
    private ParallaxLayerLayout mParallax;
    private LocationManager mLocationManager;
    private FloatingSearchView mSearchView;

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

        //Get the location manager
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        //Link the search view
        mSearchView = (FloatingSearchView)this.findViewById(R.id.floating_search_view);
        mSearchView.setOnQueryChangeListener(onQueryChangeListener);
        mSearchView.setOnLeftMenuClickListener(onLeftMenuClickListener);
        mSearchView.setOnSearchListener(onSearchListener);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Check https://developer.android.com/training/permissions/requesting.html
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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

        //Get my current location
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        //mMap.setMyLocationEnabled(true);

        Location location_gps = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location location_wifi = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        Location location_passive = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

        Location[] locations = new Location[]{location_gps, location_wifi, location_passive};
        Location location_present = location_gps;
        for(Location location : locations){
            if(location != null)
                location_present = location;
        }

        //If still null, hard code to columbia university
        if(location_present == null) {
            location_present.setLatitude(40.80);
            location_present.setLongitude(73.96);
        }

        LatLng myLocation_latlng = new LatLng(location_present.getLatitude(), location_present.getLongitude());

        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation_latlng));


        mMap.moveCamera(CameraUpdateFactory.zoomTo(15.0f));

    }

    @Override
    public void onResume(){
        super.onResume();

        //Re-register the sensor manager
        mSensorTranslationUpdater.registerSensorManager();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == ADD_EVENT_REQUEST){
            if(resultCode == RESULT_OK){
                Bundle extras = data.getExtras();
                //Remove the current marker if we cancel the add activity
                if(extras.containsKey("Cancel"))
                    currentMarker.remove();
            }
        }
    }

    @Override
    public void onPause(){
        super.onPause();

        //Un-register the sensor manager
        mSensorTranslationUpdater.unregisterSensorManager();
    }

    Marker currentMarker;
    LatLng currentLatLng;

    private GoogleMap.SnapshotReadyCallback snapshotReadyCallback = new GoogleMap.SnapshotReadyCallback() {
        @Override
        public void onSnapshotReady(Bitmap bitmap) {
            //Create the dimension of crop image
            int mapWidth  = bitmap.getWidth();
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

    private List<android.location.Address> searchAddress(String address){
        Geocoder geocoder = new Geocoder(getApplicationContext());
        List<android.location.Address> addressList = null;
        try {
            addressList = geocoder.getFromLocationName(address, 10);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return addressList;
    }

    private void onMapClickHelper(LatLng latLng){
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
}
