package com.example.jambo.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jambo.ActiveBus;
import com.example.jambo.Arrival;
import com.example.jambo.BusStop;
import com.example.jambo.RouteShape;
import com.example.jambo.repository.RouteRepository;

import java.util.List;

public class RouteViewModel extends ViewModel {

    private final RouteRepository repository;
    private Handler trackingHandler;
    private Runnable trackingRunnable;
    private String currentTrackedTripId;
    private final MutableLiveData<Boolean> isBusTracking = new MutableLiveData<>(false);

    public RouteViewModel(RouteRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<BusStop>> getBusStops() {
        return repository.getBusStops();
    }

    public LiveData<List<BusStop>> getRouteStops() {
        return repository.getRouteStops();
    }

    public LiveData<List<Arrival>> getArrivals() {
        return repository.getArrivals();
    }

    public LiveData<ActiveBus> getActiveBus() {
        return repository.getActiveBus();
    }

    public LiveData<List<RouteShape>> getRoutePath() {
        return repository.getRoutePath();
    }

    public LiveData<Boolean> isLoading() {
        return repository.isLoading();
    }

    public LiveData<String> getError() {
        return repository.getError();
    }

    public LiveData<Boolean> isBusTracking() {
        return isBusTracking;
    }

    public void loadArrivals(String stopId, int limit) {
        repository.fetchArrivals(stopId, limit);
    }

    public void loadActiveBus(String tripId) {
        repository.fetchActiveBus(tripId);
    }

    public void loadRoutePath(String route) {
        repository.fetchRoutePath(route);
    }

    public void loadRouteStops(String route) {
        repository.fetchRouteStops(route);
    }

    public void startBusTracking(String tripId) {
        // Stop any existing tracking
        stopBusTracking();
        currentTrackedTripId = tripId;
        isBusTracking.setValue(true);
        // Initialize handler
        trackingHandler = new Handler(Looper.getMainLooper());
        // Create tracking runnable
        trackingRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentTrackedTripId != null) {
                    repository.fetchActiveBus(currentTrackedTripId);
                    // Schedule next update in 3 seconds
                    trackingHandler.postDelayed(this, 3000);
                }
            }
        };
        // Start tracking immediately
        trackingRunnable.run();
    }

    public void stopBusTracking() {
        if (trackingHandler != null && trackingRunnable != null) {
            trackingHandler.removeCallbacks(trackingRunnable);
        }

        currentTrackedTripId = null;
        isBusTracking.setValue(false);
        repository.clearActiveBus();
    }

    public void clearArrivals() {
        repository.clearArrivals();
    }

    public void clearActiveBus() {
        repository.clearActiveBus();
    }

    public void clearRouteStops() {
        repository.clearRouteStops();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopBusTracking();
    }
}
