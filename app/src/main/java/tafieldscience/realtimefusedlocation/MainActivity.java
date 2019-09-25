package tafieldscience.realtimefusedlocation;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

//import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient sFusedLocationClient;

    private LocationCallback sLocationCallback;
    private LocationRequest sLocationRequest;
    private GoogleApiClient sGoogleApiClient;
    private boolean apiconnectionstatus = false ;
    private int priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY ;
    private double Latitude = 0.0, Longitude = 0.0;
    private static final String TAG = "MainActivity";
    Button realtimegetGPS,metagetGPS ;
    TextView longitude,latitude ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        realtimegetGPS = findViewById(R.id.realtimegetGPS);
        metagetGPS = findViewById(R.id.metagetGPS);
        latitude = findViewById(R.id.latitude);
        longitude = findViewById(R.id.longitude);

        realtimegetGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start Realtime Activity
                Intent i = new Intent(MainActivity.this, RealtimeActivity.class);
                startActivity(i);
            }
        });

        metagetGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(apiconnectionstatus) {
                    locationSettingsRequest();
                }
            }
        });
    }

    /**
     * Function to connect googleapiclient
     * */
    private void connectGoogleClient() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int resultCode = googleAPI.isGooglePlayServicesAvailable(this);
        if (resultCode == ConnectionResult.SUCCESS) {
            sGoogleApiClient.connect();
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
            latitude.setText(getString(R.string.loading));
            longitude.setText(getString(R.string.loading));
            sFusedLocationClient.requestLocationUpdates(sLocationRequest, sLocationCallback, Looper.myLooper());
        }
    }

    /**
     * Build GoogleApiClient and connect
     */
    private synchronized void buildGoogleApiClient() {
        sFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        sGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {

                        // Creating a location request
                        sLocationRequest = new LocationRequest();
                        sLocationRequest.setPriority(priority);
                        sLocationRequest.setSmallestDisplacement(0);
                        sLocationRequest.setNumUpdates(1);

                        // FusedLocation callback
                        sLocationCallback = new LocationCallback() {
                            @Override
                            public void onLocationResult(final LocationResult locationResult) {
                                super.onLocationResult(locationResult);

                                Latitude = locationResult.getLastLocation().getLatitude();
                                Longitude = locationResult.getLastLocation().getLongitude();

                                if (Latitude == 0.0 && Longitude == 0.0) {
                                    requestLocationUpdate();
                                } else {
                                    // Update Textview
                                    latitude.setText(Double.toString(Latitude));
                                    longitude.setText(Double.toString(Longitude));
                                }
                            }
                        };

                        // Simple api status check
                        apiconnectionstatus = true ;
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
        builder.addLocationRequest(sLocationRequest);
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
                                rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
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
        sFusedLocationClient.removeLocationUpdates(sLocationCallback);
    }

    // Handle results of Location Service Dialog
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 214) {
            switch (resultCode){
                case Activity.RESULT_OK:
                {
                    // User enabled GPS start fused location
                    requestLocationUpdate();
                    break;
                }
                case Activity.RESULT_CANCELED:
                {
                    // The user was asked to change settings, but chose not to
                    Toast.makeText(getApplication(), "Location not enabled, user cancelled.", Toast.LENGTH_LONG).show();
                    break;
                }
                default:
                {
                    break;
                }
            }
        }
    }
}
