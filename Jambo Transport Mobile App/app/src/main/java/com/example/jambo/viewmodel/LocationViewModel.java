package com.example.jambo.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.jambo.repository.LocationRepository;

import org.osmdroid.util.GeoPoint;

public class LocationViewModel extends ViewModel {

    private final LocationRepository repository;

    public LocationViewModel(LocationRepository repository) {
        this.repository = repository;
    }

    public LiveData<GeoPoint> getUserLocation() {
        return repository.getUserLocation();
    }

    public void updateUserLocation(double latitude, double longitude) {
        repository.updateUserLocation(latitude, longitude);
    }

    public void updateUserLocation(GeoPoint location) {
        repository.updateUserLocation(location);
    }
}
