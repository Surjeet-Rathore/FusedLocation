package com.webaddict.locationlibrary.listeners;

import java.util.Date;

/**
 * Created by Lenovo on 5/12/2017.
 */

public interface LocationDataListener {
    void onLocationUpdate(double LocationLatitude,double LocationLongitude,double LocationSpeed,double LocationDistance,Date LocationDateTime);
}
