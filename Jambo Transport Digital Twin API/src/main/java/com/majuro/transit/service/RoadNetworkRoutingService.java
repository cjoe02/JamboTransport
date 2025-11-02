package com.majuro.transit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for generating realistic route paths that follow road networks using OSRM routing API
 */
@Service
@RequiredArgsConstructor
public class RoadNetworkRoutingService {

    private static final String OSRM_ROUTE_URL = "https://router.project-osrm.org/route/v1/driving/";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generates a route that follows the road network between consecutive waypoints
     * @param waypoints List of [latitude, longitude] pairs representing stops
     * @return List of coordinates that follow actual roads
     */
    public List<double[]> generateRoadPath(List<double[]> waypoints) {
        if (waypoints == null || waypoints.size() < 2) {
            return waypoints;
        }

        List<double[]> fullPath = new ArrayList<>();

        // Generate road-based routes between each consecutive pair of waypoints
        for (int i = 0; i < waypoints.size() - 1; i++) {
            double[] from = waypoints.get(i);
            double[] to = waypoints.get(i + 1);

            List<double[]> segment = routeBetweenPoints(from[0], from[1], to[0], to[1]);

            // Add segment to full path (skip first point if not the first segment to avoid duplicates)
            if (i == 0) {
                fullPath.addAll(segment);
            } else if (!segment.isEmpty()) {
                fullPath.addAll(segment.subList(1, segment.size()));
            }
        }

        return fullPath.isEmpty() ? waypoints : fullPath;
    }

    /**
     * Routes between two points using OSRM
     * @return List of coordinates following the road network
     */
    private List<double[]> routeBetweenPoints(double lat1, double lon1, double lat2, double lon2) {
        List<double[]> path = new ArrayList<>();

        try {
            // Build OSRM route API URL
            // Format: lon,lat;lon,lat
            String coordString = String.format("%f,%f;%f,%f", lon1, lat1, lon2, lat2);

            // OSRM route API parameters:
            // - geometries=geojson: return coordinates in GeoJSON format
            // - overview=full: return the full route geometry
            // - steps=false: we don't need turn-by-turn instructions
            String url = OSRM_ROUTE_URL + coordString +
                         "?geometries=geojson&overview=full&steps=false";

            // Call OSRM API
            String response = restTemplate.getForObject(url, String.class);

            // Parse response
            JsonNode root = objectMapper.readTree(response);

            if (root.has("routes") && root.get("routes").size() > 0) {
                JsonNode geometry = root.get("routes").get(0).get("geometry");
                JsonNode coordsNode = geometry.get("coordinates");

                for (JsonNode coord : coordsNode) {
                    double lon = coord.get(0).asDouble();
                    double lat = coord.get(1).asDouble();
                    path.add(new double[]{lat, lon});
                }
            }

            return path;

        } catch (Exception e) {
            System.err.println("Error routing between points: " + e.getMessage());
            // Fallback to straight line
            path.add(new double[]{lat1, lon1});
            path.add(new double[]{lat2, lon2});
            return path;
        }
    }

    /**
     * Generates a route using multiple waypoints (all at once)
     * This is more efficient than calling routeBetweenPoints multiple times
     * @param waypoints List of [latitude, longitude] pairs
     * @return List of coordinates following the road network
     */
    public List<double[]> generateRoadPathOptimized(List<double[]> waypoints) {
        if (waypoints == null || waypoints.size() < 2) {
            return waypoints;
        }

        // OSRM has a limit on the number of waypoints (typically around 100)
        // If we have more, we need to batch them
        if (waypoints.size() > 100) {
            return generateRoadPath(waypoints); // Fall back to pairwise routing
        }

        try {
            // Build coordinate string
            StringBuilder coordString = new StringBuilder();
            for (int i = 0; i < waypoints.size(); i++) {
                double[] coord = waypoints.get(i);
                coordString.append(coord[1]).append(",").append(coord[0]); // lon,lat
                if (i < waypoints.size() - 1) {
                    coordString.append(";");
                }
            }

            String url = OSRM_ROUTE_URL + coordString.toString() +
                         "?geometries=geojson&overview=full&steps=false";

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            if (root.has("routes") && root.get("routes").size() > 0) {
                JsonNode geometry = root.get("routes").get(0).get("geometry");
                JsonNode coordsNode = geometry.get("coordinates");

                List<double[]> path = new ArrayList<>();
                for (JsonNode coord : coordsNode) {
                    double lon = coord.get(0).asDouble();
                    double lat = coord.get(1).asDouble();
                    path.add(new double[]{lat, lon});
                }

                return path;
            }

        } catch (Exception e) {
            System.err.println("Error generating optimized road path: " + e.getMessage());
        }

        // Fallback
        return waypoints;
    }
}
