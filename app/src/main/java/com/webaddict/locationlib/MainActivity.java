package com.webaddict.locationlib;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.webaddict.locationlibrary.base.BaseClass;
import com.webaddict.locationlibrary.listeners.LocationDataListener;

import java.util.Date;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, LocationDataListener {

    private Button startButton, stopButton;
    private TextView locationDataTextView;
    private GoogleApiClient.ConnectionCallbacks connectionCallbacks;
    private long locationInterval = 10000;
    private String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = (Button) findViewById(R.id.startButton);
        stopButton = (Button) findViewById(R.id.stopButton);
        locationDataTextView = (TextView) findViewById(R.id.locationData);

        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);


        connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {
            }

            @Override
            public void onConnectionSuspended(int i) {

            }
        };

        if (!BaseClass.getInstance().hasPermission)
        {
            BaseClass.getInstance().permissions(MainActivity.this);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.startButton:
                    BaseClass.getInstance().TurnOnGps(connectionCallbacks,MainActivity.this);
                    BaseClass.getInstance().startLocationService(MainActivity.this,this,locationInterval);
                break;
            case R.id.stopButton:
                BaseClass.getInstance().stopLocationService(MainActivity.this);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
    
    @Override
    public void onLocationUpdate(double LocationLatitude,double LocationLongitude,double LocationSpeed,double LocationDistance,Date LocationDateTime) {
        Log.d(TAG, "onLocationUpdate: Latitude"  + LocationLatitude + " Longitude " + LocationLongitude + " Speed " + LocationSpeed + " Distance " + LocationDistance + " DateTime " + LocationDateTime.toString());
        locationDataTextView.setText("Latitude"  + LocationLatitude + " Longitude " + LocationLongitude + " Speed " + LocationSpeed + " Distance " + LocationDistance + " DateTime " + LocationDateTime.toString());
    }
}
