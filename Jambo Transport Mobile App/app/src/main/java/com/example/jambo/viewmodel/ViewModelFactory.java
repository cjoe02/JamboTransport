package com.example.jambo.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.jambo.repository.LocationRepository;
import com.example.jambo.repository.RouteRepository;

public class ViewModelFactory implements ViewModelProvider.Factory {

    private final RouteRepository routeRepository;
    private final LocationRepository locationRepository;

    public ViewModelFactory(RouteRepository routeRepository, LocationRepository locationRepository) {
        this.routeRepository = routeRepository;
        this.locationRepository = locationRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(RouteViewModel.class)) {
            return (T) new RouteViewModel(routeRepository);
        } else if (modelClass.isAssignableFrom(LocationViewModel.class)) {
            return (T) new LocationViewModel(locationRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
