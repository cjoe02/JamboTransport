package com.majuro.transit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class MajuroTransitApplication {

    private static long startTime;

    public static void main(String[] args) {
        startTime = System.currentTimeMillis();
        SpringApplication.run(MajuroTransitApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logStartupTime() {
        long endTime = System.currentTimeMillis();
        double totalStartupSeconds = (endTime - startTime) / 1000.0;
        log.info("========================================");
        log.info("Application fully deployed and ready!");
        log.info("Total deployment time: {} seconds", String.format("%.3f", totalStartupSeconds));
        log.info("========================================");
    }
}
