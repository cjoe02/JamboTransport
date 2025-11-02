package com.example.jambo;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jambo.repository.LocationRepository;
import com.example.jambo.repository.RouteRepository;
import com.example.jambo.viewmodel.LocationViewModel;
import com.example.jambo.viewmodel.RouteViewModel;
import com.example.jambo.viewmodel.ViewModelFactory;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    // ViewModels
    private RouteViewModel routeViewModel;
    private LocationViewModel locationViewModel;

    // API Service
    private BusApiService apiService;

    // UI Components
    private MapView mapView;
    private TextView btnMap;
    private TextView btnRoutes;
    private LinearLayout headerLayout;
    private LinearLayout busTrackingContainer;
    private LinearLayout routeInfoContainer;
    private LinearLayout routesContainer;
    private LinearLayout routeAContainer;
    private LinearLayout routeBContainer;
    private LinearLayout selectedRouteContainer;
    private TextView selectedRouteBadge;
    private TextView selectedRouteName;
    private TextView selectedRouteStops;
    private TextView selectedRouteStatus;
    private TextView infoIcon;
    private TextView routeNameText;
    private TextView busStatusText;
    private TextView scheduledArrivalTimeText;
    private TextView estimatedArrivalTimeText;
    private TextView serviceStatusText;

    // Map markers and tracking
    private Marker currentOpenMarker;
    private Marker activeBusMarker;
    private Marker userLocationMarker;
    private Marker waveMarker;
    private GeoPoint currentUserLocation;
    private Arrival currentTrackedArrival;
    private Polyline routePath;
    private Polyline activeRoutePath;
    private Polyline routeAPath;
    private Polyline routeBPath;
    private List<RouteShape> routeAData;
    private List<RouteShape> routeBData;
    private boolean routeALoaded = false;
    private boolean routeBLoaded = false;
    private RouteStatus routeAStatusData;
    private RouteStatus routeBStatusData;
    private List<GeoPoint> fullRoutePoints;
    private BusStop currentlyTrackedBusStop;
    private String currentTrackedRoute;
    private String selectedRoute = null; // Track which route is selected (A, B, or null for both)
    private boolean isFirstBusUpdate;
    private boolean isInRoutesMode = false; // Track if we're in routes tab mode
    private List<Marker> busStopMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        setContentView(R.layout.activity_main);

        initializeViewModels();
        initializeViews();
        setupMap();
        setupBottomNavigation();
        observeViewModels();
    }

    private void initializeViewModels() {
        // Initialize API service
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(BusApiService.class);

        // Initialize repositories
        RouteRepository routeRepository = new RouteRepository(apiService);
        LocationRepository locationRepository = new LocationRepository();

        // Initialize ViewModels with factory
        ViewModelFactory factory = new ViewModelFactory(routeRepository, locationRepository);
        routeViewModel = new ViewModelProvider(this, factory).get(RouteViewModel.class);
        locationViewModel = new ViewModelProvider(this, factory).get(LocationViewModel.class);
    }

    private void initializeViews() {
        mapView = findViewById(R.id.map);
        btnMap = findViewById(R.id.btn_map);
        btnRoutes = findViewById(R.id.btn_routes);

        // Header and tracking UI
        headerLayout = findViewById(R.id.header_layout);

        // The included layout's root becomes the view with bus_tracking_info ID
        busTrackingContainer = findViewById(R.id.bus_tracking_info);
        routeNameText = findViewById(R.id.route_name_text);
        busStatusText = findViewById(R.id.bus_status_text);
        scheduledArrivalTimeText = findViewById(R.id.scheduled_arrival_time_text);
        estimatedArrivalTimeText = findViewById(R.id.estimated_arrival_time_text);
        serviceStatusText = findViewById(R.id.service_status_text);

        // Route info container
        routeInfoContainer = findViewById(R.id.route_info_container);
        routesContainer = findViewById(R.id.routes_container);
        routeAContainer = findViewById(R.id.route_a_container);
        routeBContainer = findViewById(R.id.route_b_container);
        selectedRouteContainer = findViewById(R.id.selected_route_container);
        selectedRouteBadge = findViewById(R.id.selected_route_badge);
        selectedRouteName = findViewById(R.id.selected_route_name);
        selectedRouteStops = findViewById(R.id.selected_route_stops);
        selectedRouteStatus = findViewById(R.id.selected_route_status);
        infoIcon = findViewById(R.id.info_icon);

        // Setup route selection click listeners
        routeAContainer.setOnClickListener(v -> onRouteSelected("A"));
        routeBContainer.setOnClickListener(v -> onRouteSelected("B"));
        selectedRouteContainer.setOnClickListener(v -> onRouteSelected(selectedRoute));

        // Setup info icon click listener
        infoIcon.setOnClickListener(v -> showTidalWaveInfoDialog());

        // Setup bus tracking container click listener to dismiss (acts like back button)
        busTrackingContainer.setOnClickListener(v -> stopBusTrackingFromUI());
    }

    private void observeViewModels() {
        // Observe bus stops - initial load
        routeViewModel.getBusStops().observe(this, busStops -> {
            if (busStops != null && !busStops.isEmpty()) {
                addBusStopMarkers(busStops);
            }
        });

        // Observe route-specific stops
        routeViewModel.getRouteStops().observe(this, routeStops -> {
            if (routeStops != null) {
                updateStopMarkersForRoute(routeStops);
            }
        });

        // Observe user location
        locationViewModel.getUserLocation().observe(this, location -> {
            if (location != null) {
                updateUserLocationMarker(location);
            }
        });

        // Observe active bus updates for tracking
        routeViewModel.getActiveBus().observe(this, activeBus -> {
            if (activeBus != null) {
                updateBusMarkerOnMap(activeBus);
                updateBusTrackingInfo(activeBus);
            }
        });

        // Observe route path from API
        routeViewModel.getRoutePath().observe(this, routeShapes -> {
            // Only process route path for bus tracking, not for routes tab
            if (!isInRoutesMode && routeShapes != null) {
                if (!routeShapes.isEmpty()) {
                    drawRoutePath(routeShapes);
                }
            }
        });
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        // Set more zoomed in default view
        IMapController mapController = mapView.getController();
        mapController.setZoom(19.0);
        GeoPoint startPoint = new GeoPoint(7.107604, 171.373001);
        mapController.setCenter(startPoint);

        // Restrict map scrolling to Majuro area
        // Majuro atoll boundaries (approximate)
        BoundingBox majuroBounds = new BoundingBox(
            7.150,  // North
            171.400, // East
            7.050,  // South
            171.300  // West
        );
        mapView.setScrollableAreaLimitDouble(majuroBounds);

        setupMapClickListener();
    }

    private void setupMapClickListener() {
        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (currentOpenMarker != null) {
                    currentOpenMarker.closeInfoWindow();
                    currentOpenMarker = null;
                }
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };

        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(mapEventsReceiver);
        mapView.getOverlays().add(0, mapEventsOverlay);
    }

    private void setupBottomNavigation() {
        btnMap.setOnClickListener(v -> selectMapButton());
        btnRoutes.setOnClickListener(v -> selectRoutesButton());
        selectMapButton();
    }

    private void selectMapButton() {
        btnMap.setBackgroundResource(R.drawable.button_selected);
        btnMap.setTextColor(ContextCompat.getColor(this, R.color.marshall_blue));
        btnMap.setSelected(true);

        btnRoutes.setBackgroundResource(R.drawable.button_unselected);
        btnRoutes.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        btnRoutes.setSelected(false);

        btnMap.refreshDrawableState();
        btnRoutes.refreshDrawableState();

        // Exit routes mode
        isInRoutesMode = false;

        // Hide route info container and show header
        routeInfoContainer.setVisibility(View.GONE);
        if (busTrackingContainer.getVisibility() != View.VISIBLE) {
            headerLayout.setVisibility(View.VISIBLE);
        }

        // Clear route overlays and reset selection
        clearRouteOverlays();
        selectedRoute = null;
        routeALoaded = false;
        routeBLoaded = false;

        // Remove wave marker
        if (waveMarker != null) {
            mapView.getOverlays().remove(waveMarker);
            waveMarker = null;
        }

        // Restore all bus stop icons to default
        restoreAllStopsToNormal();

        // Zoom back to user location with animation
        IMapController mapController = mapView.getController();
        if (currentUserLocation != null) {
            mapController.zoomTo(19.0, 800L);
            mapController.animateTo(currentUserLocation);
        } else {
            // Default to Majuro center if no user location
            mapController.zoomTo(19.0, 800L);
            GeoPoint defaultCenter = new GeoPoint(7.107604, 171.373001);
            mapController.animateTo(defaultCenter);
        }
    }

    private void selectRoutesButton() {
        btnMap.setBackgroundResource(R.drawable.button_unselected);
        btnMap.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        btnMap.setSelected(false);

        btnRoutes.setBackgroundResource(R.drawable.button_selected);
        btnRoutes.setTextColor(ContextCompat.getColor(this, R.color.marshall_blue));
        btnRoutes.setSelected(true);

        btnMap.refreshDrawableState();
        btnRoutes.refreshDrawableState();

        // Stop any active bus tracking
        if (currentTrackedArrival != null || activeBusMarker != null) {
            stopBusTracking();
        }

        // Enter routes mode
        isInRoutesMode = true;

        // Reset loading flags to allow fresh load
        routeALoaded = false;
        routeBLoaded = false;

        // Show route info container and hide header/tracking
        headerLayout.setVisibility(View.GONE);
        busTrackingContainer.setVisibility(View.GONE);
        routeInfoContainer.setVisibility(View.VISIBLE);

        // Reset route selection to show both
        selectedRoute = null;
        updateRouteSelectionUI();

        // Show all bus stops with circular icons immediately
        showAllStopsAsRouteStops();

        // Zoom out to show both routes
        zoomOutForRoutes();

        // Load and display both routes
        loadAndDisplayRoutes();
    }

    private void updateUserLocationMarker(GeoPoint userLocation) {
        // Store current user location
        currentUserLocation = userLocation;

        if (userLocationMarker != null) {
            mapView.getOverlays().remove(userLocationMarker);
        }

        userLocationMarker = new Marker(mapView);
        userLocationMarker.setPosition(userLocation);
        userLocationMarker.setTitle("📍 Your Location");
        userLocationMarker.setSnippet("You are here in Majuro");
        userLocationMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.pin_user_location));
        userLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        userLocationMarker.setOnMarkerClickListener((clickedMarker, mapView) -> {
            if (currentOpenMarker != null && currentOpenMarker != clickedMarker) {
                currentOpenMarker.closeInfoWindow();
            }
            currentOpenMarker = clickedMarker;

            IMapController mapController = mapView.getController();
            mapController.animateTo(clickedMarker.getPosition());

            clickedMarker.showInfoWindow();
            return true;
        });

        mapView.getOverlays().add(userLocationMarker);
        mapView.invalidate();
    }

    private void addBusStopMarkers(List<BusStop> busStops) {
        for (BusStop stop : busStops) {
            addBusStopMarker(stop);
        }
        mapView.invalidate();
    }

    private void addBusStopMarker(BusStop busStop) {
        GeoPoint location = new GeoPoint(busStop.getLatitude(), busStop.getLongitude());
        Marker marker = new Marker(mapView);
        marker.setPosition(location);
        marker.setTitle(busStop.getName());
        marker.setSnippet("Tap for routes");
        marker.setIcon(ContextCompat.getDrawable(this, R.drawable.pin_bus_stop));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setRelatedObject(busStop);

        CustomInfoWindow infoWindow = new CustomInfoWindow(R.layout.custom_info_window, mapView);
        infoWindow.setOnInfoWindowClickListener(() -> {
            marker.closeInfoWindow();
            showArrivalsBottomSheet(busStop);
        });
        marker.setInfoWindow(infoWindow);

        marker.setOnMarkerClickListener((clickedMarker, mapView) -> {
            // Don't allow clicking on stops when in routes mode
            if (isInRoutesMode) {
                return false;
            }

            if (currentOpenMarker != null && currentOpenMarker != clickedMarker) {
                currentOpenMarker.closeInfoWindow();
            }
            currentOpenMarker = clickedMarker;

            IMapController mapController = mapView.getController();
            mapController.animateTo(clickedMarker.getPosition());

            clickedMarker.showInfoWindow();
            return true;
        });

        mapView.getOverlays().add(marker);
        busStopMarkers.add(marker);
    }

    private void showArrivalsBottomSheet(BusStop busStop) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_arrivals, null);
        bottomSheet.setContentView(bottomSheetView);

        TextView stopName = bottomSheetView.findViewById(R.id.stop_name);
        LinearLayout arrivalsContainer = bottomSheetView.findViewById(R.id.arrivals_container);
        RecyclerView arrivalsList = bottomSheetView.findViewById(R.id.arrivals_list);
        ProgressBar loadingIndicator = bottomSheetView.findViewById(R.id.loading_indicator);
        TextView errorMessage = bottomSheetView.findViewById(R.id.error_message);
        TextView emptyMessage = bottomSheetView.findViewById(R.id.empty_message);

        stopName.setText(busStop.getName());

        ArrivalsAdapter adapter = new ArrivalsAdapter();
        arrivalsList.setLayoutManager(new LinearLayoutManager(this));
        arrivalsList.setAdapter(adapter);

        adapter.setOnArrivalClickListener(arrival -> {
            String routeName = arrival.getRouteName();
            // Extract route ID (A or B)
            String routeId = extractRouteId(routeName);
            bottomSheet.dismiss();
            startBusTracking(arrival, routeId, busStop);
        });

        arrivalsContainer.setVisibility(View.VISIBLE);
        loadingIndicator.setVisibility(View.VISIBLE);
        arrivalsList.setVisibility(View.GONE);
        errorMessage.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.GONE);

        bottomSheet.show();

        // Observe arrivals from ViewModel
        routeViewModel.getArrivals().observe(this, arrivals -> {
            if (arrivals != null) {
                if (arrivals.isEmpty()) {
                    emptyMessage.setVisibility(View.VISIBLE);
                    arrivalsList.setVisibility(View.GONE);
                } else {
                    adapter.setArrivals(arrivals);
                    arrivalsList.setVisibility(View.VISIBLE);
                    emptyMessage.setVisibility(View.GONE);
                }
            }
        });

        // Observe loading state
        routeViewModel.isLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        // Observe errors
        routeViewModel.getError().observe(this, error -> {
            if (error != null) {
                errorMessage.setText(error);
                errorMessage.setVisibility(View.VISIBLE);
                arrivalsList.setVisibility(View.GONE);
            } else {
                errorMessage.setVisibility(View.GONE);
            }
        });

        // Fetch arrivals via ViewModel
        routeViewModel.loadArrivals(busStop.getId(), 5);
    }

    private void startBusTracking(Arrival arrival, String routeId, BusStop busStop) {
        // Clear old bus marker if exists
        if (activeBusMarker != null) {
            mapView.getOverlays().remove(activeBusMarker);
            activeBusMarker = null;
        }
        // Clear old route path if exists
        if (routePath != null) {
            mapView.getOverlays().remove(routePath);
            routePath = null;
        }
        mapView.invalidate();
        currentlyTrackedBusStop = busStop;
        currentTrackedRoute = routeId;
        currentTrackedArrival = arrival;
        isFirstBusUpdate = true;
        // Load route path (which includes stop IDs)
        routeViewModel.loadRoutePath(routeId);
        // Show bus tracking info, hide search header
        showBusTrackingUI();
        // Display arrival information
        displayArrivalInfo();
        // Start tracking via ViewModel (updates every 3 seconds)
        routeViewModel.startBusTracking(arrival.getBusLabel());
    }

    private void stopBusTracking() {
        // Stop tracking via ViewModel
        routeViewModel.stopBusTracking();

        // Clear UI markers
        if (activeBusMarker != null) {
            mapView.getOverlays().remove(activeBusMarker);
            activeBusMarker = null;
        }

        // Clear route paths
        if (routePath != null) {
            mapView.getOverlays().remove(routePath);
            routePath = null;
        }

        if (activeRoutePath != null) {
            mapView.getOverlays().remove(activeRoutePath);
            activeRoutePath = null;
        }

        // Clear stored route points
        fullRoutePoints = null;

        // Clear route stops
        routeViewModel.clearRouteStops();

        // Restore all bus stop markers to normal
        restoreAllStopsToNormal();

        mapView.invalidate();

        currentlyTrackedBusStop = null;
        currentTrackedArrival = null;
    }

    private void stopBusTrackingFromUI() {
        stopBusTracking();
        hideBusTrackingUI();
    }

    private void showBusTrackingUI() {
        headerLayout.setVisibility(View.GONE);

        // Make visible and start from above the screen
        busTrackingContainer.setVisibility(View.VISIBLE);
        busTrackingContainer.setTranslationY(-busTrackingContainer.getHeight());

        // Animate sliding down from top
        busTrackingContainer.animate()
                .translationY(0)
                .setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    private void hideBusTrackingUI() {
        busTrackingContainer.setVisibility(View.GONE);
        headerLayout.setVisibility(View.VISIBLE);
    }

    private void displayArrivalInfo() {
        if (currentTrackedArrival == null) {
            return;
        }

        // Update route badge
        routeNameText.setText(currentTrackedArrival.getRouteName());

        // Update scheduled arrival time
        scheduledArrivalTimeText.setText(currentTrackedArrival.getScheduledArrivalTime() != null
                ? formatTimeAsAmPm(currentTrackedArrival.getScheduledArrivalTime()) : "--");

        // Update estimated arrival time
        estimatedArrivalTimeText.setText(currentTrackedArrival.getEstimatedArrivalTime() != null
                ? formatTimeAsAmPm(currentTrackedArrival.getEstimatedArrivalTime()) : "--");

        // Update service status
        serviceStatusText.setText(currentTrackedArrival.getServiceStatus() != null
                ? currentTrackedArrival.getServiceStatus() : "--");
    }

    private String formatTimeAsAmPm(String time24) {
        if (time24 == null || time24.isEmpty()) {
            return "--";
        }

        try {
            // Parse 24-hour time (e.g., "14:30" or "14:30:00")
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.US);
            if (time24.length() > 5) {
                // Has seconds
                inputFormat = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US);
            }
            java.util.Date date = inputFormat.parse(time24);

            // Format as 12-hour with AM/PM
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("h:mm a", java.util.Locale.US);
            return outputFormat.format(date);
        } catch (java.text.ParseException e) {
            android.util.Log.e("TimeFormat", "Failed to parse time: " + time24, e);
            return time24; // Return original if parsing fails
        }
    }

    private void updateBusTrackingInfo(ActiveBus bus) {
        // Update status from live bus data
        busStatusText.setText(bus.getStatus() != null ? bus.getStatus() : "--");
    }

    private void updateBusMarkerOnMap(ActiveBus bus) {
        // Create GeoPoint with correct lat/lon order
        GeoPoint busLocation = new GeoPoint(bus.getLatitude(), bus.getLongitude());
        if (activeBusMarker != null) {
            mapView.getOverlays().remove(activeBusMarker);
        }
        activeBusMarker = new Marker(mapView);
        activeBusMarker.setPosition(busLocation);
        activeBusMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.pin_bus_active));
        activeBusMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        mapView.getOverlays().add(activeBusMarker);
        // Draw blue line from bus to selected stop
        if (currentlyTrackedBusStop != null) {
            drawActiveRouteSection(busLocation, currentlyTrackedBusStop);
        }
        if (isFirstBusUpdate && currentlyTrackedBusStop != null) {
            zoomToShowBusAndStop(busLocation, currentlyTrackedBusStop);
            isFirstBusUpdate = false;
        }
        mapView.invalidate();
    }

    /**
     * Updates stop markers based on route stop IDs from route path
     * - Hides stops not on the route
     * - Shows route stops with circle icon (except selected stop)
     * - Keeps selected stop with original icon
     */
    private void updateStopMarkersForRouteStopIds(List<String> routeStopIds) {
        // Create set of route stop IDs for quick lookup
        java.util.Set<String> routeStopIdSet = new java.util.HashSet<>(routeStopIds);
        // Update each marker
        for (Marker marker : busStopMarkers) {
            BusStop markerStop = (BusStop) marker.getRelatedObject();
            if (markerStop == null) continue;
            boolean isOnRoute = routeStopIdSet.contains(markerStop.getId());
            boolean isSelected = currentlyTrackedBusStop != null &&
                                markerStop.getId().equals(currentlyTrackedBusStop.getId());
            if (!isOnRoute) {
                // Hide stops not on this route
                marker.setVisible(false);
            } else if (isSelected) {
                // Keep selected stop with original icon
                marker.setVisible(true);
                marker.setIcon(ContextCompat.getDrawable(this, R.drawable.pin_bus_stop));
            } else {
                // Show route stops with circle icon
                marker.setVisible(true);
                marker.setIcon(ContextCompat.getDrawable(this, R.drawable.pin_route_stop));
            }
        }
        mapView.invalidate();
    }

    /**
     * Legacy method - kept for compatibility with observer
     */
    private void updateStopMarkersForRoute(List<BusStop> routeStops) {
        if (routeStops == null || routeStops.isEmpty()) return;

        List<String> stopIds = new ArrayList<>();
        for (BusStop stop : routeStops) {
            stopIds.add(stop.getId());
        }
        updateStopMarkersForRouteStopIds(stopIds);
    }

    /**
     * Restores all stop markers to their original state
     */
    private void restoreAllStopsToNormal() {
        for (Marker marker : busStopMarkers) {
            marker.setVisible(true);
            marker.setIcon(ContextCompat.getDrawable(this, R.drawable.pin_bus_stop));
        }
        mapView.invalidate();
    }

    /**
     * Changes all stop markers to circular route icons
     */
    private void showAllStopsAsRouteStops() {
        for (Marker marker : busStopMarkers) {
            marker.setVisible(true);
            marker.setIcon(ContextCompat.getDrawable(this, R.drawable.pin_route_stop));
        }
        mapView.invalidate();
    }

    private String extractRouteId(String routeName) {
        // If already "A" or "B", return as is
        if ("A".equals(routeName) || "B".equals(routeName)) {
            return routeName;
        }

        // Extract A or B from route name
        if (routeName != null) {
            if (routeName.contains("A") || routeName.contains("a")) {
                return "A";
            } else if (routeName.contains("B") || routeName.contains("b")) {
                return "B";
            }
        }

        // Default to A if can't determine
        return "A";
    }

    private void drawRoutePath(List<RouteShape> routeShapes) {
        // Remove old paths if they exist
        if (routePath != null) {
            mapView.getOverlays().remove(routePath);
        }
        if (activeRoutePath != null) {
            mapView.getOverlays().remove(activeRoutePath);
        }
        // Extract stop IDs from route path (only where isStop is true or stopId is not null)
        List<String> routeStopIds = new ArrayList<>();
        for (RouteShape shape : routeShapes) {
            // Check if this point has a stop ID
            if (shape.getStopId() != null) {
                if (!routeStopIds.contains(shape.getStopId())) {
                    routeStopIds.add(shape.getStopId());
                }
            }
        }
        // Update stop markers based on route
        updateStopMarkersForRouteStopIds(routeStopIds);
        // Convert RouteShape to GeoPoint list and log all points
        List<GeoPoint> points = new ArrayList<>();
        for (int i = 0; i < routeShapes.size(); i++) {
            RouteShape shape = routeShapes.get(i);
            GeoPoint point = new GeoPoint(shape.getLatitude(), shape.getLongitude());
            points.add(point);
        }
        // Store full route points for later use
        fullRoutePoints = new ArrayList<>(points);
        android.util.Log.d("RoutePath", "Route path loaded with " + fullRoutePoints.size() + " points");

        // Create and style the polyline - thicker to match road width
        routePath = new Polyline();
        routePath.setPoints(points);
        routePath.setColor(ContextCompat.getColor(this, R.color.gray_medium));
        routePath.setWidth(15f); // Increased from 8f to match road thickness
        // Add path to map at the bottom (below markers)
        mapView.getOverlays().add(0, routePath);
        mapView.invalidate();

        // If we're already tracking a bus, redraw the blue path now that we have route data
        if (activeBusMarker != null && currentlyTrackedBusStop != null) {
            android.util.Log.d("RoutePath", "Redrawing blue path with route data");
            drawActiveRouteSection(activeBusMarker.getPosition(), currentlyTrackedBusStop);
        }
    }


    private void drawActiveRouteSection(GeoPoint busLocation, BusStop targetStop) {
        // Remove old active path if exists
        if (activeRoutePath != null) {
            mapView.getOverlays().remove(activeRoutePath);
            activeRoutePath = null;
        }

        GeoPoint stopLocation = new GeoPoint(targetStop.getLatitude(), targetStop.getLongitude());
        List<GeoPoint> activeSection = new ArrayList<>();

        // If we don't have the full route yet, draw a straight line
        if (fullRoutePoints == null || fullRoutePoints.isEmpty()) {
            android.util.Log.d("ActiveRoute", "Drawing straight line - route not loaded yet");
            activeSection.add(busLocation);
            activeSection.add(stopLocation);
        } else {
            // Find closest points on route to bus and stop
            int busIndex = findClosestPointIndex(fullRoutePoints, busLocation);
            int stopIndex = findClosestPointIndex(fullRoutePoints, stopLocation);

            android.util.Log.d("ActiveRoute", "Bus at " + busLocation.getLatitude() + "," + busLocation.getLongitude() +
                               " -> index " + busIndex);
            android.util.Log.d("ActiveRoute", "Stop at " + stopLocation.getLatitude() + "," + stopLocation.getLongitude() +
                               " -> index " + stopIndex);
            android.util.Log.d("ActiveRoute", "Total route points: " + fullRoutePoints.size());

            // Ensure busIndex is before stopIndex (bus hasn't passed the stop yet)
            if (busIndex < stopIndex) {
                // Add bus location as start point
                activeSection.add(busLocation);
                // Add route points between bus and stop
                for (int i = busIndex; i <= stopIndex; i++) {
                    activeSection.add(fullRoutePoints.get(i));
                }
                // Add stop location as end point
                activeSection.add(stopLocation);
                android.util.Log.d("ActiveRoute", "Drew snapped path with " + activeSection.size() + " points");
            } else {
                // Bus has passed the stop or indices are equal - draw straight line
                android.util.Log.d("ActiveRoute", "Bus passed stop or same index - drawing straight line");
                activeSection.add(busLocation);
                activeSection.add(stopLocation);
            }
        }

        // Create blue polyline for active section
        activeRoutePath = new Polyline();
        activeRoutePath.setPoints(activeSection);
        activeRoutePath.setColor(ContextCompat.getColor(this, R.color.marshall_blue));
        activeRoutePath.setWidth(15f); // Same thickness as main route

        // Add active path above the gray route but below markers
        // Find the index of the gray route and add just after it
        int routeIndex = mapView.getOverlays().indexOf(routePath);
        if (routeIndex >= 0) {
            mapView.getOverlays().add(routeIndex + 1, activeRoutePath);
        } else {
            mapView.getOverlays().add(activeRoutePath);
        }

        android.util.Log.d("ActiveRoute", "Blue path drawn with " + activeSection.size() + " points");
    }

    /**
     * Finds the index of the closest point in the route to a given location
     */
    private int findClosestPointIndex(List<GeoPoint> points, GeoPoint target) {
        int closestIndex = 0;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < points.size(); i++) {
            GeoPoint point = points.get(i);
            double distance = calculateDistance(point, target);

            if (distance < minDistance) {
                minDistance = distance;
                closestIndex = i;
            }
        }

        return closestIndex;
    }

    /**
     * Calculate distance between two GeoPoints (simple Euclidean)
     */
    private double calculateDistance(GeoPoint p1, GeoPoint p2) {
        double latDiff = p2.getLatitude() - p1.getLatitude();
        double lonDiff = p2.getLongitude() - p1.getLongitude();
        return Math.sqrt(latDiff * latDiff + lonDiff * lonDiff);
    }

    private void zoomToShowBusAndStop(GeoPoint busLocation, BusStop busStop) {
        GeoPoint stopLocation = new GeoPoint(busStop.getLatitude(), busStop.getLongitude());

        double minLat = Math.min(busLocation.getLatitude(), stopLocation.getLatitude());
        double maxLat = Math.max(busLocation.getLatitude(), stopLocation.getLatitude());
        double minLon = Math.min(busLocation.getLongitude(), stopLocation.getLongitude());
        double maxLon = Math.max(busLocation.getLongitude(), stopLocation.getLongitude());

        double latPadding = (maxLat - minLat) * 0.1;
        double lonPadding = (maxLon - minLon) * 0.1;

        latPadding = Math.max(latPadding, 0.002);
        lonPadding = Math.max(lonPadding, 0.002);

        BoundingBox boundingBox = new BoundingBox(
                maxLat + latPadding,
                maxLon + lonPadding,
                minLat - latPadding,
                minLon - lonPadding
        );

        mapView.zoomToBoundingBox(boundingBox, true);
    }

    private void loadAndDisplayRoutes() {
        // Clear any existing route overlays first
        clearRouteOverlays();

        // If we already have cached data, use it immediately
        if (routeAData != null && !routeAData.isEmpty() && routeBData != null && !routeBData.isEmpty()) {
            drawRouteOverlay(routeAData, true);
            drawRouteOverlay(routeBData, false);
            showAllStopsAsRouteStops();
            return;
        }

        // Use direct API calls for both routes in parallel
        final boolean[] routeAComplete = {false};
        final boolean[] routeBComplete = {false};

        // Fetch Route A
        apiService.getRoutePath("A").enqueue(new retrofit2.Callback<RoutePathResponse>() {
            @Override
            public void onResponse(retrofit2.Call<RoutePathResponse> call, retrofit2.Response<RoutePathResponse> response) {
                if (response.isSuccessful() && response.body() != null && isInRoutesMode) {
                    RoutePathResponse pathResponse = response.body();
                    List<RouteShape> path = pathResponse.getPath();
                    if (path != null && !path.isEmpty()) {
                        routeAData = path;
                        drawRouteOverlay(path, true);
                        routeAComplete[0] = true;

                        // Show stops if both routes are loaded
                        if (routeAComplete[0] && routeBComplete[0]) {
                            showAllStopsAsRouteStops();
                        }
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<RoutePathResponse> call, Throwable t) {
                // Handle failure silently
            }
        });

        // Fetch Route B
        apiService.getRoutePath("B").enqueue(new retrofit2.Callback<RoutePathResponse>() {
            @Override
            public void onResponse(retrofit2.Call<RoutePathResponse> call, retrofit2.Response<RoutePathResponse> response) {
                if (response.isSuccessful() && response.body() != null && isInRoutesMode) {
                    RoutePathResponse pathResponse = response.body();
                    List<RouteShape> path = pathResponse.getPath();
                    if (path != null && !path.isEmpty()) {
                        routeBData = path;
                        drawRouteOverlay(path, false);
                        routeBComplete[0] = true;

                        // Show stops if both routes are loaded
                        if (routeAComplete[0] && routeBComplete[0]) {
                            showAllStopsAsRouteStops();
                        }
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<RoutePathResponse> call, Throwable t) {
                // Handle failure silently
            }
        });
    }

    private void drawRouteOverlay(List<RouteShape> routeShapes, boolean isRouteA) {
        // Convert RouteShape to GeoPoint list
        List<GeoPoint> points = new ArrayList<>();
        for (RouteShape shape : routeShapes) {
            GeoPoint point = new GeoPoint(shape.getLatitude(), shape.getLongitude());
            points.add(point);
        }

        // Create and style the polyline
        Polyline routePolyline = new Polyline();
        routePolyline.setPoints(points);
        // Default to gray, will be updated by updateRouteColors() if selected
        routePolyline.setColor(ContextCompat.getColor(this, R.color.gray_medium));
        routePolyline.setWidth(12f);
        // Make routes semi-transparent so both colors are visible when overlapping
        routePolyline.getPaint().setAlpha(180); // 70% opacity

        // Store reference and add to map
        if (isRouteA) {
            routeAPath = routePolyline;
            // Add Route A at index 1 (on top of Route B)
            mapView.getOverlays().add(1, routePolyline);
        } else {
            routeBPath = routePolyline;
            // Add Route B at index 0 (at the bottom)
            mapView.getOverlays().add(0, routePolyline);
        }

        mapView.invalidate();
    }

    private void clearRouteOverlays() {
        if (routeAPath != null) {
            mapView.getOverlays().remove(routeAPath);
            routeAPath = null;
        }
        if (routeBPath != null) {
            mapView.getOverlays().remove(routeBPath);
            routeBPath = null;
        }
        mapView.invalidate();
    }

    private void onRouteSelected(String route) {
        // Toggle selection - if clicking the same route, deselect it
        if (route.equals(selectedRoute)) {
            selectedRoute = null;
            // Remove wave marker when deselecting
            if (waveMarker != null) {
                mapView.getOverlays().remove(waveMarker);
                waveMarker = null;
                mapView.invalidate();
            }
        } else {
            selectedRoute = route;
            // Fetch route status when selecting
            fetchRouteStatus(route);
        }

        // Update UI to show selection
        updateRouteSelectionUI();

        // Update route colors on map
        updateRouteColors();
    }

    private void fetchRouteStatus(String route) {
        // Construct the route name for API (ROUTE_A or ROUTE_B)
        String routeName = "ROUTE_" + route;

        android.util.Log.d("RouteStatus", "Fetching status for: " + routeName);

        apiService.getRouteStatus(routeName).enqueue(new retrofit2.Callback<RouteStatus>() {
            @Override
            public void onResponse(retrofit2.Call<RouteStatus> call, retrofit2.Response<RouteStatus> response) {
                android.util.Log.d("RouteStatus", "Response received - Success: " + response.isSuccessful() + ", Code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    RouteStatus status = response.body();
                    // Store status data
                    if ("A".equals(route)) {
                        routeAStatusData = status;
                    } else {
                        routeBStatusData = status;
                    }
                    displayRouteStatus(route, status);
                    // Update route colors (orange if delayed, green otherwise)
                    updateRouteColors();
                } else {
                    android.util.Log.e("RouteStatus", "Response failed or body is null");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<RouteStatus> call, Throwable t) {
                android.util.Log.e("RouteStatus", "API call failed", t);
            }
        });
    }

    private void displayRouteStatus(String route, RouteStatus status) {
        if (status == null) {
            return;
        }

        // Log the status data for debugging
        android.util.Log.d("RouteStatus", "Route: " + route +
                           ", ImpactLevel: " + status.getImpactLevel() +
                           ", Delay: " + status.getEstimatedDelayMinutes() +
                           ", Reason: " + status.getReason() +
                           ", TideDirection: " + status.getTideDirection());

        // Build status text
        StringBuilder statusText = new StringBuilder();

        // Add delay information if present
        if (status.getEstimatedDelayMinutes() != null && status.getEstimatedDelayMinutes() > 0) {
            statusText.append("⏱ ").append(status.getEstimatedDelayMinutes()).append(" min delay");
        }

        // Add reason if present
        if (status.getReason() != null && !status.getReason().isEmpty()) {
            if (statusText.length() > 0) {
                statusText.append(" • ");
            }
            statusText.append(status.getReason());
        }

        // Set background color based on impact level
        int statusBgColor;
        String impactLevel = status.getImpactLevel();

        if ("HIGH".equalsIgnoreCase(impactLevel)) {
            statusBgColor = ContextCompat.getColor(this, R.color.flood_warning); // Red
        } else if ("MEDIUM".equalsIgnoreCase(impactLevel)) {
            statusBgColor = ContextCompat.getColor(this, R.color.flood_minor); // Orange
        } else if ("LOW".equalsIgnoreCase(impactLevel)) {
            statusBgColor = ContextCompat.getColor(this, R.color.flood_safe); // Green
        } else {
            // Default to orange for unknown/unstable conditions
            statusBgColor = ContextCompat.getColor(this, R.color.flood_minor); // Orange
        }

        // Update the selected route detail status if this route is currently selected
        if (route.equals(selectedRoute)) {
            // Create rounded background with dynamic color
            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            drawable.setCornerRadius(24f); // 8dp in pixels (approximately)
            drawable.setColor(statusBgColor);

            selectedRouteStatus.setBackground(drawable);
            selectedRouteStatus.setTextColor(ContextCompat.getColor(this, R.color.black));
            selectedRouteStatus.setText(statusText.toString());
            selectedRouteStatus.setVisibility(statusText.length() > 0 ? View.VISIBLE : View.GONE);
        }

        // Update wave marker on map if in routes mode
        if (isInRoutesMode) {
            android.util.Log.d("WaveMarker", "Updating wave marker for route: " + route + ", selectedRoute: " + selectedRoute + ", isInRoutesMode: " + isInRoutesMode);
            updateWaveMarker(status, statusBgColor);
        }
    }


    private void showTidalWaveInfoDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Tidal Wave Information")
                .setMessage("The tidal wave reading features are extracted from services provided by:\n\nhttps://www.pacioos.hawaii.edu/waves/buoy-majuro/\n\nThis data helps us provide accurate delay estimates for bus routes affected by tidal conditions.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void updateWaveMarker(RouteStatus status, int bgColor) {
        String tideDirection = status.getTideDirection();
        android.util.Log.d("WaveMarker", "Tide direction: " + tideDirection + ", bgColor: " + bgColor);

        // Default to SOUTHERLY for testing if no tide direction provided
        if (tideDirection == null || tideDirection.isEmpty()) {
            android.util.Log.d("WaveMarker", "No tide direction from API, using SOUTHERLY as default for testing");
            tideDirection = "SOUTHERLY";
        }

        // Determine position based on tide direction
        GeoPoint wavePosition;
        if ("SOUTHERLY".equalsIgnoreCase(tideDirection) || "SOUTH".equalsIgnoreCase(tideDirection)) {
            // Southerly: 7.123702, 171.366593
            wavePosition = new GeoPoint(7.123702, 171.366593);
            android.util.Log.d("WaveMarker", "Southerly tide - position: " + wavePosition);
        } else if ("NORTHERLY".equalsIgnoreCase(tideDirection) || "NORTH".equalsIgnoreCase(tideDirection)) {
            // Northerly: 7.117144, 171.362452
            wavePosition = new GeoPoint(7.117144, 171.362452);
            android.util.Log.d("WaveMarker", "Northerly tide - position: " + wavePosition);
        } else {
            android.util.Log.d("WaveMarker", "Unknown tide direction: " + tideDirection);
            // Unknown direction, don't show marker
            if (waveMarker != null) {
                mapView.getOverlays().remove(waveMarker);
                waveMarker = null;
                mapView.invalidate();
            }
            return;
        }

        // Create larger colored circle bitmap with wave icon overlay
        int circleSize = 140; // Size in pixels (increased from 100)
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(circleSize, circleSize, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);

        // Draw filled circle background
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setAntiAlias(true);
        paint.setColor(bgColor);
        paint.setStyle(android.graphics.Paint.Style.FILL);
        canvas.drawCircle(circleSize / 2, circleSize / 2, circleSize / 2, paint);

        // Draw wave icon on top
        try {
            android.graphics.drawable.Drawable waveDrawable = ContextCompat.getDrawable(this, R.drawable.wave_icon);
            if (waveDrawable != null) {
                // Center the wave icon with some padding
                int wavePadding = circleSize / 5; // 20% padding
                waveDrawable.setBounds(wavePadding, wavePadding, circleSize - wavePadding, circleSize - wavePadding);
                waveDrawable.draw(canvas);
            }
        } catch (Exception e) {
            android.util.Log.e("WaveMarker", "Failed to load wave icon", e);
        }

        // Remove old marker if exists
        if (waveMarker != null) {
            mapView.getOverlays().remove(waveMarker);
        }

        // Create new marker
        waveMarker = new Marker(mapView);
        waveMarker.setPosition(wavePosition);
        waveMarker.setIcon(new android.graphics.drawable.BitmapDrawable(getResources(), bitmap));
        waveMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        waveMarker.setTitle("Tidal Impact");

        mapView.getOverlays().add(waveMarker);
        mapView.invalidate();

        android.util.Log.d("WaveMarker", "Wave marker added at: " + wavePosition + ", overlays count: " + mapView.getOverlays().size());
    }

    private void updateRouteSelectionUI() {
        if (selectedRoute != null) {
            // A route is selected - fade out both small containers, fade in the detail view
            routesContainer.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        routesContainer.setVisibility(View.GONE);

                        // Populate the selected route detail container
                        if ("A".equals(selectedRoute)) {
                            selectedRouteBadge.setText("A");
                            selectedRouteName.setText("Rita Roundabout");
                            selectedRouteStops.setText("17 stops");
                        } else if ("B".equals(selectedRoute)) {
                            selectedRouteBadge.setText("B");
                            selectedRouteName.setText("Downtown → Rita");
                            selectedRouteStops.setText("17 stops");
                        }

                        // Show the selected route container with fade in
                        selectedRouteContainer.setVisibility(View.VISIBLE);
                        selectedRouteContainer.setAlpha(0f);
                        selectedRouteContainer.animate()
                                .alpha(1f)
                                .setDuration(250)
                                .start();
                    })
                    .start();
        } else {
            // No route selected - fade out detail view, fade in both small containers
            selectedRouteContainer.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        selectedRouteContainer.setVisibility(View.GONE);

                        // Reset backgrounds for both route containers
                        routeAContainer.setBackgroundResource(R.drawable.route_card_background);
                        routeBContainer.setBackgroundResource(R.drawable.route_card_background);

                        // Show the routes container with fade in
                        routesContainer.setVisibility(View.VISIBLE);
                        routesContainer.setAlpha(0f);
                        routesContainer.animate()
                                .alpha(1f)
                                .setDuration(250)
                                .start();
                    })
                    .start();
        }
    }

    private void updateRouteColors() {
        // Update Route A color
        if (routeAPath != null) {
            int colorA;
            if ("A".equals(selectedRoute)) {
                // Check if route has delay - use orange if delayed, green otherwise
                boolean hasDelay = routeAStatusData != null &&
                                   routeAStatusData.getEstimatedDelayMinutes() != null &&
                                   routeAStatusData.getEstimatedDelayMinutes() > 0;
                colorA = hasDelay ? R.color.flood_minor : R.color.route_green;
            } else {
                colorA = R.color.gray_medium; // Not selected, use gray
            }
            routeAPath.setColor(ContextCompat.getColor(this, colorA));
        }

        // Update Route B color
        if (routeBPath != null) {
            int colorB;
            if ("B".equals(selectedRoute)) {
                // Check if route has delay - use orange if delayed, green otherwise
                boolean hasDelay = routeBStatusData != null &&
                                   routeBStatusData.getEstimatedDelayMinutes() != null &&
                                   routeBStatusData.getEstimatedDelayMinutes() > 0;
                colorB = hasDelay ? R.color.flood_minor : R.color.route_green;
            } else {
                colorB = R.color.gray_medium; // Not selected, use gray
            }
            routeBPath.setColor(ContextCompat.getColor(this, colorB));
        }

        // Adjust z-order: bring selected route to front
        if ("A".equals(selectedRoute)) {
            // Move Route A to top
            if (routeAPath != null) {
                mapView.getOverlays().remove(routeAPath);
                mapView.getOverlays().add(routeAPath);
            }
        } else if ("B".equals(selectedRoute)) {
            // Move Route B to top
            if (routeBPath != null) {
                mapView.getOverlays().remove(routeBPath);
                mapView.getOverlays().add(routeBPath);
            }
        }

        mapView.invalidate();
    }

    private void zoomOutForRoutes() {
        // Create bounding box between Alwal and Downtown Uliga
        // Alwal: 7.124234, 171.356106
        // Downtown Uliga: 7.109348, 171.371379
        double minLat = 7.109348;
        double maxLat = 7.124234;
        double minLon = 171.356106;
        double maxLon = 171.371379;

        // Add padding (10% on each side)
        double latPadding = (maxLat - minLat) * 0.1;
        double lonPadding = (maxLon - minLon) * 0.1;

        BoundingBox routeBounds = new BoundingBox(
            maxLat + latPadding,  // North
            maxLon + lonPadding,  // East
            minLat - latPadding,  // South
            minLon - lonPadding   // West
        );

        mapView.zoomToBoundingBox(routeBounds, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        stopBusTracking();
    }

    @Override
    public void onBackPressed() {
        // If a route is selected in routes mode, deselect it first
        if (isInRoutesMode && selectedRoute != null) {
            onRouteSelected(selectedRoute); // Toggle to deselect
        }
        // If tracking a bus on map tab, stop tracking instead of exiting
        else if (!isInRoutesMode && currentTrackedArrival != null) {
            stopBusTrackingFromUI();
        }
        else {
            super.onBackPressed();
        }
    }
}
