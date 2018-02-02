package com.example.tswalano.mapme;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import transportapisdk.TransportApiClient;
import transportapisdk.TransportApiClientSettings;
import transportapisdk.TransportApiResult;
import transportapisdk.models.Direction;
import transportapisdk.models.Journey;
import transportapisdk.models.Leg;

import static junit.framework.Assert.assertTrue;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    ListView listView;
    List<String> array = new ArrayList<>();
    List<List<Double>> coordinates = new ArrayList<List<Double>>();
    ArrayAdapter<String> adapter;

    private static String clientId = "60c0c3ba-c7cf-4e7a-94e9-cd595c5576c4";
    private static String clientSecret = "9fiGFYD460MLqz5LYTFLzz5HB4vYjV/JZ5TqPGi5rSA=";
    private static TransportApiClient client;

    private static double defaultStartLatitude = -33.94393;
    private static double defaultStartLongitude = 18.37755;
    private static double defaultEndLatitude = -33.91252;
    private static double defaultEndLongitude = 18.41489;

    private static String defaultOmitMode = "Bus";
    private static String defaultNoExcludes = "";

    List<List<Double>> geo;

    //Google Maps
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(MainActivity.this);

        new BackgroundTask().execute(clientId, clientSecret);
        listView = findViewById(R.id.list_item);
    }

    public void onMapSearch(View view) {
        EditText locationSearch = (EditText) findViewById(R.id.editLocation);
        String location = locationSearch.getText().toString();
        List<Address>addressList = null;
        mMap.clear();
        array.clear();
        listView.setVisibility(View.GONE);

        if (location != null || !location.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(location, 1);

            } catch (IOException e) {
                e.printStackTrace();
            }
            Address address = addressList.get(0);
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            mMap.addMarker(new MarkerOptions().position(latLng).title("Marker"));
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

            Toast.makeText(this, "Lat "+address.getLatitude()+" Lng " + address.getLongitude(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

//      Add a marker in Sydney and move the camera
        LatLng startLocation = new LatLng(defaultStartLatitude, defaultStartLongitude);
        mMap.addMarker(new MarkerOptions().position(startLocation).title("My Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(startLocation));
        mMap.getUiSettings().setZoomControlsEnabled(true);

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(startLocation));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

        LatLng endLocation = new LatLng(defaultEndLatitude, defaultEndLongitude);
        mMap.addMarker(new MarkerOptions().position(endLocation).title("Destination"));
    }

    private class BackgroundTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {

            String clientId = strings[0];
            String clientSecret = strings[1];

            client = new TransportApiClient(new TransportApiClientSettings(clientId, clientSecret));
            //listView.setAdapter(adapter);

            PostJourney_DefaultValues_IsSuccess();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, array);
            listView.setAdapter(adapter);

            List<Double> myGeo;
            int x = 0;
            PolylineOptions polylineOptions = new PolylineOptions();
            ArrayList<LatLng> coordList = new ArrayList<>();

            for (int i = 0; i < geo.size(); i++) {
                myGeo = geo.get(i);

                //Declare Latitude and Longitude
                Double longitude = myGeo.get(0);
                Double latitude = myGeo.get(1);

                // Adding points to ArrayList
                coordList.add(new LatLng(latitude, longitude));
            }

            // Create polyline options with existing LatLng ArrayList
            polylineOptions.addAll(coordList);
            polylineOptions
                    .width(10)
                    .color(Color.GRAY);

            // Adding multiple points in map using polyline and arraylist
            mMap.addPolyline(polylineOptions);

            super.onPostExecute(aVoid);
        }
    }

    public void PostJourney_DefaultValues_IsSuccess() {
        TransportApiResult<Journey> journey = client.postJourney(null, defaultStartLatitude, defaultStartLongitude, defaultEndLatitude, defaultEndLongitude, defaultNoExcludes);

        assertTrue(journey.isSuccess);
        assertTrue(journey.data.getId() != null);

        List<List<Double>> coordinates1 = journey.data.getGeometry().getCoordinates();
        List<Double> coordinates2 = journey.data.getGeometry().getCoordinates().get(1);

        List<Leg> legs = journey.data.getItineraries().get(0).getLegs();
        List<Direction> directions = legs.get(0).getDirections();

        geo = legs.get(0).getGeometry().getCoordinates();

        int count = 0;

        do {
            array.add(directions.get(count).getInstruction());
            System.out.println("Geometry " + geo.get(count));
            count++;
        } while (count < directions.size());

        System.out.println(directions.get(0).getInstruction());
        System.out.println("Array " + array.size());
    }

//    void direction(GoogleMap mMap){
//        // Add a thin red line from London to New York.
//        Polyline line = mMap.addPolyline(new PolylineOptions()
//                .add(new LatLng(defaultStartLatitude , defaultStartLongitude),
//                        new LatLng(-33.94395 , 18.37755),
//                        new LatLng(-33.94399,  18.37745),
//                        new LatLng(-33.9441, 18.37754))
//                .width(5)
//                .color(Color.RED));
//    }
}
