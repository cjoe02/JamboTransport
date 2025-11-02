package com.example.jambo.service;

import com.example.jambo.RouteShape;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.osmdroid.util.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service for snapping route waypoints to actual roads using OSRM (Open Source Routing Machine)
 */
public class OsrmService {
    // OSRM public API endpoint
    private static final String OSRM_BASE_URL = "https://router.project-osrm.org";

    private final OkHttpClient client;
    private final Gson gson;

    public OsrmService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    /**
     * Snaps a list of waypoints to actual roads using OSRM routing
     * @param waypoints List of waypoints to snap
     * @return List of RouteShape points snapped to roads
     */
    public List<RouteShape> snapRouteToRoads(List<GeoPoint> waypoints) {
        if (waypoints == null || waypoints.size() < 2) {
            return new ArrayList<>();
        }
        // If we have many waypoints already (dense route), add intermediate points for better control
        List<GeoPoint> denseWaypoints = addIntermediateWaypoints(waypoints);
        try {
            // Build OSRM route request URL with stricter parameters
            String url = buildOsrmUrl(denseWaypoints);
            // Make request
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                return new ArrayList<>();
            }
            String responseBody = response.body().string();
            // Parse response
            return parseOsrmResponse(responseBody);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Adds intermediate waypoints between existing waypoints to better control the route
     * This prevents OSRM from taking shortcuts through side roads
     */
    private List<GeoPoint> addIntermediateWaypoints(List<GeoPoint> waypoints) {
        List<GeoPoint> result = new ArrayList<>();

        for (int i = 0; i < waypoints.size() - 1; i++) {
            GeoPoint current = waypoints.get(i);
            GeoPoint next = waypoints.get(i + 1);

            result.add(current);

            // Calculate distance between points
            double distance = calculateDistance(current, next);

            // If points are far apart (>100m), add intermediate points
            if (distance > 0.001) { // roughly 100 meters
                int numIntermediate = (int) Math.ceil(distance / 0.0005); // Add point every ~50m
                numIntermediate = Math.min(numIntermediate, 5); // Max 5 intermediate points

                for (int j = 1; j <= numIntermediate; j++) {
                    double ratio = (double) j / (numIntermediate + 1);
                    double lat = current.getLatitude() + (next.getLatitude() - current.getLatitude()) * ratio;
                    double lon = current.getLongitude() + (next.getLongitude() - current.getLongitude()) * ratio;
                    result.add(new GeoPoint(lat, lon));
                }
            }
        }

        // Add the last point
        result.add(waypoints.get(waypoints.size() - 1));

        return result;
    }

    /**
     * Calculate simple distance between two points (not exact, but good enough for our purposes)
     */
    private double calculateDistance(GeoPoint p1, GeoPoint p2) {
        double latDiff = p2.getLatitude() - p1.getLatitude();
        double lonDiff = p2.getLongitude() - p1.getLongitude();
        return Math.sqrt(latDiff * latDiff + lonDiff * lonDiff);
    }

    /**
     * Builds OSRM route request URL from waypoints
     * Using 'match' service instead of 'route' to stick closer to provided waypoints
     */
    private String buildOsrmUrl(List<GeoPoint> waypoints) {
        StringBuilder coordinates = new StringBuilder();

        for (int i = 0; i < waypoints.size(); i++) {
            GeoPoint point = waypoints.get(i);
            // OSRM uses lon,lat format (opposite of lat,lon)
            coordinates.append(point.getLongitude())
                    .append(",")
                    .append(point.getLatitude());

            if (i < waypoints.size() - 1) {
                coordinates.append(";");
            }
        }

        // Use 'match' service which snaps GPS traces to roads more closely
        // This prevents detours through side roads
        return OSRM_BASE_URL + "/match/v1/driving/"
                + coordinates.toString()
                + "?overview=full&geometries=geojson&radiuses="
                + buildRadiusString(waypoints.size());
    }

    /**
     * Builds radius string for OSRM match service
     * Smaller radius = stricter matching to provided waypoints
     */
    private String buildRadiusString(int waypointCount) {
        StringBuilder radiuses = new StringBuilder();
        for (int i = 0; i < waypointCount; i++) {
            radiuses.append("50"); // 50 meter radius for matching
            if (i < waypointCount - 1) {
                radiuses.append(";");
            }
        }
        return radiuses.toString();
    }

    /**
     * Parses OSRM response to extract route geometry
     * Handles both 'match' and 'route' API responses
     */
    private List<RouteShape> parseOsrmResponse(String responseBody) {
        List<RouteShape> routePoints = new ArrayList<>();
        try {
            JsonObject response = gson.fromJson(responseBody, JsonObject.class);
            // Check for 'matchings' first (match API), fallback to 'routes' (route API)
            JsonArray items = null;
            if (response.has("matchings") && response.getAsJsonArray("matchings").size() > 0) {
                items = response.getAsJsonArray("matchings");
            } else if (response.has("routes") && response.getAsJsonArray("routes").size() > 0) {
                items = response.getAsJsonArray("routes");
            } else {
                return routePoints;
            }
            JsonObject item = items.get(0).getAsJsonObject();
            JsonObject geometry = item.getAsJsonObject("geometry");
            JsonArray coordinates = geometry.getAsJsonArray("coordinates");
            // Convert coordinates to RouteShape objects
            for (int i = 0; i < coordinates.size(); i++) {
                JsonArray coord = coordinates.get(i).getAsJsonArray();
                double lon = coord.get(0).getAsDouble();
                double lat = coord.get(1).getAsDouble();
                routePoints.add(createRouteShape(lat, lon, i));
            }
        } catch (Exception e) {
        }
        return routePoints;
    }

    /**
     * Helper method to create RouteShape with lat/lon/sequence
     * Note: This assumes RouteShape has a constructor or we modify it to be mutable
     */
    private RouteShape createRouteShape(double lat, double lon, int sequence) {
        // Since RouteShape doesn't have setters, we'll need to use a workaround
        // We'll create it via JSON deserialization
        String json = String.format(
            "{\"latitude\": %f, \"longitude\": %f, \"sequence\": %d}",
            lat, lon, sequence
        );
        return gson.fromJson(json, RouteShape.class);
    }
}
