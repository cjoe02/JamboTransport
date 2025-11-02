package com.majuro.transit.loader;

import com.majuro.transit.model.gtfs.*;
import com.majuro.transit.repository.gtfs.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class GtfsDataLoader implements CommandLineRunner {

    private final GtfsStopRepository stopRepository;
    private final GtfsRouteRepository routeRepository;
    private final GtfsTripRepository tripRepository;
    private final GtfsStopTimeRepository stopTimeRepository;
    private final GtfsCalendarRepository calendarRepository;

    private static final String GTFS_DIR = "gtfs";

    @Override
    public void run(String... args) throws Exception {
        long startTime = System.currentTimeMillis();
        log.info("Loading GTFS data...");

        File gtfsDir = new File(GTFS_DIR);
        if (!gtfsDir.exists() || !gtfsDir.isDirectory()) {
            log.error("GTFS directory not found: {}", GTFS_DIR);
            return;
        }

        // Load in correct order due to foreign key dependencies
        loadCalendar();
        loadStops();
        loadRoutes();
        loadTrips();
        loadStopTimes();

        long endTime = System.currentTimeMillis();
        double deploymentTimeSeconds = (endTime - startTime) / 1000.0;

        log.info("GTFS data loading completed!");
        log.info("Application deployment time: {} seconds", String.format("%.3f", deploymentTimeSeconds));
    }

    private void loadCalendar() throws Exception {
        File file = new File(GTFS_DIR, "calendar.txt");
        if (!file.exists()) {
            log.warn("calendar.txt not found");
            return;
        }

        try (Reader reader = new FileReader(file);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : parser) {
                GtfsCalendar calendar = new GtfsCalendar();
                calendar.setServiceId(record.get("service_id"));
                calendar.setMonday(record.get("monday").equals("1"));
                calendar.setTuesday(record.get("tuesday").equals("1"));
                calendar.setWednesday(record.get("wednesday").equals("1"));
                calendar.setThursday(record.get("thursday").equals("1"));
                calendar.setFriday(record.get("friday").equals("1"));
                calendar.setSaturday(record.get("saturday").equals("1"));
                calendar.setSunday(record.get("sunday").equals("1"));
                calendar.setStartDate(LocalDate.parse(record.get("start_date"), DateTimeFormatter.BASIC_ISO_DATE));
                calendar.setEndDate(LocalDate.parse(record.get("end_date"), DateTimeFormatter.BASIC_ISO_DATE));

                calendarRepository.save(calendar);
            }

            log.info("Loaded {} calendar entries", calendarRepository.count());
        }
    }

    private void loadStops() throws Exception {
        File file = new File(GTFS_DIR, "stops.txt");
        if (!file.exists()) {
            log.warn("stops.txt not found");
            return;
        }

        try (Reader reader = new FileReader(file);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : parser) {
                GtfsStop stop = new GtfsStop();
                stop.setStopId(record.get("stop_id"));
                stop.setStopName(record.get("stop_name"));
                stop.setStopLat(Double.parseDouble(record.get("stop_lat")));
                stop.setStopLon(Double.parseDouble(record.get("stop_lon")));

                stopRepository.save(stop);
            }

            log.info("Loaded {} stops", stopRepository.count());
        }
    }

    private void loadRoutes() throws Exception {
        File file = new File(GTFS_DIR, "routes.txt");
        if (!file.exists()) {
            log.warn("routes.txt not found");
            return;
        }

        try (Reader reader = new FileReader(file);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : parser) {
                GtfsRoute route = new GtfsRoute();
                route.setRouteId(record.get("route_id"));
                route.setRouteShortName(record.get("route_short_name"));
                route.setRouteLongName(record.get("route_long_name"));
                route.setRouteType(Integer.parseInt(record.get("route_type")));

                routeRepository.save(route);
            }

            log.info("Loaded {} routes", routeRepository.count());
        }
    }

    private void loadTrips() throws Exception {
        File file = new File(GTFS_DIR, "trips.txt");
        if (!file.exists()) {
            log.warn("trips.txt not found");
            return;
        }

        Map<String, GtfsRoute> routeCache = new HashMap<>();
        Map<String, GtfsCalendar> calendarCache = new HashMap<>();

        try (Reader reader = new FileReader(file);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : parser) {
                String routeId = record.get("route_id");
                String serviceId = record.get("service_id");

                GtfsRoute route = routeCache.computeIfAbsent(routeId,
                    id -> routeRepository.findById(id).orElseThrow());
                GtfsCalendar calendar = calendarCache.computeIfAbsent(serviceId,
                    id -> calendarRepository.findById(id).orElseThrow());

                GtfsTrip trip = new GtfsTrip();
                trip.setTripId(record.get("trip_id"));
                trip.setRoute(route);
                trip.setService(calendar);
                trip.setTripHeadsign(record.get("trip_headsign"));
                trip.setDirectionId(Integer.parseInt(record.get("direction_id")));

                tripRepository.save(trip);
            }

            log.info("Loaded {} trips", tripRepository.count());
        }
    }

    private void loadStopTimes() throws Exception {
        File file = new File(GTFS_DIR, "stop_times.txt");
        if (!file.exists()) {
            log.warn("stop_times.txt not found");
            return;
        }

        Map<String, GtfsTrip> tripCache = new HashMap<>();
        Map<String, GtfsStop> stopCache = new HashMap<>();

        try (Reader reader = new FileReader(file);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : parser) {
                String tripId = record.get("trip_id");
                String stopId = record.get("stop_id");

                GtfsTrip trip = tripCache.computeIfAbsent(tripId,
                    id -> tripRepository.findById(id).orElseThrow());
                GtfsStop stop = stopCache.computeIfAbsent(stopId,
                    id -> stopRepository.findById(id).orElseThrow());

                GtfsStopTime stopTime = new GtfsStopTime();
                stopTime.setTrip(trip);
                stopTime.setStop(stop);
                stopTime.setArrivalTime(parseTime(record.get("arrival_time")));
                stopTime.setDepartureTime(parseTime(record.get("departure_time")));
                stopTime.setStopSequence(Integer.parseInt(record.get("stop_sequence")));

                stopTimeRepository.save(stopTime);
            }

            log.info("Loaded {} stop times", stopTimeRepository.count());
        }
    }

    private LocalTime parseTime(String timeStr) {
        // Handle times beyond 24:00:00 (e.g., 25:30:00 for 1:30 AM next day)
        String[] parts = timeStr.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);

        // Convert to valid LocalTime (0-23 hours)
        if (hours >= 24) {
            hours = hours % 24;
        }

        return LocalTime.of(hours, minutes, seconds);
    }
}
