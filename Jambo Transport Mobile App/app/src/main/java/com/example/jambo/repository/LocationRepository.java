package com.example.jambo.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.osmdroid.util.GeoPoint;

public class LocationRepository {

    private final MutableLiveData<GeoPoint> userLocationLiveData = new MutableLiveData<>();

    public LocationRepository() {
        // Initialize with default Majuro location
        // In the future, this can be replaced with actual GPS location
        initializeDefaultLocation();
    }

    private void initializeDefaultLocation() {
        GeoPoint defaultLocation = new GeoPoint(7.107604, 171.373001);
        userLocationLiveData.setValue(defaultLocation);
    }

    public LiveData<GeoPoint> getUserLocation() {
        return userLocationLiveData;
    }

    public void updateUserLocation(double latitude, double longitude) {
        GeoPoint location = new GeoPoint(latitude, longitude);
        userLocationLiveData.setValue(location);
    }

    public void updateUserLocation(GeoPoint location) {
        userLocationLiveData.setValue(location);
    }
}
