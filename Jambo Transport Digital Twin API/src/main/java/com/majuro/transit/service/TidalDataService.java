package com.majuro.transit.service;

import com.majuro.transit.model.TidalReading;
import com.majuro.transit.repository.TidalReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TidalDataService {

    private final TidalReadingRepository tidalReadingRepository;
    private final WebClient.Builder webClientBuilder;

    private List<TidalReading> historicalData = new ArrayList<>();
    private int currentIndex = 0;

    private static final String CDIP_API_URL =
        "https://erddap.cdip.ucsd.edu/erddap/tabledap/wave_agg.xhtml" +
        "?station_id,time,waveHs,waveTp,waveTa,waveDp,metaStationName,latitude,longitude" +
        "&station_id=\"163\"" +
        "&time>2025-08-08T00:30:00Z" +
        "&waveFlagPrimary=1";

    @PostConstruct
    public void initialize() {
        log.info("Initializing Tidal Data Service - fetching Aug 29 historical data...");
        fetchHistoricalData();

        if (!historicalData.isEmpty()) {
            // Save first reading to database
            TidalReading firstReading = historicalData.get(0);
            tidalReadingRepository.save(firstReading);
            log.info("Loaded {} historical tidal readings. Starting with wave height: {}m",
                     historicalData.size(), firstReading.getWaveHeight());
        }
    }

    private void fetchHistoricalData() {
        try {
            WebClient webClient = webClientBuilder.build();

            String htmlData = webClient.get()
                    .uri(CDIP_API_URL)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (htmlData != null) {
                parseHTMLTableData(htmlData);
            }
        } catch (Exception e) {
            log.error("Failed to fetch historical tidal data: {}", e.getMessage());
            // Create mock data for testing if API fails
            createMockData();
        }
    }

    private void parseHTMLTableData(String htmlData) {
        // Parse HTML table format from CDIP API
        // Each row starts with <tr>, data cells are <td>...</td>
        // Skip first 2 rows (headers), then from row 3 onwards:
        // 2nd <td> = timestamp, 3rd <td> = wave height, 6th <td> = direction

        String[] lines = htmlData.split("<tr>");

        // Start from row 3 (index 3) - skip headers
        for (int i = 3; i < lines.length; i++) {
            String row = lines[i];
            if (!row.contains("<td>")) continue;

            try {
                // Extract all <td> values
                List<String> tdValues = new ArrayList<>();
                String[] tdSplit = row.split("<td>");

                for (int j = 1; j < tdSplit.length; j++) {
                    String td = tdSplit[j];
                    int endIndex = td.indexOf("</td>");
                    if (endIndex > 0) {
                        String value = td.substring(0, endIndex).trim();
                        tdValues.add(value);
                    }
                }

                // Need at least 6 td values (timestamp=2nd, waveHeight=3rd, direction=6th)
                if (tdValues.size() < 6) continue;

                String timestamp = tdValues.get(1);  // 2nd td (index 1)
                String waveHeight = tdValues.get(2); // 3rd td (index 2)
                String waveDirection = tdValues.get(5); // 6th td (index 5)

                TidalReading reading = TidalReading.builder()
                        .stationId("163")
                        .timestamp(parseTimestamp(timestamp))
                        .waveHeight(Double.parseDouble(waveHeight))
                        .wavePeriod(12.0) // Not using waveTp for now
                        .waveDirection(Double.parseDouble(waveDirection))
                        .stationName("Majuro Station 163")
                        .latitude(7.0897)
                        .longitude(171.2720)
                        .build();

                historicalData.add(reading);

            } catch (Exception e) {
                log.warn("Failed to parse HTML row: {} - {}", row.substring(0, Math.min(100, row.length())), e.getMessage());
            }
        }

        log.info("Successfully parsed {} tidal readings from CDIP API", historicalData.size());
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        // Format: 2025-08-29T00:30:00Z
        timestamp = timestamp.replace("\"", "").replace("Z", "");
        return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private void createMockData() {
        log.info("Creating mock tidal data for testing...");

        // Create 24 hours of mock data with varying wave heights and directions
        LocalDateTime baseTime = LocalDateTime.of(2025, 8, 29, 0, 0);

        for (int hour = 0; hour < 24; hour++) {
            // Simulate wave height variations (2.5m to 5.5m)
            double waveHeight = 3.5 + Math.sin(hour * Math.PI / 6) * 2.0;

            // Simulate direction changes (southerly = 180, northerly = 0/360)
            double direction = hour < 12 ? 180.0 : 10.0;  // Southerly morning, northerly afternoon

            TidalReading reading = TidalReading.builder()
                    .stationId("163")
                    .timestamp(baseTime.plusHours(hour))
                    .waveHeight(waveHeight)
                    .wavePeriod(12.0)
                    .waveDirection(direction)
                    .stationName("Majuro Station 163")
                    .latitude(7.0897)
                    .longitude(171.2720)
                    .build();

            historicalData.add(reading);
        }
    }

    // Rotate through historical data every 10 minutes (simulates 1 hour of real time)
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void rotateData() {
        if (historicalData.isEmpty()) {
            log.warn("No historical data available for rotation");
            return;
        }

        currentIndex = (currentIndex + 1) % historicalData.size();
        TidalReading currentReading = historicalData.get(currentIndex);

        // Update database with new "current" reading
        tidalReadingRepository.deleteAll();
        tidalReadingRepository.save(currentReading);

        log.info("Rotated to reading {}/{}: Wave Height: {}m, Direction: {} ({})",
                 currentIndex + 1,
                 historicalData.size(),
                 currentReading.getWaveHeight(),
                 currentReading.getWaveDirection(),
                 currentReading.getDirectionName());
    }

    public TidalReading getCurrentReading() {
        return tidalReadingRepository.findFirstByOrderByTimestampDesc()
                .orElseGet(() -> {
                    if (!historicalData.isEmpty()) {
                        return historicalData.get(currentIndex);
                    }
                    // Fallback default reading
                    return TidalReading.builder()
                            .stationId("163")
                            .timestamp(LocalDateTime.now())
                            .waveHeight(3.0)
                            .wavePeriod(12.0)
                            .waveDirection(180.0)
                            .stationName("Default")
                            .build();
                });
    }

    public List<TidalReading> getHistoricalData() {
        return new ArrayList<>(historicalData);
    }
}
