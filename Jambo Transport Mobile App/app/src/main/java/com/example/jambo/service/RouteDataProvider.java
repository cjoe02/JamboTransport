package com.example.jambo.service;

import com.example.jambo.model.Bus;
import com.example.jambo.model.BusRoute;
import org.osmdroid.util.GeoPoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RouteDataProvider {
    
    public static List<BusRoute> getMajuroRoutes() {
        List<BusRoute> routes = new ArrayList<>();
        
        BusRoute ritaToHospital = createRitaToHospitalRoute();
        BusRoute hospitalToRita = createHospitalToRitaRoute();
        
        routes.add(ritaToHospital);
        routes.add(hospitalToRita);
        
        return routes;
    }

    private static BusRoute createRitaToHospitalRoute() {
        List<GeoPoint> routePoints = Arrays.asList(
            new GeoPoint(7.1164, 171.1847), // Rita area
            new GeoPoint(7.1156, 171.1735),
            new GeoPoint(7.1148, 171.1623),
            new GeoPoint(7.1140, 171.1511),
            new GeoPoint(7.1132, 171.1399),
            new GeoPoint(7.1124, 171.1287),
            new GeoPoint(7.1116, 171.1175),
            new GeoPoint(7.1108, 171.1063), 
            new GeoPoint(7.1100, 171.0951),
            new GeoPoint(7.1092, 171.0839),
            new GeoPoint(7.1084, 171.0727) // Hospital area
        );

        List<Bus> activeBuses = Arrays.asList(
            new Bus("bus_001", "Bus 1", new GeoPoint(7.1160, 171.1800), "rita_hospital_1", true),
            new Bus("bus_002", "Bus 2", new GeoPoint(7.1140, 171.1500), "rita_hospital_1", true),
            new Bus("bus_003", "Bus 3", new GeoPoint(7.1120, 171.1200), "rita_hospital_1", true),
            new Bus("bus_004", "Bus 4", new GeoPoint(7.1100, 171.0900), "rita_hospital_1", true)
        );

        return new BusRoute(
            "rita_hospital_1",
            "Rita - Hospital Route",
            "Rita",
            "Majuro Hospital",
            routePoints,
            activeBuses
        );
    }

    private static BusRoute createHospitalToRitaRoute() {
        List<GeoPoint> routePoints = Arrays.asList(
            new GeoPoint(7.1084, 171.0727), // Hospital area
            new GeoPoint(7.1092, 171.0839),
            new GeoPoint(7.1100, 171.0951),
            new GeoPoint(7.1108, 171.1063),
            new GeoPoint(7.1116, 171.1175),
            new GeoPoint(7.1124, 171.1287),
            new GeoPoint(7.1132, 171.1399),
            new GeoPoint(7.1140, 171.1511),
            new GeoPoint(7.1148, 171.1623),
            new GeoPoint(7.1156, 171.1735),
            new GeoPoint(7.1164, 171.1847)  // Rita area
        );

        List<Bus> activeBuses = Arrays.asList(
            new Bus("bus_005", "Bus 5", new GeoPoint(7.1090, 171.0800), "hospital_rita_1", true),
            new Bus("bus_006", "Bus 6", new GeoPoint(7.1110, 171.1100), "hospital_rita_1", true),
            new Bus("bus_007", "Bus 7", new GeoPoint(7.1130, 171.1400), "hospital_rita_1", true),
            new Bus("bus_008", "Bus 8", new GeoPoint(7.1150, 171.1700), "hospital_rita_1", true)
        );

        return new BusRoute(
            "hospital_rita_1",
            "Hospital - Rita Route",
            "Majuro Hospital",
            "Rita",
            routePoints,
            activeBuses
        );
    }

    public static GeoPoint getMajuroCenter() {
        return new GeoPoint(7.1164, 171.1287);
    }

    public static float getDefaultZoom() {
        return 13.0f;
    }
}