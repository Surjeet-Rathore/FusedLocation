package com.webaddict.locationlibrary.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.webaddict.locationlibrary.base.BaseClass;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

/**
 * Created by lenovo on 5/2/2017.
 */

public class FusedLocationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private long UPDATE_INTERVAL_IN_MILLISECONDS = 0;
    /**
     * The fastest rate for active location updates. Updates will never be more frequent
     * than this value.
     */

    private Date gpsDate;

    /**
     * The entry point to Google Play Services.
     */
    private GoogleApiClient mGoogleApiClient;
    /**
     * Contains parameters used by {@link com.google.android.gms.location.FusedLocationProviderApi}.
     */
    private LocationRequest mLocationRequest;
    private Location lastLocation = null;
    private double calculatedSpeed = 0;
    SharedPreferences sharedPreferences;

    double currentLatitude, currentLongitude, previousLatitude = 0.0, previousLongitude = 0.0;
    double travelledDistance = 0.0;

    private String TAG = FusedLocationService.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
        sharedPreferences = getSharedPreferences("LocationPreferences", Context.MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.hasExtra("LocationInterval")) {
            UPDATE_INTERVAL_IN_MILLISECONDS = intent.getLongExtra("LocationInterval", 0);
            Log.d(TAG, "onStartCommand: LocationIntervalTime " + UPDATE_INTERVAL_IN_MILLISECONDS);
            buildGoogleApiClient(UPDATE_INTERVAL_IN_MILLISECONDS);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Connected to GoogleApiClient");
        initialiseGoogleClient(UPDATE_INTERVAL_IN_MILLISECONDS);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.v(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {

        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();

        gpsDate = new Date(location.getTime());

        if (lastLocation != null) {
            double elapsedTime = (location.getTime() - lastLocation.getTime()) / 1_000; // Convert milliseconds to seconds
            calculatedSpeed = lastLocation.distanceTo(location) / elapsedTime;
        }
        this.lastLocation = location;

        double speed = location.hasSpeed() ? location.getSpeed() : calculatedSpeed; // speed in metre/second


        if (sharedPreferences.getString("LocationUpdates", "").equals("false")) {
            travelledDistance = 0.0;
            previousLatitude = 0.0;
            previousLongitude = 0.0;
            return;
        } else if (sharedPreferences.getString("LocationUpdates", "").equals("true")) {
            if (previousLatitude == 0.0) {
                previousLatitude = currentLatitude;
                previousLongitude = currentLongitude;
            } else {
                travelledDistance += distance(previousLatitude, previousLongitude, currentLatitude, currentLongitude, "K");
                previousLatitude = currentLatitude;
                previousLongitude = currentLongitude;

                Log.d(TAG, "onLocationChanged: Speed " + speed);
                Log.d(TAG, "onLocationChanged: Distance " + String.valueOf(travelledDistance));
                if (!String.valueOf(speed).equals("NaN") && !String.valueOf(travelledDistance).equals("NaN")) {
                    Log.d(TAG, "onLocationChanged: Latitude " + location.getLatitude() + " Longitude " + location.getLongitude() + " speed " + roundDecimal(toKmPerHr(speed), 2) + " DateTime " + gpsDate + " distance " + roundDecimal(travelledDistance, 2));

                    BaseClass.getInstance().locationDataListener.onLocationUpdate(location.getLatitude(),location.getLongitude(),roundDecimal(toKmPerHr(speed), 2),roundDecimal(travelledDistance, 2),gpsDate);
                }
            }
        }

    }


    private double toKmPerHr(double speed) {
        return ((speed * 3600) / 1000);
    }

    private double roundDecimal(double value, final int decimalPlace) {
        BigDecimal bd = new BigDecimal(value);

        bd = bd.setScale(decimalPlace, RoundingMode.HALF_UP);
        value = bd.doubleValue();

        return value;
    }

    private static double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if (unit == "K") {
            dist = dist * 1.609344;
        } else if (unit == "N") {
            dist = dist * 0.8684;
        }
        return (dist);
    }

    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }


    //	This function converts radians to decimal degrees						

    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initialiseGoogleClient(long locationInterval) {
        Log.d(TAG, "initialiseGoogleClient: ");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission
                .ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
        } else {
            if (mGoogleApiClient.isConnected()) {
                LocationServices.FusedLocationApi.requestLocationUpdates(
                        mGoogleApiClient, mLocationRequest, this);
            } else {
                buildGoogleApiClient(locationInterval);
            }
        }
    }

    protected synchronized void buildGoogleApiClient(long locationInterval) {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest(locationInterval);
        mGoogleApiClient.connect();
    }

    protected void createLocationRequest(long locationInterval) {
        Log.d(TAG, "createLocationRequest: ");
        mLocationRequest = new LocationRequest();
        // requested if other applications are requesting location at a faster interval.

        mLocationRequest.setInterval(locationInterval);
        mLocationRequest.setFastestInterval(locationInterval);

        // application will never receive updates faster than this value.  FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }
}
