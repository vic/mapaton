package org.novy.mapathon;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;
import com.firebase.client.Firebase;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class MainActivity extends AppCompatActivity implements LocationListener, GoogleApiClient.OnConnectionFailedListener {

    private static final int ACCESS_COURSE_LOCATION_PERMISSION = 1;

    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 1;
    private static final long MIN_TIME_BW_UPDATES = 1000;

    private static final int VIBRATE_ON_DISTANCE_BEFORE_TARGET = 100;

    /*
     *
     */

    protected GoogleApiClient mGoogleApiClient;

    private PlaceAutocompleteAdapter mAdapter;

    private AutoCompleteTextView mAutocompleteView;

    private static final LatLngBounds BOUNDS_CDMX = new LatLngBounds(
            new LatLng(-34.041458, 150.790100), new LatLng(-33.682247, 151.383362));

    Button buttonF01;
    Button buttonF02;
    Button buttonF03;

    Firebase mRef;

    String myLat = " ";
    String myLong = " ";
    private Location lastLocation;
    private JSONObject routesJSON;
    private LatLng targetPlace;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        monitorLocation();

        // Construct a GoogleApiClient for the {@link Places#GEO_DATA_API} using AutoManage
        // functionality, which automatically sets up the API client to handle Activity lifecycle
        // events. If your activity does not extend FragmentActivity, make sure to call connect()
        // and disconnect() explicitly.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0 /* clientId */, this)
                .addApi(Places.GEO_DATA_API)
                .build();

        setContentView(R.layout.activity_main);

        // Retrieve the AutoCompleteTextView that will display Place suggestions.
        mAutocompleteView = (AutoCompleteTextView)
                findViewById(R.id.autoCompleteTextView);

        // Register a listener that receives callbacks when a suggestion has been selected
        mAutocompleteView.setOnItemClickListener(mAutocompleteClickListener);

        // Set up the adapter that will retrieve suggestions from the Places Geo Data API that cover
        // the entire world.
        mAdapter = new PlaceAutocompleteAdapter(this, mGoogleApiClient, BOUNDS_CDMX,
                null);
        mAutocompleteView.setAdapter(mAdapter);

        // Set up the 'clear text' button that clears the text in the autocomplete view
        Button clearButton = (Button) findViewById(R.id.button_clear);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAutocompleteView.setText("");
            }
        });



        //Firebase
        Firebase.setAndroidContext(this);
        buttonF01 = (Button)findViewById(R.id.buttonF01);
        buttonF02 = (Button)findViewById(R.id.buttonF02);
        buttonF03 = (Button)findViewById(R.id.buttonF03);

        mRef = new Firebase("https://training-demos.firebaseio.com/");

        buttonF01.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String latitude = myLat;
                String longitude = myLong;
                String timestamp = new Date().toString();
                mRef.setValue("{flag: F01, latitude:" + latitude + ", longitude:"
                        + longitude + ", fecha:" + timestamp + "}");

                Toast.makeText(getApplicationContext(), "Incidente de Robo reportado",
                        Toast.LENGTH_SHORT).show();


            }
        });

        buttonF02.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String latitude = myLat;
                String longitude = myLong;
                String timestamp = new Date().toString();
                mRef.setValue("{flag: F02, latitude:" + latitude + ", longitude:"
                        + longitude + ", fecha:" + timestamp+"}");

                Toast.makeText(getApplicationContext(), "Incidente de Agresion reportado",
                        Toast.LENGTH_SHORT).show();

            }
        });

        buttonF03.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String latitude = myLat;
                String longitude = myLong;
                String timestamp = new Date().toString();
                mRef.setValue("{flag: F03, latitude:" + latitude + ", longitude:"
                        + longitude + ", fecha:" + timestamp+"}");

                Toast.makeText(getApplicationContext(), "Ruta marcada como destacada",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Listener that handles selections from suggestions from the AutoCompleteTextView that
     * displays Place suggestions.
     * Gets the place id of the selected item and issues a request to the Places Geo Data API
     * to retrieve more details about the place.
     *
     * @see com.google.android.gms.location.places.GeoDataApi#getPlaceById(com.google.android.gms.common.api.GoogleApiClient,
     * String...)
     */
    private AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            /*
             Retrieve the place ID of the selected item from the Adapter.
             The adapter stores each Place suggestion in a AutocompletePrediction from which we
             read the place ID and title.
              */
            final AutocompletePrediction item = mAdapter.getItem(position);
            final String placeId = item.getPlaceId();
            final CharSequence primaryText = item.getPrimaryText(null);

            /*
             Issue a request to the Places Geo Data API to retrieve a Place object with additional
             details about the place.
              */
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);

            Toast.makeText(getApplicationContext(), "Clicked: " + primaryText,
                    Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * Callback for results from a Places Geo Data API query that shows the first place result in
     * the details view on screen.
     */
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                places.release();
                return;
            }
            // Get the Place object from the buffer.
            final Place place = places.get(0);
            targetPlace = place.getLatLng();

            System.out.println("Going to place "+place);

            try {
                searchRoutes(place);
            } catch (JSONException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            places.release();
        }


    };

    private void searchRoutes(Place place) throws JSONException {
        JSONObject params = new JSONObject();
        if (lastLocation == null) {
            return;
        }

        JSONObject fromLocation = new JSONObject();
        fromLocation.put("lat", lastLocation.getLatitude());
        fromLocation.put("lon", lastLocation.getLongitude());

        JSONObject toLocation = new JSONObject();
        toLocation.put("lat", place.getLatLng().latitude);
        toLocation.put("lon", place.getLatLng().longitude);


        params.put("fromLocation", fromLocation);
        params.put("toLocation", toLocation);

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, "http://mapaton.localtunnel.me/routesNearTrip", params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        System.out.println(response);
                        routesJSON = response;
                        try {
                            renderRoutesSelect();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println(error);
                    }
                });

        MySingleton.getInstance(this).addToRequestQueue(req);
    }

    private void renderRoutesSelect() throws JSONException {
        JSONArray routes = this.routesJSON.getJSONArray("routes");
        JSONObject first = routes.getJSONObject(0);
        NetworkImageView mNetworkImageView = (NetworkImageView) findViewById(R.id.networkImageView);
        ImageLoader mImageLoader = MySingleton.getInstance(this).getImageLoader();
        mNetworkImageView.setImageUrl(first.getString("imageURL")+"&zoom=16", mImageLoader);
    }

    private void monitorLocation() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.VIBRATE},
                    ACCESS_COURSE_LOCATION_PERMISSION);
            return;
        }
        lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                MIN_TIME_BW_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        LogService.log("PERMISSIONS " + permissions + " GRANTS " + grantResults);
    }

    @Override
    public void onLocationChanged(Location location) {
        this.lastLocation = location;
        double lat = location.getLatitude();
        double lng = location.getLongitude();

        TextView textView = (TextView) this.findViewById(R.id.here);
        if (targetPlace == null) {
            textView.setText("");
        } else {
            double distance = getDistanceFromLatLonInM(lat, lng, targetPlace.latitude, targetPlace.longitude);
            textView.setText("Faltan "+Math.round(distance)+" metros para bajar");
            if (distance <= VIBRATE_ON_DISTANCE_BEFORE_TARGET) {
                vibrate();
            }
        }
    }

    private void vibrate() {
        Toast.makeText(getApplicationContext(), "BAJAN !!",
                Toast.LENGTH_SHORT).show();

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(new long[]{1000, 1000, 2000, 1000, 3000}, -1);
    }

    public static float getDistanceFromLatLonInM(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        float dist = (float) (earthRadius * c);
        return dist;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    /**
     * Called when the Activity could not connect to Google Play services and the auto manager
     * could resolve the error automatically.
     * In this case the API is not available and notify the user.
     *
     * @param connectionResult can be inspected to determine the cause of the failure
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        // TODO(Developer): Check error code and notify the user of error state and resolution.
        Toast.makeText(this,
                "Could not connect to Google API Client: Error " + connectionResult.getErrorCode(),
                Toast.LENGTH_SHORT).show();
    }
}

