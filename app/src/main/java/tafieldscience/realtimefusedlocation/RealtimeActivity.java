package tafieldscience.realtimefusedlocation;

import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCanceledListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class RealtimeActivity extends AppCompatActivity {
    private FusedLocationProviderClient mFusedLocationClient;

    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;

    private long interval = 30000;
    private long fastestInterval = 10000;
    private int priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY ;
    private int numberOfUpdates;
    private double Latitude = 0.0, Longitude = 0.0;
    private ArrayAdapter arrayAdapter;
    private ArrayList update = new ArrayList<String>();
    private static final String TAG = "RealtimeActivity";

    // Server url for location updates
    public static String dburl = "http://192.168.42.85/RealtimeLocation/addlocation.php",uniqueId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_realtime);

        //Creating listview adapter
        arrayAdapter = new ArrayAdapter<String>(this,R.layout.list_layout,update);

        ListView listView = findViewById(R.id.listing_items);
        listView.setAdapter(arrayAdapter);

        //Unique ID for an app instance
        uniqueId = UUID.randomUUID().toString();
    }

    /**
     * Function to connect googleapiclient
     */
    private void connectGoogleClient() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int resultCode = googleAPI.isGooglePlayServicesAvailable(this);
        if (resultCode == ConnectionResult.SUCCESS) {
            mGoogleApiClient.connect();
        } else {
            int REQUEST_GOOGLE_PLAY_SERVICE = 988;
            googleAPI.getErrorDialog(this, resultCode, REQUEST_GOOGLE_PLAY_SERVICE);
        }
    }

    /**
     * Function to start FusedLocation updates
     */
    public void requestLocationUpdate() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        }
    }

    /**
     * Build GoogleApiClient and connect
     */
    private synchronized void buildGoogleApiClient() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        // Creating a location request
                        mLocationRequest = new LocationRequest();
                        mLocationRequest.setInterval(interval);
                        mLocationRequest.setFastestInterval(fastestInterval);
                        mLocationRequest.setPriority(priority);
                        mLocationRequest.setSmallestDisplacement(0);

                        // FusedLocation callback
                        mLocationCallback = new LocationCallback() {
                            @Override
                            public void onLocationResult(final LocationResult locationResult) {
                                super.onLocationResult(locationResult);

                                Latitude = locationResult.getLastLocation().getLatitude();
                                Longitude = locationResult.getLastLocation().getLongitude();

                                if (Latitude == 0.0 && Longitude == 0.0) {
                                    requestLocationUpdate();
                                } else {
                                    // Check internet permission and
                                    // Send Location to Database
                                    if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
                                        saveLocation(uniqueId, Double.toString(Latitude), Double.toString(Longitude));
                                    }
                                }
                            }
                        };

                        // Call location settings function to enable gps
                        locationSettingsRequest();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        connectGoogleClient();
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

                    }
                })
                .addApi(LocationServices.API)
                .build();

        // Connect googleapiclient after build
        connectGoogleClient();
    }

    /**
     * Function to request Location Service Dialog
     */
    private void locationSettingsRequest(){
        SettingsClient mSettingsClient = LocationServices.getSettingsClient(this);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        LocationSettingsRequest mLocationSettingsRequest = builder.build();

        mSettingsClient
                .checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(locationSettingsResponse -> {
                    // Start FusedLocation if GPS is enabled
                    requestLocationUpdate();
                })
                .addOnFailureListener(e -> {
                    // Show enable GPS Dialog and handle dialog buttons
                    int statusCode = ((ApiException) e).getStatusCode();
                    switch (statusCode) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                int REQUEST_CHECK_SETTINGS = 214;
                                ResolvableApiException rae = (ResolvableApiException) e;
                                rae.startResolutionForResult(RealtimeActivity.this, REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException sie) {
                                showLog("Unable to Execute Request");
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            showLog("Location Settings are Inadequate, and Cannot be fixed here. Fix in Settings");
                    }
                })
                .addOnCanceledListener(new OnCanceledListener() {
                    @Override
                    public void onCanceled() {
                        showLog("Canceled No Thanks");
                    }
                });
    }

    private void showLog(String message) {
        Log.e(TAG, "" + message);
    }

    @Override
    public void onResume(){
        super.onResume();
        buildGoogleApiClient();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            showLog("Canceled Location Thanks");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_stop) {
            // Stop location updates and disconnect googleapiclient
            // Clear listview
            try {
                mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                mGoogleApiClient.disconnect();
                update.clear();
                arrayAdapter.notifyDataSetChanged();
                Toast.makeText(getApplication(), "Location Stopped.", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Function to Save location to db and update listview
     * @param uniqueId unique ID for location in database
     * @param latitude latitude from fused location
     * @param longitude longitude from fused location
     */
    public void saveLocation(String uniqueId, String latitude, String longitude){
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, dburl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // Post response from server in JSON
                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if(error) {
                        update.add(Calendar.getInstance().getTime() + " - Location Updated");
                        arrayAdapter.notifyDataSetChanged();
                    }else{
                        update.add(Calendar.getInstance().getTime() + " - Update Failed");
                        arrayAdapter.notifyDataSetChanged();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                showLog("Volley error: "+error.toString());
            }
        }){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<String, String>();
                // Adding parameters to post request
                params.put("uniqueId",uniqueId);
                params.put("latitude",latitude);
                params.put("longitude",longitude);
                return params;
            }
        };

        // Adding request to request queue
        requestQueue.add(stringRequest);
    }
}
