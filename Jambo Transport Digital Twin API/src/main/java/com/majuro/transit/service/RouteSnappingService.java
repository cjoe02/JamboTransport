package com.majuro.transit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for snapping route coordinates to OpenStreetMap roads using OSRM map-matching API
 */
@Service
@RequiredArgsConstructor
public class RouteSnappingService {

    private static final String OSRM_MATCH_URL = "https://router.project-osrm.org/match/v1/driving/";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Snaps a list of coordinates to the nearest roads in OpenStreetMap
     * @param coordinates List of [longitude, latitude] pairs
     * @return List of snapped coordinates that follow OSM roads
     */
    public List<double[]> snapToRoads(List<double[]> coordinates) {
        if (coordinates == null || coordinates.size() < 2) {
            return coordinates;
        }

        try {
            // Build OSRM match API URL
            StringBuilder coordString = new StringBuilder();
            for (int i = 0; i < coordinates.size(); i++) {
                double[] coord = coordinates.get(i);
                coordString.append(coord[0]).append(",").append(coord[1]);
                if (i < coordinates.size() - 1) {
                    coordString.append(";");
                }
            }

            // OSRM match API parameters:
            // - geometries=geojson: return coordinates in GeoJSON format
            // - overview=full: return the full route geometry
            // - radiuses: allow matching within radius (in meters)
            String url = OSRM_MATCH_URL + coordString.toString() +
                         "?geometries=geojson&overview=full&radiuses=" +
                         generateRadiuses(coordinates.size());

            // Call OSRM API
            String response = restTemplate.getForObject(url, String.class);

            // Parse response
            JsonNode root = objectMapper.readTree(response);

            if (root.has("matchings") && root.get("matchings").size() > 0) {
                JsonNode geometry = root.get("matchings").get(0).get("geometry");
                JsonNode coordsNode = geometry.get("coordinates");

                List<double[]> snappedCoords = new ArrayList<>();
                for (JsonNode coord : coordsNode) {
                    double lon = coord.get(0).asDouble();
                    double lat = coord.get(1).asDouble();
                    snappedCoords.add(new double[]{lon, lat});
                }

                return snappedCoords;
            }

            // If snapping fails, return original coordinates
            System.err.println("Route snapping failed, using original coordinates");
            return coordinates;

        } catch (Exception e) {
            System.err.println("Error snapping route to roads: " + e.getMessage());
            return coordinates;
        }
    }

    /**
     * Generates radiuses parameter for OSRM API
     * Allows matching points within 50 meters of roads
     */
    private String generateRadiuses(int count) {
        StringBuilder radiuses = new StringBuilder();
        for (int i = 0; i < count; i++) {
            radiuses.append("50"); // 50 meters radius
            if (i < count - 1) {
                radiuses.append(";");
            }
        }
        return radiuses.toString();
    }

    /**
     * Convenience method to snap from [lat, lon] format
     */
    public List<double[]> snapToRoadsFromLatLon(List<double[]> latLonCoords) {
        // Convert from [lat, lon] to [lon, lat]
        List<double[]> lonLatCoords = new ArrayList<>();
        for (double[] coord : latLonCoords) {
            lonLatCoords.add(new double[]{coord[1], coord[0]});
        }

        List<double[]> snapped = snapToRoads(lonLatCoords);

        // Convert back to [lat, lon]
        List<double[]> result = new ArrayList<>();
        for (double[] coord : snapped) {
            result.add(new double[]{coord[1], coord[0]});
        }

        return result;
    }
}
