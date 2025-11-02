package com.majuro.transit.repository;

import com.majuro.transit.model.TidalReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TidalReadingRepository extends JpaRepository<TidalReading, Long> {

    @Query("SELECT t FROM TidalReading t ORDER BY t.timestamp DESC LIMIT 1")
    Optional<TidalReading> findLatest();

    Optional<TidalReading> findFirstByOrderByTimestampDesc();
}
